package io.github.arashiyama11.tinybudget

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf

@JvmInline
value class TransactionId(val value: String)

@JvmInline
value class CategoryId(val value: String)

@JvmInline
value class TriggerAppId(val value: String)

@JvmInline
value class Amount(val value: Long) {
    init {
        require(value >= 0) { "Amount must be non-negative" }
    }
}

data class Transaction(
    val id: TransactionId,
    val amount: Amount,
    val date: Long,
    val category: Category,
    val note: String,
)

data class Category(
    val id: CategoryId,
    val name: String,
    val isEnabled: Boolean = true,
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalAmount: Amount,
    val categoryWiseAmounts: ImmutableMap<Category, Amount>,
    val transactions: ImmutableList<Transaction> = persistentListOf()
)

data class TriggerApp(
    val id: TriggerAppId,
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean
)
