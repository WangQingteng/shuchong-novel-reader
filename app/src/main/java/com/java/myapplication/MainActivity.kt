package com.java.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.java.myapplication.model.Book
import com.java.myapplication.ui.screens.BookDetailScreen
import com.java.myapplication.ui.screens.BookshelfScreen
import com.java.myapplication.ui.screens.ReaderScreen
import com.java.myapplication.ui.screens.SearchScreen
import com.java.myapplication.ui.theme.MyApplicationTheme
import com.java.myapplication.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BookWormApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookWormApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 底部导航栏显示在首页（搜索和书架）
    val showBottomBar = currentRoute in listOf("search", "bookshelf")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                        label = { Text("搜索") },
                        selected = currentRoute == "search",
                        onClick = {
                            if (currentRoute != "search") {
                                navController.navigate("search") {
                                    popUpTo("search") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Book, contentDescription = "书架") },
                        label = { Text("书架") },
                        selected = currentRoute == "bookshelf",
                        onClick = {
                            if (currentRoute != "bookshelf") {
                                navController.navigate("bookshelf") {
                                    popUpTo("bookshelf") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "search",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        viewModel.selectBook(book)
                        navController.navigate("book_detail/${book.id}")
                    }
                )
            }

            composable("bookshelf") {
                BookshelfScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        viewModel.selectBook(book)
                        navController.navigate("book_detail/${book.id}")
                    }
                )
            }

            composable(
                route = "book_detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { entry ->
                val bookId = entry.arguments?.getString("bookId")
                val selectedBook by viewModel.selectedBook.collectAsState()
                val shelfBooks by viewModel.bookshelfBooks.collectAsState()
                val currentBook: Book? = remember(bookId, selectedBook, shelfBooks) {
                    selectedBook?.takeIf { it.id == bookId }
                        ?: shelfBooks.firstOrNull { it.id == bookId }
                }
                if (currentBook != null) {
                    BookDetailScreen(
                        viewModel = viewModel,
                        book = currentBook,
                        onChapterClick = { chapter ->
                            viewModel.selectChapter(chapter)
                            viewModel.loadChapterContent(chapter)
                            navController.navigate("reader/${chapter.id}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = "reader/{chapterId}",
                arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
            ) {
                val selectedChapter by viewModel.selectedChapter.collectAsState()
                val chapters by viewModel.chapters.collectAsState()
                if (selectedChapter != null) {
                    ReaderScreen(
                        viewModel = viewModel,
                        chapter = selectedChapter!!,
                        chapters = chapters,
                        onBack = { navController.popBackStack() },
                        onChapterChange = { chapter ->
                            viewModel.selectChapter(chapter)
                            viewModel.loadChapterContent(chapter)
                        }
                    )
                }
            }
        }
    }
}
