package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LogManageScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = viewModel()
) {
    // ViewModel에서 로그 불러오기
    val logs by viewModel.logs.observeAsState(emptyList())
    val listState = rememberLazyListState()
    var headerDate by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.fetchLogs() }

    // 초기 헤더 날짜는 첫 로그 기준
    LaunchedEffect(logs) {
        if (logs.isNotEmpty()) {
            headerDate = formatHeaderDateFromTs(logs.first().timestamp)
        } else {
            headerDate = ""
        }
    }

    // 스크롤 시, 화면 상단(첫 가시 아이템)의 날짜로 갱신
    LaunchedEffect(listState, logs) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in logs.indices) {
                    headerDate = formatHeaderDateFromTs(logs[idx].timestamp)
                }
            }
    }

    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 60.dp)
        ) {
            // 헤더: 뒤로가기 + 중앙 타이틀
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_page_move_left),
                    contentDescription = "뒤로가기",
                    colorFilter = ColorFilter.tint(Gray800),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(24.dp)
                        .clickable { navController?.popBackStack() }
                )
                Text(
                    text = "기록",
                    color = Gray900,
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold))
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.height(60.dp))

            // 공간/집 선택 드롭다운 제거 -> 고정 텍스트 "우리 집"
            Text(
                text = "우리 집",
                color = Gray900,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
            )

            Spacer(Modifier.height(16.dp))

            // 날짜
            Text(
                text = headerDate,
                color = Gray600,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
            )

            Spacer(Modifier.height(12.dp))

            // 로그 카드 (무한 스크롤 LazyColumn)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.dp, Gray50),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 나머지 공간을 모두 차지
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(logs) { log ->
                        LogRecordItem(log)
                        // 마지막 아이템이 아닌 경우 Divider 추가
                        if (log != logs.lastOrNull()) {
                            Divider(
                                color = Gray50,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

@Preview
@Composable
private fun LogManageScreenPreview() {
    EeumTheme(dynamicColor = false) {
        LogManageScreen()
    }
}

private fun formatHeaderDateFromTs(ts: Long): String {
    val zdt = Instant.ofEpochMilli(ts).atZone(ZoneId.of("Asia/Seoul"))
    val fmt = DateTimeFormatter.ofPattern("M.d EEEE", Locale.KOREAN)
    return zdt.format(fmt)
}

@Composable
private fun LogRecordItem(record: LogRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 시간 영역
        Column(modifier = Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = record.period,
                color = Gray500,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = record.time,
                color = Gray800,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
            )
        }
        Spacer(Modifier.width(16.dp))
        // 내용 영역
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.device,
                color = Gray900,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = record.location,
                color = Gray500,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = record.status,
                color = Gray500,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
            )
        }
    }
}
