package com.example.eeum.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.eeum.R
import com.example.eeum.ui.pages.MyRoutinePage
import com.example.eeum.ui.pages.RecommendRoutinePage
import kotlinx.coroutines.launch

private val TabBg = Color(0xFFF5F5F5)
private val TextUnselected = Color(0xFF4B5563)
private val TextSelected = Color(0xFF007BFF)

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun RoutineScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("내 루틴", "추천 루틴")

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
                Text(
                    text = "루틴 관리",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "추가",
                    color = TextSelected,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SegmentedTabRow(
                selectedIndex = pagerState.currentPage,
                titles = tabs,
                onSelect = { idx -> coroutineScope.launch { pagerState.animateScrollToPage(idx) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pager: 각 페이지에 원하는 Composable 배치
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> MyRoutinePage()
                    1 -> RecommendRoutinePage()
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabRow(
    selectedIndex: Int,
    titles: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TabBg),
        contentAlignment = Alignment.Center
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = Color.Transparent,
            contentColor = Color.Unspecified,
            divider = {}, // 밑줄 제거
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedIndex])
                            .padding(4.dp)   // 52dp - 8dp = 44dp 캡슐
                            .fillMaxHeight()
                            .zIndex(-1f)     // 텍스트 뒤로
                    ) {}
                }
            }
        ) {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Tab(
                    selected = selected,
                    onClick = { onSelect(index) },
                    selectedContentColor = TextSelected,
                    unselectedContentColor = TextUnselected,
                    text = {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selected) TextSelected else TextUnselected
                        )
                    }
                )
            }
        }
    }
}

