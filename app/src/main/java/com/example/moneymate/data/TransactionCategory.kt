package com.example.moneymate.data

enum class TransactionCategory(
    val displayName: String,
    val type: TransactionType,
    val colorHex: Int
) {
    // Income categories
    SALARY("Salary", TransactionType.INCOME, 0xFF16A34A.toInt()),
    BONUS("Bonus", TransactionType.INCOME, 0xFF10B981.toInt()),
    INVESTMENT("Investment", TransactionType.INCOME, 0xFF0EA5E9.toInt()),
    OTHER_INCOME("Other Income", TransactionType.INCOME, 0xFF14B8A6.toInt()),

    // Expense categories
    FOOD("Food & Dining", TransactionType.EXPENSE, 0xFFEF4444.toInt()),
    TRANSPORTATION("Transportation", TransactionType.EXPENSE, 0xFFF97316.toInt()),
    HOUSING("Housing & Rent", TransactionType.EXPENSE, 0xFF8B5CF6.toInt()),
    UTILITIES("Utilities", TransactionType.EXPENSE, 0xFF06B6D4.toInt()),
    ENTERTAINMENT("Entertainment", TransactionType.EXPENSE, 0xFFEC4899.toInt()),
    SHOPPING("Shopping", TransactionType.EXPENSE, 0xFFF59E0B.toInt()),
    HEALTH("Health & Fitness", TransactionType.EXPENSE, 0xFF10B981.toInt()),
    EDUCATION("Education", TransactionType.EXPENSE, 0xFF3B82F6.toInt()),
    TRAVEL("Travel", TransactionType.EXPENSE, 0xFF6366F1.toInt()),
    OTHER_EXPENSE("Other Expense", TransactionType.EXPENSE, 0xFF64748B.toInt());

    companion object {
        fun forType(type: TransactionType): List<TransactionCategory> =
            values().filter { it.type == type }

        fun expenseCategories(): List<TransactionCategory> =
            forType(TransactionType.EXPENSE)

        fun incomeCategories(): List<TransactionCategory> =
            forType(TransactionType.INCOME)

        fun fromInput(value: String): TransactionCategory? =
            values().find {
                it.name.equals(value.trim(), ignoreCase = true) ||
                    it.displayName.equals(value.trim(), ignoreCase = true)
            }
    }
}
