package io.github.arashiyama11.tinybudget.data.repository

import io.github.arashiyama11.tinybudget.data.local.dao.TransactionDao
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface TransactionRepository {
    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
}


class TransactionRepositoryImpl(private val transactionDao: TransactionDao) :
    TransactionRepository {
    override fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endOfMonth = cal.timeInMillis
        return transactionDao.getByMonth(startOfMonth, endOfMonth)
    }

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
}
