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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import kotlin.math.roundToInt

private val TabBg = Color(0xFFF5F5F5)
private val TextUnselected = Color(0xFF4B5563)
private val TextSelected = Color(0xFF007BFF)

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun RoutineScreen(navController: NavController) {
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
                    modifier = Modifier.clickable {
                        navController.navigate("createRoutineFirst") // ROUTE_CREATE_ROUTINE_FIRST
                    },
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
    // 전체 컨테이너 가로(px)
    var containerWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val height = 52.dp
    val corner = 8.dp
    val capsuleCorner = 6.dp
    val capsuleOuterPad = 4.dp
    val count = titles.size.coerceAtLeast(1)

    // 현재 캡슐의 좌측 x(px). 0 ~ (count-1)*tabWidthPx
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // 탭 너비(px)
    val tabWidthPx = remember(containerWidthPx, count) {
        if (count == 0) 0f else containerWidthPx / count
    }

    // 외부 선택이 바뀌면(페이지 스와이프 등) 드래그 중이 아닐 때 위치 동기화
    LaunchedEffect(selectedIndex, tabWidthPx, isDragging) {
        if (!isDragging && tabWidthPx > 0f) {
            dragOffsetPx = selectedIndex * tabWidthPx
        }
    }

    // 애니메이션된 캡슐
    val animatedStartDp: Dp by animateDpAsState(
        targetValue = with(density) { (dragOffsetPx / density.density).dp } + capsuleOuterPad,
        label = "capsuleStart"
    )
    val capsuleWidthDp: Dp by animateDpAsState(
        targetValue = with(density) { (tabWidthPx / density.density).dp } - capsuleOuterPad * 2,
        label = "capsuleWidth"
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(TabBg)
            .onGloballyPositioned { containerWidthPx = it.size.width.toFloat() }
    ) {
        // 하얀 캡슐
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(capsuleCorner),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = capsuleOuterPad)
                .offset(x = animatedStartDp)
                .width(capsuleWidthDp)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (tabWidthPx > 0f) {
                            val max = (count - 1) * tabWidthPx
                            dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, max)
                        }
                    },
                    onDragStarted = { isDragging = true },
                    onDragStopped = {
                        isDragging = false
                        if (tabWidthPx > 0f) {
                            // 가장 가까운 탭으로 스냅
                            val target = (dragOffsetPx / tabWidthPx).roundToInt()
                                .coerceIn(0, count - 1)
                            onSelect(target) // 상위에서 pagerState.animateScrollToPage 호출됨
                        }
                    }
                )
        ) {}

        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) TextSelected else TextUnselected
                    )
                }
            }
        }
    }
}

