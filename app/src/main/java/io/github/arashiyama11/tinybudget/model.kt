package io.github.arashiyama11.tinybudget

import android.os.Parcelable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
value class TransactionId(val value: String): Parcelable

@Parcelize
@JvmInline
value class CategoryId(val value: String): Parcelable

@JvmInline
value class TriggerAppId(val value: String)

@Parcelize
@JvmInline
value class Amount(val value: Long): Parcelable {
    init {
        require(value >= 0) { "Amount must be non-negative" }
    }
}

@Parcelize
data class Transaction(
    val id: TransactionId,
    val amount: Amount,
    val date: Long,
    val category: Category,
    val note: String,
): Parcelable

fun Transaction.toEntity(): io.github.arashiyama11.tinybudget.data.local.entity.Transaction {
    return io.github.arashiyama11.tinybudget.data.local.entity.Transaction(
        id = this.id.value.toInt(),
        amount = this.amount.value,
        categoryId = this.category.id.value.toInt(),
        note = this.note,
        timestamp = this.date
    )
}

@Parcelize
data class Category(
    val id: CategoryId,
    val name: String,
    val isEnabled: Boolean = true,
): Parcelable

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
