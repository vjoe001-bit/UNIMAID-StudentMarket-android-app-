package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.Order
import com.example.data.models.OrderItem
import com.example.data.models.OrderStatusHistory
import java.util.UUID

/**
 * Order & Campus Handover Repository for UNIMAID StudentMarket.
 * Strictly adheres to university policy:
 * All transactions represent physical campus meetup arrangements, with NO online payment gateway.
 */
interface OrderRepository {
    suspend fun createOrder(order: Order, items: List<OrderItem>): SupabaseResult<Order>
    suspend fun getOrdersForUser(userId: String, asBuyerOnly: Boolean = false, asSellerOnly: Boolean = false): SupabaseResult<List<Order>>
    suspend fun getOrderById(orderId: String): SupabaseResult<Order>
    suspend fun updateOrderStatus(orderId: String, newStatus: String, changedBy: String, note: String? = null): SupabaseResult<Order>
    suspend fun updatePaymentStatus(orderId: String, newPaymentStatus: String, changedBy: String): SupabaseResult<Order>
    suspend fun arrangeMeeting(orderId: String, location: String, time: String?, changedBy: String, note: String? = null): SupabaseResult<Order>
    suspend fun acceptOrder(orderId: String, sellerId: String, note: String? = null): SupabaseResult<Order>
    suspend fun declineOrder(orderId: String, sellerId: String, reason: String? = null): SupabaseResult<Order>
    suspend fun cancelOrder(orderId: String, userId: String, reason: String? = null): SupabaseResult<Order>
    suspend fun completeOrder(orderId: String, userId: String, note: String? = null): SupabaseResult<Order>
}

class OrderRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi(),
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl(restApi)
) : OrderRepository {

    override suspend fun createOrder(order: Order, items: List<OrderItem>): SupabaseResult<Order> {
        return try {
            // Safety: cannot buy own listing
            if (order.buyerId == order.sellerId) {
                return SupabaseResult.Error("You cannot submit a purchase request for your own listing.")
            }

            // 1. Resolve listing and check availability & inventory
            val targetListingId = order.listingId ?: items.firstOrNull()?.listingId
            if (targetListingId.isNullOrBlank()) {
                return SupabaseResult.Error("No item specified for purchase request.")
            }

            val reqQuantity = if (order.quantity > 0) order.quantity else (items.firstOrNull()?.quantity ?: 1)

            val listingResp = restApi.getListingById("eq.$targetListingId")
            if (listingResp.isSuccessful && !listingResp.body().isNullOrEmpty()) {
                val listing = listingResp.body()!!.first()
                if (!listing.isAvailable || listing.status.equals("SOLD", ignoreCase = true) || listing.status.equals("DEACTIVATED", ignoreCase = true)) {
                    return SupabaseResult.Error("Item '${listing.displayTitle}' is no longer available.")
                }
                if (reqQuantity > listing.quantity) {
                    return SupabaseResult.Error("Item '${listing.displayTitle}' only has ${listing.quantity} remaining.")
                }
            } else {
                return SupabaseResult.Error("The requested campus item could not be found.")
            }

            // 2. Insert order directly into public.orders table
            val orderToCreate = order.copy(
                listingId = targetListingId,
                quantity = reqQuantity
            )
            val response = restApi.createOrder(orderToCreate)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val createdOrder = response.body()!!.first()

                // Record initial status history safely
                runCatching {
                    val initialHistory = OrderStatusHistory(
                        id = UUID.randomUUID().toString(),
                        orderId = createdOrder.id,
                        newStatus = createdOrder.status,
                        changedBy = order.buyerId,
                        note = order.buyerNote ?: "Purchase request placed for campus physical handover."
                    )
                    restApi.addOrderStatusHistory(initialHistory)
                }

                // Notify seller of incoming request
                runCatching {
                    notificationRepository.createNotification(
                        userId = order.sellerId,
                        title = "New Purchase Request",
                        message = "A student placed a handover request for ₦${"%,.0f".format(order.totalAmount)}.",
                        type = "ORDER"
                    )
                }

                SupabaseResult.Success(createdOrder)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getOrdersForUser(userId: String, asBuyerOnly: Boolean, asSellerOnly: Boolean): SupabaseResult<List<Order>> {
        return try {
            val filters = mutableMapOf<String, String>()
            when {
                asBuyerOnly -> filters["buyer_id"] = "eq.$userId"
                asSellerOnly -> filters["seller_id"] = "eq.$userId"
                else -> filters["or"] = "(buyer_id.eq.$userId,seller_id.eq.$userId)"
            }
            val response = restApi.getOrders(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getOrderById(orderId: String): SupabaseResult<Order> {
        return try {
            val response = restApi.getOrderById("eq.$orderId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        changedBy: String,
        note: String?
    ): SupabaseResult<Order> {
        return try {
            val updates = mutableMapOf<String, Any?>("status" to newStatus)
            if (newStatus == "COMPLETED") {
                updates["completed_at"] = "now()"
                updates["payment_status"] = "PAID_ON_CAMPUS"
            }
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                val history = OrderStatusHistory(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    status = newStatus,
                    changedBy = changedBy,
                    note = note
                )
                restApi.addOrderStatusHistory(history)
                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updatePaymentStatus(
        orderId: String,
        newPaymentStatus: String,
        changedBy: String
    ): SupabaseResult<Order> {
        return try {
            val updates = mapOf("payment_status" to newPaymentStatus)
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                val history = OrderStatusHistory(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    status = "PAYMENT_${newPaymentStatus}",
                    changedBy = changedBy,
                    note = "Payment status updated to $newPaymentStatus (Cash/In-Person on Campus)"
                )
                restApi.addOrderStatusHistory(history)
                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun arrangeMeeting(
        orderId: String,
        location: String,
        time: String?,
        changedBy: String,
        note: String?
    ): SupabaseResult<Order> {
        return try {
            val orderRes = getOrderById(orderId)
            if (orderRes !is SupabaseResult.Success) {
                return SupabaseResult.Error("Transaction could not be found.")
            }
            val currentOrder = orderRes.data
            if (changedBy != currentOrder.buyerId && changedBy != currentOrder.sellerId) {
                return SupabaseResult.Error("Unauthorized to arrange meeting for this transaction.")
            }

            val updates = mutableMapOf<String, Any?>(
                "status" to "MEETING_ARRANGED",
                "meeting_location" to location
            )
            if (!note.isNullOrBlank()) {
                updates["seller_note"] = note
            }
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                runCatching {
                    val history = OrderStatusHistory(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        oldStatus = currentOrder.status,
                        newStatus = "MEETING_ARRANGED",
                        changedBy = changedBy,
                        note = "Meeting arranged at $location.${if (!time.isNullOrBlank()) " Time: $time." else ""}"
                    )
                    restApi.addOrderStatusHistory(history)
                }

                // Notify other participant
                val recipientId = if (changedBy == currentOrder.buyerId) currentOrder.sellerId else currentOrder.buyerId
                runCatching {
                    notificationRepository.createNotification(
                        userId = recipientId,
                        title = "Meetup Arranged",
                        message = "Campus meeting location set to $location.",
                        type = "ORDER"
                    )
                }

                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun acceptOrder(orderId: String, sellerId: String, note: String?): SupabaseResult<Order> {
        return try {
            val orderRes = getOrderById(orderId)
            if (orderRes !is SupabaseResult.Success) {
                return SupabaseResult.Error("Transaction not found.")
            }
            val order = orderRes.data
            // Authorization check
            if (order.sellerId != sellerId) {
                return SupabaseResult.Error("Unauthorized: You are not the seller of this order.")
            }
            if (order.status != "PENDING") {
                return SupabaseResult.Error("This order is already ${order.status.lowercase()}.")
            }

            // Live availability & inventory decrement
            for (item in order.effectiveItems) {
                val listingResp = restApi.getListingById("eq.${item.listingId}")
                if (listingResp.isSuccessful && !listingResp.body().isNullOrEmpty()) {
                    val listing = listingResp.body()!!.first()
                    val newQuantity = maxOf(0, listing.quantity - item.quantity)
                    val isSold = newQuantity == 0
                    restApi.updateListing(
                        "eq.${item.listingId}",
                        mapOf(
                            "quantity" to newQuantity,
                            "is_available" to !isSold,
                            "status" to if (isSold) "SOLD" else "ACTIVE"
                        )
                    )
                }
            }

            // Update order status
            val updates = mapOf<String, Any?>(
                "status" to "ACCEPTED",
                "seller_note" to (note ?: "Accepted by student seller.")
            )
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                runCatching {
                    val history = OrderStatusHistory(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        oldStatus = "PENDING",
                        newStatus = "ACCEPTED",
                        changedBy = sellerId,
                        note = note ?: "Seller accepted the request."
                    )
                    restApi.addOrderStatusHistory(history)
                }

                // Notify buyer
                runCatching {
                    notificationRepository.createNotification(
                        userId = order.buyerId,
                        title = "Request Accepted!",
                        message = "The student seller accepted your purchase request. Coordinate meeting details now.",
                        type = "ORDER"
                    )
                }

                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun declineOrder(orderId: String, sellerId: String, reason: String?): SupabaseResult<Order> {
        return try {
            val orderRes = getOrderById(orderId)
            if (orderRes !is SupabaseResult.Success) {
                return SupabaseResult.Error("Transaction not found.")
            }
            val order = orderRes.data
            if (order.sellerId != sellerId) {
                return SupabaseResult.Error("Unauthorized: You are not the seller of this order.")
            }
            if (order.status != "PENDING") {
                return SupabaseResult.Error("Order cannot be declined because its status is ${order.status}.")
            }

            val updates = mapOf<String, Any?>(
                "status" to "DECLINED",
                "seller_note" to (reason ?: "Declined by seller.")
            )
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                runCatching {
                    val history = OrderStatusHistory(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        oldStatus = order.status,
                        newStatus = "DECLINED",
                        changedBy = sellerId,
                        note = reason ?: "Seller declined request."
                    )
                    restApi.addOrderStatusHistory(history)
                }

                // Notify buyer
                runCatching {
                    notificationRepository.createNotification(
                        userId = order.buyerId,
                        title = "Request Declined",
                        message = "The student seller was unable to fulfill your request: ${reason ?: "No reason given."}",
                        type = "ORDER"
                    )
                }

                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun cancelOrder(orderId: String, userId: String, reason: String?): SupabaseResult<Order> {
        return try {
            val orderRes = getOrderById(orderId)
            if (orderRes !is SupabaseResult.Success) {
                return SupabaseResult.Error("Transaction not found.")
            }
            val order = orderRes.data
            if (order.buyerId != userId && order.sellerId != userId) {
                return SupabaseResult.Error("Unauthorized to cancel this order.")
            }
            if (order.status == "COMPLETED") {
                return SupabaseResult.Error("Cannot cancel a completed transaction.")
            }
            if (order.status == "CANCELLED") {
                return SupabaseResult.Error("Order is already cancelled.")
            }

            // If order was ACCEPTED, restore inventory
            if (order.status == "ACCEPTED" || order.status == "MEETING_ARRANGED") {
                for (item in order.effectiveItems) {
                    val listingResp = restApi.getListingById("eq.${item.listingId}")
                    if (listingResp.isSuccessful && !listingResp.body().isNullOrEmpty()) {
                        val listing = listingResp.body()!!.first()
                        val restoredQty = listing.quantity + item.quantity
                        restApi.updateListing(
                            "eq.${item.listingId}",
                            mapOf(
                                "quantity" to restoredQty,
                                "is_available" to true,
                                "status" to "ACTIVE"
                            )
                        )
                    }
                }
            }

            val updates = mapOf<String, Any?>(
                "status" to "CANCELLED"
            )
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                runCatching {
                    val history = OrderStatusHistory(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        oldStatus = order.status,
                        newStatus = "CANCELLED",
                        changedBy = userId,
                        note = reason ?: "Transaction cancelled by user."
                    )
                    restApi.addOrderStatusHistory(history)
                }

                // Notify other party
                val recipientId = if (userId == order.buyerId) order.sellerId else order.buyerId
                runCatching {
                    notificationRepository.createNotification(
                        userId = recipientId,
                        title = "Order Cancelled",
                        message = "The campus transaction was cancelled: ${reason ?: "No reason given."}",
                        type = "ORDER"
                    )
                }

                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun completeOrder(orderId: String, userId: String, note: String?): SupabaseResult<Order> {
        return try {
            val orderRes = getOrderById(orderId)
            if (orderRes !is SupabaseResult.Success) {
                return SupabaseResult.Error("Transaction not found.")
            }
            val order = orderRes.data
            if (order.buyerId != userId && order.sellerId != userId) {
                return SupabaseResult.Error("Unauthorized to complete this order.")
            }

            val updates = mapOf<String, Any?>(
                "status" to "COMPLETED"
            )
            val response = restApi.updateOrder("eq.$orderId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                runCatching {
                    val history = OrderStatusHistory(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        oldStatus = order.status,
                        newStatus = "COMPLETED",
                        changedBy = userId,
                        note = note ?: "In-person campus transaction completed successfully."
                    )
                    restApi.addOrderStatusHistory(history)
                }

                // Notify other party
                val recipientId = if (userId == order.buyerId) order.sellerId else order.buyerId
                runCatching {
                    notificationRepository.createNotification(
                        userId = recipientId,
                        title = "Transaction Completed",
                        message = "Campus handover confirmed! Item verified and physical exchange concluded.",
                        type = "ORDER"
                    )
                }

                SupabaseResult.Success(updated)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}
