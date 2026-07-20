package com.java.myapplication.data

import android.content.Context
import com.java.myapplication.model.DownloadStatus
import com.java.myapplication.model.DownloadTask
import com.java.myapplication.model.Book
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * 书籍下载管理器
 */
class DownloadManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadTasks = mutableMapOf<String, DownloadTask>()
    private val activeJobs = mutableMapOf<String, Job>()

    private val _onProgress = mutableListOf<(String, Int) -> Unit>()
    private val _onComplete = mutableListOf<(String) -> Unit>()
    private val _onError = mutableListOf<(String, String) -> Unit>()

    fun onProgress(listener: (String, Int) -> Unit) { _onProgress.add(listener) }
    fun onComplete(listener: (String) -> Unit) { _onComplete.add(listener) }
    fun onError(listener: (String, String) -> Unit) { _onError.add(listener) }

    /**
     * 获取下载目录
     */
    fun getDownloadDir(): File {
        val dir = File(context.getExternalFilesDir(null), "下载书籍")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 下载整本书
     */
    fun downloadBook(book: Book, chapters: List<Book>): Boolean {
        // 实际实现：下载所有章节并保存为txt
        return true
    }

    /**
     * 下载章节内容
     */
    fun downloadChapter(
        bookTitle: String,
        chapterTitle: String,
        content: String
    ): String {
        val bookDir = File(getDownloadDir(), sanitizeFileName(bookTitle))
        if (!bookDir.exists()) bookDir.mkdirs()

        val file = File(bookDir, sanitizeFileName(chapterTitle) + ".txt")
        file.writeText(content)
        return file.absolutePath
    }

    /**
     * 合并书籍所有章节为单个txt文件
     */
    fun mergeToSingleFile(bookTitle: String, chapters: List<Pair<String, String>>): String {
        val file = File(getDownloadDir(), sanitizeFileName(bookTitle) + ".txt")
        val sb = StringBuilder()
        sb.appendLine(bookTitle)
        sb.appendLine("=" .repeat(50))

        chapters.forEachIndexed { index, (title, content) ->
            sb.appendLine()
            sb.appendLine("第${index + 1}章 $title")
            sb.appendLine("-".repeat(30))
            sb.appendLine(content)
            sb.appendLine()
        }

        file.writeText(sb.toString())
        return file.absolutePath
    }

    /**
     * 获取所有已下载的书籍
     */
    fun getDownloadedBooks(): List<File> {
        return getDownloadDir().listFiles()
            ?.filter { it.extension == "txt" || it.isDirectory }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 获取下载任务列表
     */
    fun getDownloadTasks(): List<DownloadTask> = downloadTasks.values.toList()

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }

    /**
     * 清理资源
     */
    fun destroy() {
        scope.cancel()
    }
}
