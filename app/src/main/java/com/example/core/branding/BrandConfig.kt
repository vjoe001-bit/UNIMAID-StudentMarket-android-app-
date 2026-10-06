package com.example.core.branding

import com.example.R

/**
 * Centralized Branding and Attribution Constants for UNIMAID StudentMarket.
 * Exclusively for University of Maiduguri students.
 */
object BrandConfig {
    const val APP_NAME = "UNIMAID StudentMarket"
    const val APP_SHORT_NAME = "UNIMAID Market"
    const val UNIVERSITY_NAME = "University of Maiduguri"
    const val UNIVERSITY_SHORT = "UNIMAID"
    const val CREATOR_ATTRIBUTION = "PrinceJoe.X"
    const val VERSION_NAME = "1.0.0 (Phase B - UI System)"

    /**
     * Core Campus Business Rule:
     * There is NO online payment gateway in the application.
     * All transactions are carried out via physical/in-person meetups on campus.
     */
    const val BUSINESS_POLICY_NO_ONLINE_PAYMENT = 
        "Transactions are handled between students through physical/in-person delivery or agreed arrangements within UNIMAID. No online payments."

    const val PHYSICAL_MEETUP_NOTICE = 
        "Always meet in public, well-lit campus areas such as the Senate Building, Complex, Faculty Quarters, or University Library."

    /**
     * Official University of Maiduguri crest logo asset.
     */
    val OFFICIAL_LOGO_RES = R.drawable.unimaid_crest
}
