package com.expensetracker.tk.data

import org.json.JSONObject

data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,          // yyyy-MM-dd
    val notes: String,
    val isRecurring: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val dirty: Boolean = false,
    val deleted: Boolean = false,
    val recurDays: Int = 0,        // local-only: 1=daily, 7=weekly, 30=monthly(30d)
    val recurParent: String? = null // local-only: set on auto-generated copies
)

data class BudgetRow(
    val id: String,
    val category: String,     // category id, '__daily_limit__', '__monthly_target__'
    val amount: Double,
    val yearMonth: String     // '' when not applicable
)

data class BorrowLend(
    val id: String,
    val type: String,          // "borrow" | "lend"
    val personName: String,
    val amount: Double,
    val date: String,
    val reminderDate: String?, // yyyy-MM-dd or null
    val status: String,        // "pending" | "settled"
    val notes: String,
    val updatedAt: Long,
    val dirty: Boolean = false,
    val deleted: Boolean = false
)

data class QuickPreset(
    val id: String,
    val label: String,
    val title: String,
    val amount: Double,
    val category: String
)

object Categories {
    data class Cat(val id: String, val name: String, val icon: String, val color: Long)

    val all = listOf(
        Cat("food", "Food", "restaurant", 0xFFF59E0B),
        Cat("transport", "Transport", "directions_bus", 0xFF3B82F6),
        Cat("shopping", "Shopping", "shopping_bag", 0xFFEC4899),
        Cat("entertainment", "Fun", "sports_esports", 0xFF8B5CF6),
        Cat("bills", "Bills", "receipt_long", 0xFFEF4444),
        Cat("health", "Health", "favorite", 0xFF10B981),
        Cat("education", "Education", "school", 0xFF06B6D4),
        Cat("other", "Other", "more_horiz", 0xFF6B7280)
    )

    fun byId(id: String): Cat = all.firstOrNull { it.id == id } ?: all.last()
}

fun Expense.toJson(userKey: String): JSONObject = JSONObject().apply {
    put("id", id)
    put("user_pin", userKey)
    put("title", title)
    put("amount", amount)
    put("category", category)
    put("date", date)
    put("notes", notes)
    put("is_recurring", isRecurring)
    put("updated_at", updatedAt)
}

fun BorrowLend.toJson(userKey: String): JSONObject = JSONObject().apply {
    put("id", id)
    put("user_pin", userKey)
    put("type", type)
    put("person_name", personName)
    put("amount", amount)
    put("date", date)
    put("reminder_date", reminderDate ?: JSONObject.NULL)
    put("status", status)
    put("notes", notes)
    put("updated_at", updatedAt)
}
