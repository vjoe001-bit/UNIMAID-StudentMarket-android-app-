package com.example.core.network

import com.example.BuildConfig

/**
 * Public Supabase client configuration for UNIMAID StudentMarket.
 *
 * CRITICAL SECURITY RULES:
 * - Uses ONLY the public/publishable Anon Key and project URL.
 * - Service-role key, database passwords, and privileged credentials MUST NEVER be included.
 * - All table access is enforced server-side via Supabase Row Level Security (RLS).
 */
object SupabaseConfig {
    // Supabase project URL provided for UNIMAID StudentMarket
    const val DEFAULT_URL = "https://uwgroqsvslyilomumial.supabase.co"

    /**
     * Resolves the Supabase URL, prioritizing BuildConfig if injected via .env / Secrets.
     */
    val url: String
        get() {
            val configured = runCatching { BuildConfig.SUPABASE_URL }.getOrNull()
            return if (!configured.isNullOrBlank()) configured else DEFAULT_URL
        }

    /**
     * Resolves the Supabase Public Anon Key.
     */
    val anonKey: String
        get() {
            val configured = runCatching { BuildConfig.SUPABASE_ANON_KEY }.getOrNull()
            return if (!configured.isNullOrBlank()) {
                configured
            } else {
                "sb_publishable_tgKRpzzAgH2yfoP4iJ5g_g_o2nicEQ2"
            }
        }

    // Supabase GoTrue Auth endpoint
    val authBaseUrl: String
        get() = "$url/auth/v1/"

    // Supabase PostgREST endpoint
    val restBaseUrl: String
        get() = "$url/rest/v1/"

    // Supabase Storage endpoint
    val storageBaseUrl: String
        get() = "$url/storage/v1/"

    // Supabase Realtime WebSocket URL
    val realtimeWebSocketUrl: String
        get() {
            val host = url.removePrefix("https://").removePrefix("http://")
            return "wss://$host/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
        }

    /**
     * Existing Storage Buckets defined in the Supabase backend.
     */
    object StorageBuckets {
        const val LISTING_IMAGES = "listing-images"
        const val SERVICE_IMAGES = "service-images"
        const val AVATARS = "avatars"
        const val VERIFICATION_DOCUMENTS = "verification-documents"

        fun getPublicUrl(bucket: String, path: String): String {
            val cleanPath = path.trimStart('/')
            return "${storageBaseUrl.trimEnd('/')}/object/public/$bucket/$cleanPath"
        }
    }

    /**
     * Existing PostgreSQL Tables configured in Supabase.
     */
    object Tables {
        const val PROFILES = "profiles"
        const val CATEGORIES = "categories"
        const val LISTINGS = "listings"
        const val LISTING_IMAGES = "listing_images"
        const val SERVICES = "services"
        const val SERVICE_IMAGES = "service_images"
        const val VERIFICATION_REQUESTS = "verification_requests"
        const val CART_ITEMS = "cart_items"
        const val CARTS = "carts"
        const val ORDERS = "orders"
        const val ORDER_ITEMS = "order_items"
        const val ORDER_STATUS_HISTORY = "order_status_history"
        const val CONVERSATIONS = "conversations"
        const val CONVERSATION_PARTICIPANTS = "conversation_participants"
        const val MESSAGES = "messages"
        const val NOTIFICATIONS = "notifications"
        const val FAVORITES = "favorites"
        const val REVIEWS = "reviews"
        const val REPORTS = "reports"
        const val USER_SETTINGS = "user_settings"
        const val BLOCKED_USERS = "blocked_users"
        const val ADMIN_ROLES = "admin_roles"
    }

    /**
     * Realtime subscribed topics.
     */
    object RealtimeTopics {
        const val MESSAGES = "realtime:public:messages"
        const val NOTIFICATIONS = "realtime:public:notifications"
        const val ORDERS = "realtime:public:orders"
        const val CONVERSATIONS = "realtime:public:conversations"
        const val LISTINGS = "realtime:public:listings"
    }
}
