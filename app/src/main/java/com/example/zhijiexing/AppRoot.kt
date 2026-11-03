package com.example.zhijiexing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import com.example.zhijiexing.ui.screens.BankScreen
import com.example.zhijiexing.ui.screens.BookmarksScreen
import com.example.zhijiexing.ui.screens.CertificatesScreen
import com.example.zhijiexing.ui.screens.GrowthScreen
import com.example.zhijiexing.ui.screens.HomeScreen
import com.example.zhijiexing.ui.screens.KnowledgeDetailScreen
import com.example.zhijiexing.ui.screens.KnowledgeListScreen
import com.example.zhijiexing.ui.screens.LoginScreen
import com.example.zhijiexing.ui.screens.MineScreen
import com.example.zhijiexing.ui.screens.QuizScreen
import com.example.zhijiexing.ui.screens.RecordsScreen
import com.example.zhijiexing.ui.screens.ResultScreen
import com.example.zhijiexing.ui.screens.ReviewDetailScreen
import com.example.zhijiexing.ui.screens.ReviewListScreen
import com.example.zhijiexing.ui.screens.SavedQuestionScreen
import com.example.zhijiexing.ui.screens.SettingsScreen
import com.example.zhijiexing.ui.screens.SplashScreen
import com.example.zhijiexing.ui.screens.WrongBookScreen
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.theme.Background
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.PrimarySoft
import kotlinx.coroutines.delay

@Composable
fun ZhijiexingApp() {
    val context = LocalContext.current.applicationContext
    val appState = remember { AppState(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                appState.repository.recordStudyMinute()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) appState.onAppResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = appState.stack.isNotEmpty()) {
        appState.pop()
    }
    BackHandler(enabled = appState.session != null && appState.stack.isEmpty() && appState.selectedTab != 0) {
        appState.goTab(0)
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        if (appState.booting) {
            SplashScreen()
        } else if (appState.session == null) {
            LoginScreen(appState)
        } else {
            MainScreen(appState)
        }

        AnimatedContent(
            targetState = appState.stack.lastOrNull(),
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = androidx.compose.animation.core.tween(260)) +
                        fadeIn(androidx.compose.animation.core.tween(220))) togetherWith
                        fadeOut(androidx.compose.animation.core.tween(160))
                } else {
                    fadeIn(androidx.compose.animation.core.tween(160)) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = androidx.compose.animation.core.tween(240)) +
                            fadeOut(androidx.compose.animation.core.tween(200)))
                }
            },
            label = "detail"
        ) { screen ->
            if (screen != null) {
                Box(Modifier.fillMaxSize().background(Background)) {
                    ScreenHost(appState, screen)
                }
            }
        }

        SnackbarHost(
            hostState = appState.snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 96.dp)
        ) { data ->
            Box(
                Modifier
                    .shadow(10.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimarySoft)
                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(data.visuals.message, color = PrimaryDeep, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MainScreen(appState: AppState) {
    val pagerState = rememberPagerState(initialPage = appState.selectedTab) { 4 }

    // 底部导航点击等外部切换：让 Pager 平滑滚到对应页面
    LaunchedEffect(appState.selectedTab) {
        if (pagerState.currentPage != appState.selectedTab) {
            pagerState.animateScrollToPage(appState.selectedTab)
        }
    }
    // 手势拖动停下后，再把当前页同步回应用状态
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != appState.selectedTab) appState.goTab(page)
        }
    }

    androidx.compose.foundation.layout.Column(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            // 背景只画一份、固定不动，滑动时页面内容在其上移动，避免两页背景拼接出割裂感
            DecoBackground()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // 二级页面打开时不响应滑动，避免答题/详情页误切主页面
                userScrollEnabled = appState.stack.isEmpty()
            ) { page ->
                when (page) {
                    0 -> HomeScreen(appState)
                    1 -> BankScreen(appState)
                    2 -> GrowthScreen(appState)
                    else -> MineScreen(appState)
                }
            }
        }
        com.example.zhijiexing.ui.components.BottomNavBar(appState)
    }
}

@Composable
private fun ScreenHost(appState: AppState, screen: Screen) {
    when (screen) {
        is Screen.Quiz -> QuizScreen(appState, screen.paperId, screen.paperName)
        is Screen.Result -> ResultScreen(appState, screen.attemptId)
        is Screen.ReviewList -> ReviewListScreen(appState, screen.attemptId)
        is Screen.ReviewDetail -> ReviewDetailScreen(appState, screen.attemptId, screen.index)
        is Screen.SavedQuestionDetail -> SavedQuestionScreen(appState, screen.questionId, screen.bookmark)
        Screen.Records -> RecordsScreen(appState)
        Screen.Certificates -> CertificatesScreen(appState)
        Screen.WrongBook -> WrongBookScreen(appState)
        Screen.Bookmarks -> BookmarksScreen(appState)
        Screen.Settings -> SettingsScreen(appState)
        Screen.KnowledgeList -> KnowledgeListScreen(appState)
        is Screen.KnowledgeDetail -> KnowledgeDetailScreen(appState, screen.id)
    }
}
