package io.github.arashiyama11.tinybudget.data.repository

import io.github.arashiyama11.tinybudget.data.local.dao.TransactionDao
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAll()
    }

    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endOfMonth = cal.timeInMillis
        return transactionDao.getByMonth(startOfMonth, endOfMonth)
    }

    suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
}
