package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.java.myapplication.model.Chapter
import com.java.myapplication.ui.viewmodel.MainViewModel

/**
 * 阅读屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    chapter: Chapter,
    chapters: List<Chapter>,
    onBack: () -> Unit,
    onChapterChange: (Chapter) -> Unit
) {
    val content by viewModel.chapterContent.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(chapter.id) {
        viewModel.loadChapterContent(chapter)
    }

    val currentIndex = chapters.indexOfFirst { it.id == chapter.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        chapter.title,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 下载当前章节
                    IconButton(onClick = {
                        if (content.length > 10) {
                            viewModel.downloadChapter(
                                chapters.getOrNull(0)?.title ?: "未知",
                                chapter.title,
                                content
                            )
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "下载章节")
                    }
                }
            )
        },
        bottomBar = {
            // 章节导航
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上一章
                    TextButton(
                        onClick = {
                            if (currentIndex > 0) {
                                onChapterChange(chapters[currentIndex - 1])
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上一章")
                    }

                    Text(
                        text = "${currentIndex + 1}/${chapters.size}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // 下一章
                    TextButton(
                        onClick = {
                            if (currentIndex < chapters.size - 1) {
                                onChapterChange(chapters[currentIndex + 1])
                            }
                        },
                        enabled = currentIndex < chapters.size - 1
                    ) {
                        Text("下一章")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (content == "加载中...") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 32.sp
                        ),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
