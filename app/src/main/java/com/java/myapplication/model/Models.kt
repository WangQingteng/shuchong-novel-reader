package com.java.myapplication.model

/**
 * 书籍信息数据模型
 */
data class Book(
    val id: String = "",
    val title: String = "未知书名",
    val author: String = "未知作者",
    val cover: String = "",
    val summary: String = "",
    val category: String = "",
    val source: BookSource = BookSource.DEFAULT,
    val sourceUrl: String = "",
    val chapters: List<Chapter> = emptyList(),
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0
)

/**
 * 章节数据模型
 */
data class Chapter(
    val id: String = "",
    val title: String = "未知章节",
    val index: Int = 0,
    val url: String = "",
    val content: String = "",
    val isDownloaded: Boolean = false
)

/**
 * 书源枚举
 */
enum class BookSource(val displayName: String, val baseUrl: String) {
    DEFAULT("默认搜索", ""),
    GITEE("Gitee搜索", "https://search.gitee.com"),
    QIDIAN("起点中文", "https://www.qidian.com"),
    BIQUGE("笔趣阁", "https://www.biquge.com"),
    ZHUISHU("追书神器", "https://www.zhishu.com");

    companion object {
        fun fromName(name: String): BookSource {
            return entries.find { it.displayName == name } ?: DEFAULT
        }
    }
}

/**
 * 搜索状态
 */
sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Success(val results: List<Book>) : SearchState()
    data class Error(val message: String) : SearchState()
}

/**
 * 下载任务状态
 */
enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

/**
 * 下载任务
 */
data class DownloadTask(
    val id: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val chapterId: String = "",
    val chapterTitle: String = "",
    val url: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val filePath: String = ""
)
