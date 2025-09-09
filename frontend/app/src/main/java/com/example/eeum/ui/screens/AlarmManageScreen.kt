package com.example.eeum.ui.screens

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.text.format.DateUtils
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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

private data class AlarmItem(
    val title: String,
    val timeMillis: Long // 절대 시간 (epoch millis)
)

@Preview
@Composable
fun AlarmManageScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val items = remember {
        val now = System.currentTimeMillis()
        listOf(
            AlarmItem(title = "알림", timeMillis = now - (24 * 60 * 60 * 1000L)), // 1일 전
            AlarmItem(title = "테스트", timeMillis = now - (5 * 365 * 24 * 60 * 60 * 1000L)), // 약 5년 전 (2019년 가정)
            AlarmItem(title = "알림 테스트", timeMillis = now - (6 * 365 * 24 * 60 * 60 * 1000L)), // 약 6년 전 (2018년 가정)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(colors = listOf(Color(0xFFB4E3FD), Color(0xFFCCFCFF)))
            )
            .padding(horizontal = 16.dp, vertical = 40.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더
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
                    text = "알림",
                    color = Gray900,
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold))
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.height(60.dp))

            // 알림 리스트 카드
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),

                ) {
                    itemsIndexed(items) { index, item ->
                        AlarmRow(item)
                        if (index < items.size - 1) {
                            Divider(
                                color = Gray50,
                                modifier = Modifier.padding(horizontal = 60.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(item: AlarmItem) {
    // 1분마다 자동 갱신되는 상대 시간 표시
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // 1분 = 60,000ms
            currentTime = System.currentTimeMillis()
        }
    }
    
    val relativeTime = remember(item.timeMillis, currentTime) {
        DateUtils.getRelativeTimeSpanString(
            item.timeMillis,
            currentTime,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            // 아이콘 원형 배지
            Surface(
                color = Blue500,
                shape = RoundedCornerShape(9999.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_alarm),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .size(42.dp)
                        .padding(10.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Gray800,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold))
                    )
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = relativeTime,
                    color = Gray500,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium))
                    )
                )
            }
    }
}
