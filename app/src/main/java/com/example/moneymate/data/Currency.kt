package com.example.moneymate.data

enum class Currency(val symbol: String, val code: String, val displayName: String) {
    USD("$", "USD", "US Dollar"),
    EUR("€", "EUR", "Euro"),
    GBP("£", "GBP", "British Pound"),
    JPY("¥", "JPY", "Japanese Yen"),
    AUD("A$", "AUD", "Australian Dollar"),
    CAD("C$", "CAD", "Canadian Dollar"),
    CHF("Fr", "CHF", "Swiss Franc"),
    CNY("¥", "CNY", "Chinese Yuan"),
    INR("₹", "INR", "Indian Rupee"),
    LKR("රු", "LKR", "Sri Lankan Rupee"),
    SGD("S$", "SGD", "Singapore Dollar"),
    AED("د.إ", "AED", "UAE Dirham"),
    SAR("﷼", "SAR", "Saudi Riyal"),
    QAR("﷼", "QAR", "Qatari Riyal"),
    MYR("RM", "MYR", "Malaysian Ringgit"),
    THB("฿", "THB", "Thai Baht"),
    IDR("Rp", "IDR", "Indonesian Rupiah"),
    PHP("₱", "PHP", "Philippine Peso"),
    KRW("₩", "KRW", "South Korean Won"),
    NZD("NZ$", "NZD", "New Zealand Dollar");

    companion object {
        fun fromCode(code: String): Currency? = values().find { it.code == code }
    }
} 
