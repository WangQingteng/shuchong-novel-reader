package com.java.myapplication.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.java.myapplication.model.Book

/**
 * 书架本地存储
 */
class BookshelfStorage(private val context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("bookshelf", Context.MODE_PRIVATE)

    /**
     * 保存书籍到书架
     */
    fun saveBook(book: Book) {
        val books = getAllBooks().toMutableList()
        val idx = books.indexOfFirst { it.id == book.id }
        if (idx >= 0) {
            books[idx] = book
        } else {
            books.add(0, book)
        }
        saveAll(books)
    }

    /**
     * 从书架移除书籍
     */
    fun removeBook(bookId: String) {
        val books = getAllBooks().filter { it.id != bookId }
        saveAll(books)
    }

    /**
     * 获取所有书架上的书
     */
    fun getAllBooks(): List<Book> {
        val json = prefs.getString("books", "") ?: ""
        if (json.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<Book>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 检查书籍是否在书架上
     */
    fun isBookOnShelf(bookId: String): Boolean {
        return getAllBooks().any { it.id == bookId }
    }

    /**
     * 清空书架
     */
    fun clear() {
        prefs.edit().remove("books").apply()
    }

    private fun saveAll(books: List<Book>) {
        prefs.edit().putString("books", gson.toJson(books)).apply()
    }
}
