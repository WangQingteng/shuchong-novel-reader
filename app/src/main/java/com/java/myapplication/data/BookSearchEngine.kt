package com.java.myapplication.data

import com.java.myapplication.model.Book
import com.java.myapplication.model.BookSource
import com.java.myapplication.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * 书籍搜索引擎 - 支持多个书源
 */
class BookSearchEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
                )
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * 搜索书籍（多源聚合）
     */
    suspend fun searchBooks(query: String, source: BookSource = BookSource.DEFAULT): List<Book> {
        return withContext(Dispatchers.IO) {
            when (source) {
                BookSource.DEFAULT -> searchAllSources(query)
                BookSource.BIQUGE -> searchBiquge(query)
                BookSource.QIDIAN -> searchQidian(query)
                BookSource.GITEE -> searchGitee(query)
                BookSource.ZHUISHU -> searchZhishu(query)
            }
        }
    }

    /**
     * 聚合搜索所有书源
     */
    private suspend fun searchAllSources(query: String): List<Book> {
        val results = mutableListOf<Book>()
        try {
            results.addAll(searchBiquge(query))
        } catch (_: Exception) {}
        try {
            results.addAll(searchQidian(query))
        } catch (_: Exception) {}
        try {
            results.addAll(searchZhishu(query))
        } catch (_: Exception) {}
        return results.distinctBy { it.title + it.author }
    }

    /**
     * 获取书籍章节列表
     */
    suspend fun getBookChapters(book: Book): List<Chapter> {
        return withContext(Dispatchers.IO) {
            try {
                when (book.source) {
                    BookSource.BIQUGE -> parseBiqugeChapters(book.sourceUrl)
                    BookSource.QIDIAN -> parseQidianChapters(book.sourceUrl)
                    else -> emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * 获取章节内容
     */
    suspend fun getChapterContent(chapter: Chapter): String {
        return withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument(chapter.url)
                // 通用解析 - 尝试多种选择器
                val content = doc.select("#content, .content, .chapter-content, .read-content, " +
                        "#chaptercontent, .txtnav, .book-content")
                    .firstOrNull()?.text() ?: doc.body().text()

                if (content.length > 100) content else "章节内容获取失败，请重试。"
            } catch (e: Exception) {
                "内容加载失败: ${e.message}"
            }
        }
    }

    // ======================== 笔趣阁搜索 ========================

    private suspend fun searchBiquge(query: String): List<Book> {
        return try {
            val url = "https://www.biquge.com/search?q=${java.net.URLEncoder.encode(query, "utf-8")}"
            val doc = fetchDocument(url)
            parseBiqugeSearchResults(doc, query)
        } catch (_: Exception) {
            // 备用搜索
            searchBiqugeBackup(query)
        }
    }

    private fun parseBiqugeSearchResults(doc: Document, query: String): List<Book> {
        val books = mutableListOf<Book>()
        // 尝试多种选择器
        val items = doc.select(".result-item, .search-result li, .book-item, " +
                "#searchresult li, .result-list li, .list-group-item")
        if (items.isNotEmpty()) {
            for (item in items) {
                val titleEl = item.select("a[href]").firstOrNull()
                val title = titleEl?.text()?.trim() ?: continue
                val link = titleEl.attr("abs:href")
                val author = item.select(".author, .gray, .book-author, span:contains(作者)")
                    .firstOrNull()?.text()?.replace("作者[：:]?", "")?.trim() ?: ""
                val summary = item.select(".intro, .desc, .summary, .book-desc")
                    .firstOrNull()?.text()?.trim() ?: ""
                val cover = item.select("img[src]").firstOrNull()?.attr("abs:src") ?: ""

                books.add(
                    Book(
                        id = "biquge_${title.hashCode()}",
                        title = title,
                        author = author,
                        summary = summary,
                        cover = cover,
                        source = BookSource.BIQUGE,
                        sourceUrl = link
                    )
                )
            }
        }
        // 如果常规选择器没找到，尝试直接匹配链接
        if (books.isEmpty()) {
            val links = doc.select("a[href~=.*(biquge|book|novel|xs).*]")
            for (link in links) {
                val title = link.text().trim()
                if (title.length in 2..30 && !title.contains("首页|搜索|上一页|下一页".toRegex())) {
                    books.add(
                        Book(
                            id = "biquge_${title.hashCode()}",
                            title = title,
                            source = BookSource.BIQUGE,
                            sourceUrl = link.attr("abs:href")
                        )
                    )
                }
            }
        }
        return books
    }

    private suspend fun searchBiqugeBackup(query: String): List<Book> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "utf-8")
            val doc = fetchDocument("https://www.biquge.com/modules/article/search.php?searchkey=$encoded")
            parseBiqugeSearchResults(doc, query)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseBiqugeChapters(url: String): List<Chapter> {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
            .timeout(15000)
            .get()

        val chapters = mutableListOf<Chapter>()
        val links = doc.select(".chapter-list a, #list a, .book-list a, .chapter a, " +
                "ul.chapter li a, .listmain a, dl dd a")

        if (links.isEmpty()) {
            // 尝试通用选择器
            doc.select("a[href~=.*(chapter|Chapter|\\d+\\.html)].*").forEach { link ->
                val href = link.attr("abs:href")
                val text = link.text().trim()
                if (text.isNotBlank() && text.length < 50) {
                    chapters.add(
                        Chapter(
                            id = "ch_${chapters.size}",
                            title = text,
                            index = chapters.size,
                            url = href
                        )
                    )
                }
            }
        } else {
            links.forEachIndexed { index, link ->
                val href = link.attr("abs:href")
                val text = link.text().trim()
                if (text.isNotBlank()) {
                    chapters.add(
                        Chapter(
                            id = "ch_$index",
                            title = text,
                            index = index,
                            url = href
                        )
                    )
                }
            }
        }
        return chapters
    }

    // ======================== 起点搜索 ========================

    private suspend fun searchQidian(query: String): List<Book> {
        return try {
            val doc = fetchDocument("https://www.qidian.com/search?kw=${java.net.URLEncoder.encode(query, "utf-8")}")
            val books = mutableListOf<Book>()
            val items = doc.select(".book-item, .res-book-item, li[data-bid], .book-list li")
            for (item in items) {
                val titleEl = item.select("h2 a, .book-title a, .title a").firstOrNull()
                    ?: item.select("a[href~=.*(book|novel).*]").firstOrNull()
                val title = titleEl?.text()?.trim() ?: continue
                val link = titleEl.attr("abs:href")
                val author = item.select(".author, .book-author, .author-name")
                    .firstOrNull()?.text()?.trim() ?: ""
                val summary = item.select(".intro, .book-desc, .summary, .desc")
                    .firstOrNull()?.text()?.trim() ?: ""

                books.add(
                    Book(
                        id = "qidian_${title.hashCode()}",
                        title = title,
                        author = author,
                        summary = summary,
                        source = BookSource.QIDIAN,
                        sourceUrl = link
                    )
                )
            }
            books
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseQidianChapters(url: String): List<Chapter> {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
                .timeout(15000)
                .get()

            val chapters = mutableListOf<Chapter>()
            val links = doc.select(".chapter-list a, .volume a, .catalog-list a, .cf a")
            links.forEachIndexed { index, link ->
                val href = link.attr("abs:href")
                val text = link.text().trim()
                if (text.isNotBlank() && text.length < 60) {
                    chapters.add(
                        Chapter(
                            id = "qd_ch_$index",
                            title = text,
                            index = index,
                            url = href
                        )
                    )
                }
            }
            chapters
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ======================== 追书搜索 ========================

    private suspend fun searchZhishu(query: String): List<Book> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "utf-8")
            val doc = fetchDocument("https://www.zhishu.com/search?keyword=$encoded")
            val books = mutableListOf<Book>()
            val items = doc.select(".book-item, .search-result-item, .result-item, .novel-item")
            for (item in items) {
                val titleEl = item.select("a[href]").firstOrNull()
                val title = titleEl?.text()?.trim() ?: continue
                val link = titleEl.attr("abs:href")
                val author = item.select(".author, .writer, .book-author").firstOrNull()?.text()?.trim() ?: ""

                books.add(
                    Book(
                        id = "zs_${title.hashCode()}",
                        title = title,
                        author = author,
                        source = BookSource.ZHUISHU,
                        sourceUrl = link
                    )
                )
            }
            books
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ======================== Gitee搜索 ========================

    private suspend fun searchGitee(query: String): List<Book> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "utf-8")
            val doc = fetchDocument("https://search.gitee.com/?q=$encoded&type=repository")
            val books = mutableListOf<Book>()
            val items = doc.select(".project-item, .search-result .item, .repository-item")
            for (item in items) {
                val titleEl = item.select("a[href]").firstOrNull()
                val title = titleEl?.text()?.trim() ?: continue
                val link = titleEl.attr("abs:href")
                val desc = item.select(".project-desc, .description, .desc").firstOrNull()?.text()?.trim() ?: ""

                val keywords = listOf("小说", "书", "book", "novel", "阅读", "电子书")
                if (keywords.any { title.contains(it, ignoreCase = true) }) {
                    books.add(
                        Book(
                            id = "gitee_${title.hashCode()}",
                            title = title,
                            summary = desc,
                            source = BookSource.GITEE,
                            sourceUrl = link
                        )
                    )
                }
            }
            books
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ======================== 通用工具 ========================

    private suspend fun fetchDocument(url: String): Document {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            response.close()
            Jsoup.parse(html)
        }
    }
}
