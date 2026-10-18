package com.example

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseResult
import com.example.core.realtime.RealtimeEvent
import com.example.core.realtime.SupabaseRealtimeManager
import com.example.data.models.Notification
import com.example.data.models.NotificationTypes
import com.example.data.repository.NotificationRepository
import com.example.ui.viewmodels.NotificationUiState
import com.example.ui.viewmodels.NotificationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationCenterArchitectureTest {

    private val studentAId = "11111111-1111-1111-1111-111111111111"
    private val studentBId = "22222222-2222-2222-2222-222222222222"

    private class MockNotificationRepository : NotificationRepository {
        val database = mutableListOf<Notification>()

        override suspend fun getNotifications(userId: String): SupabaseResult<List<Notification>> {
            // User-specific RLS enforcement: only return user's own notifications
            val userNotifs = database.filter { it.userId == userId }
            return SupabaseResult.Success(userNotifs)
        }

        override suspend fun markAsRead(notificationId: String): SupabaseResult<Unit> {
            val idx = database.indexOfFirst { it.id == notificationId }
            if (idx != -1) {
                database[idx] = database[idx].copy(isRead = true)
            }
            return SupabaseResult.Success(Unit)
        }

        override suspend fun markAllAsRead(userId: String): SupabaseResult<Unit> {
            for (i in database.indices) {
                if (database[i].userId == userId) {
                    database[i] = database[i].copy(isRead = true)
                }
            }
            return SupabaseResult.Success(Unit)
        }

        override suspend fun deleteNotification(notificationId: String): SupabaseResult<Unit> {
            database.removeAll { it.id == notificationId }
            return SupabaseResult.Success(Unit)
        }

        override suspend fun getUnreadCount(userId: String): SupabaseResult<Int> {
            val count = database.count { it.userId == userId && !it.isRead }
            return SupabaseResult.Success(count)
        }

        override suspend fun createNotification(
            userId: String,
            title: String,
            message: String,
            type: String,
            dataPayload: Map<String, Any?>?
        ): SupabaseResult<Notification> {
            val notif = Notification(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                message = message,
                body = message,
                type = type,
                isRead = false,
                dataPayload = dataPayload,
                createdAt = "2026-10-08T10:00:00Z"
            )
            database.add(0, notif)
            return SupabaseResult.Success(notif)
        }
    }

    @Test
    fun `test A - message notification creation and navigation payload`() = runTest {
        val repo = MockNotificationRepository()

        // Student A sends message to Student B
        val createRes = repo.createNotification(
            userId = studentBId,
            title = "New Message",
            message = "Can we meet at Ramat Library?",
            type = NotificationTypes.MESSAGE,
            dataPayload = mapOf(
                "conversation_id" to "conv-999",
                "sender_id" to studentAId,
                "entity_id" to "conv-999"
            )
        )

        assertTrue(createRes is SupabaseResult.Success)
        val notif = (createRes as SupabaseResult.Success).data
        assertEquals(studentBId, notif.userId)
        assertEquals("New Message", notif.title)
        assertEquals(NotificationTypes.MESSAGE, notif.type)
        assertEquals("conv-999", notif.conversationId)
        assertEquals(studentAId, notif.senderId)
        assertFalse(notif.isRead)

        // Mark as read decreases unread count
        val unreadBefore = repo.getUnreadCount(studentBId)
        assertEquals(1, (unreadBefore as SupabaseResult.Success).data)

        repo.markAsRead(notif.id)

        val unreadAfter = repo.getUnreadCount(studentBId)
        assertEquals(0, (unreadAfter as SupabaseResult.Success).data)
    }

    @Test
    fun `test B - purchase request and acceptance lifecycle notifications`() = runTest {
        val repo = MockNotificationRepository()

        // 1. Buyer creates purchase request -> Seller receives notification
        val reqRes = repo.createNotification(
            userId = studentAId, // Seller
            title = "New Purchase Request",
            message = "A student placed a handover request for ₦15,000.",
            type = NotificationTypes.PURCHASE_REQUEST,
            dataPayload = mapOf(
                "order_id" to "order-123",
                "tab" to "sales",
                "entity_id" to "order-123"
            )
        )
        assertTrue(reqRes is SupabaseResult.Success)
        val reqNotif = (reqRes as SupabaseResult.Success).data
        assertEquals("sales", reqNotif.orderTab)
        assertEquals("order-123", reqNotif.orderId)

        // 2. Seller accepts -> Buyer receives notification
        val acceptRes = repo.createNotification(
            userId = studentBId, // Buyer
            title = "Request Accepted!",
            message = "The student seller accepted your purchase request. Coordinate meeting details now.",
            type = NotificationTypes.PURCHASE_ACCEPTED,
            dataPayload = mapOf(
                "order_id" to "order-123",
                "tab" to "purchases",
                "entity_id" to "order-123"
            )
        )
        assertTrue(acceptRes is SupabaseResult.Success)
        val acceptNotif = (acceptRes as SupabaseResult.Success).data
        assertEquals("purchases", acceptNotif.orderTab)
        assertEquals("order-123", acceptNotif.orderId)
    }

    @Test
    fun `test C - security isolation between Student A and Student B`() = runTest {
        val repo = MockNotificationRepository()

        // Create notifications for Student A
        repo.createNotification(
            userId = studentAId,
            title = "Student A Secret Alert",
            message = "Private transaction update",
            type = NotificationTypes.TRANSACTION_UPDATE
        )

        // Create notifications for Student B
        repo.createNotification(
            userId = studentBId,
            title = "Student B Order Alert",
            message = "Campus meetup set",
            type = NotificationTypes.HANDOVER_UPDATE
        )

        // Query Student A
        val studentANotifs = repo.getNotifications(studentAId)
        assertTrue(studentANotifs is SupabaseResult.Success)
        val listA = (studentANotifs as SupabaseResult.Success).data
        assertEquals(1, listA.size)
        assertEquals("Student A Secret Alert", listA[0].title)

        // Query Student B
        val studentBNotifs = repo.getNotifications(studentBId)
        assertTrue(studentBNotifs is SupabaseResult.Success)
        val listB = (studentBNotifs as SupabaseResult.Success).data
        assertEquals(1, listB.size)
        assertEquals("Student B Order Alert", listB[0].title)

        // Student B must NOT see Student A notifications
        assertTrue(listB.none { it.userId == studentAId })
        assertTrue(listA.none { it.userId == studentBId })
    }

    @Test
    fun `test D - logout and user session switch clears notifications cache`() = runTest {
        val repo = MockNotificationRepository()
        val realtime = SupabaseRealtimeManager(SupabaseClient.okHttpClient)
        val viewModel = NotificationViewModel(repo, realtime)

        // 1. Student A logs in
        repo.createNotification(studentAId, "A Note", "Content", NotificationTypes.SYSTEM)
        viewModel.onUserSessionChanged(studentAId)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.notifications.size)
        assertEquals(1, viewModel.uiState.value.unreadCount)

        // 2. Student A logs out
        viewModel.onUserSessionChanged(null)
        testScheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.notifications.size)
        assertEquals(0, viewModel.uiState.value.unreadCount)

        // 3. Student B logs in on same device
        repo.createNotification(studentBId, "B Note", "B Content", NotificationTypes.SYSTEM)
        viewModel.onUserSessionChanged(studentBId)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.notifications.size)
        assertEquals("B Note", viewModel.uiState.value.notifications[0].title)
        assertEquals(studentBId, viewModel.uiState.value.notifications[0].userId)
    }

    @Test
    fun `test E - mark all as read and notification deletion`() = runTest {
        val repo = MockNotificationRepository()
        val realtime = SupabaseRealtimeManager(SupabaseClient.okHttpClient)
        val viewModel = NotificationViewModel(repo, realtime)

        val n1 = repo.createNotification(studentAId, "Title 1", "Msg 1", NotificationTypes.SYSTEM)
        val n2 = repo.createNotification(studentAId, "Title 2", "Msg 2", NotificationTypes.SYSTEM)
        val notif1Id = (n1 as SupabaseResult.Success).data.id
        val notif2Id = (n2 as SupabaseResult.Success).data.id

        viewModel.onUserSessionChanged(studentAId)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.unreadCount)

        // Mark all as read
        viewModel.markAllAsRead()
        testScheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.notifications.all { it.isRead })

        // Delete one notification
        viewModel.deleteNotification(notif1Id)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.notifications.size)
        assertEquals(notif2Id, viewModel.uiState.value.notifications[0].id)
    }

    @Test
    fun `test F - realtime event model parsing for Supabase notifications table`() {
        val rawJson = JSONObject().apply {
            put("topic", "realtime:public:notifications")
            put("event", "INSERT")
            put("payload", JSONObject().apply {
                put("table", "notifications")
                put("type", "INSERT")
                put("record", JSONObject().apply {
                    put("id", "notif-uuid-1")
                    put("user_id", studentAId)
                    put("title", "Realtime Alert")
                    put("body", "Handover meetup arranged at Faculty of Engineering")
                    put("type", "HANDOVER_UPDATE")
                    put("is_read", false)
                })
            })
        }

        val event = RealtimeEvent(
            topic = rawJson.getString("topic"),
            event = rawJson.getString("event"),
            table = "notifications",
            eventType = "INSERT",
            payload = rawJson.getJSONObject("payload")
        )

        assertNotNull(event.record)
        assertEquals("notif-uuid-1", event.record?.getString("id"))
        assertEquals(studentAId, event.record?.getString("user_id"))
        assertEquals("HANDOVER_UPDATE", event.record?.getString("type"))
    }
}
