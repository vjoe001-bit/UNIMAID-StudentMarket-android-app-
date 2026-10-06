package com.example.navigation

/**
 * Type-safe navigation routes across UNIMAID StudentMarket.
 * Prepares the route hierarchy for Public, Main Tabs, Feature, Seller, and Admin screens.
 */
sealed class NavRoutes(val route: String) {

    // Public / Onboarding & Auth
    object Welcome : NavRoutes("welcome")
    object Login : NavRoutes("login")
    object SignUp : NavRoutes("signup")
    object ForgotPassword : NavRoutes("forgot_password")

    // Main Tabs (Bottom Navigation)
    object MainHost : NavRoutes("main_host")
    object Home : NavRoutes("home")
    object Categories : NavRoutes("categories")
    object Cart : NavRoutes("cart")
    object Messages : NavRoutes("messages?recipientId={recipientId}&listingId={listingId}") {
        fun createRoute(recipientId: String? = null, listingId: String? = null): String {
            val params = mutableListOf<String>()
            if (!recipientId.isNullOrBlank()) params.add("recipientId=$recipientId")
            if (!listingId.isNullOrBlank()) params.add("listingId=$listingId")
            return if (params.isNotEmpty()) "messages?${params.joinToString("&")}" else "messages"
        }
    }
    object Profile : NavRoutes("profile")

    // Marketplace Features & Details
    object Search : NavRoutes("search")
    object Favorites : NavRoutes("favorites")
    object Notifications : NavRoutes("notifications")
    object StudentVerification : NavRoutes("student_verification")
    object ProfileCompletion : NavRoutes("profile_completion")
    object ProductDetails : NavRoutes("product_details/{listingId}") {
        fun createRoute(listingId: String) = "product_details/$listingId"
    }
    object ServiceDetails : NavRoutes("service_details/{serviceId}") {
        fun createRoute(serviceId: String) = "service_details/$serviceId"
    }

    // Seller Hub
    object SellerHub : NavRoutes("seller_hub")
    object CreateListing : NavRoutes("create_listing")
    object EditListing : NavRoutes("edit_listing/{listingId}") {
        fun createRoute(listingId: String) = "edit_listing/$listingId"
    }
    object MyListings : NavRoutes("my_listings")
    object MyServices : NavRoutes("my_services")
    object SellerOrders : NavRoutes("seller_orders")
    object Orders : NavRoutes("orders?initialTab={initialTab}") {
        fun createRoute(initialTab: String = "purchases") = "orders?initialTab=$initialTab"
    }

    // Administration
    object AdminDashboard : NavRoutes("admin_dashboard")
    object AdminVerifications : NavRoutes("admin_verifications")
    object AdminReports : NavRoutes("admin_reports")
    object AdminUsers : NavRoutes("admin_users")
    object AdminListings : NavRoutes("admin_listings")
    object AdminRoles : NavRoutes("admin_roles")
    object AdminPermissions : NavRoutes("admin_permissions")
}
