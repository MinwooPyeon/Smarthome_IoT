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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.theme.*

// 로그 데이터 모델
data class LogRecord(
    val id: String,
    val timestamp: Long,
    val period: String, // "오전" | "오후"
    val time: String,   // "10:33"
    val device: String,
    val location: String,
    val status: String
)

@Preview
@Composable
fun LogManageScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    // 정적 데이터 사용
    val spaces = listOf("우리 집", "원룸")
    var selectedSpace by remember { mutableStateOf("우리 집") }
    val selectedDate = "8. 26. 화요일"
    
    // 드롭다운 메뉴 상태
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    // 더미 로그 데이터
    val logs = remember {
        listOf(
            LogRecord(
                id = "1",
                timestamp = System.currentTimeMillis(),
                period = "오후",
                time = "10:33",
                device = "스탠드형 에어컨",
                location = "거실",
                status = "전원: 켜짐"
            ),
            LogRecord(
                id = "2",
                timestamp = System.currentTimeMillis() - 1000,
                period = "오후",
                time = "9:52",
                device = "벽걸이형 에어컨",
                location = "거실",
                status = "전원: 켜짐"
            ),
            LogRecord(
                id = "3",
                timestamp = System.currentTimeMillis() - 2000,
                period = "오후",
                time = "9:33",
                device = "벽걸이형 에어컨",
                location = "거실",
                status = "전원: 켜짐"
            ),
            LogRecord(
                id = "4",
                timestamp = System.currentTimeMillis() - 3000,
                period = "오전",
                time = "11:41",
                device = "스탠드형 에어컨",
                location = "거실",
                status = "전원: 켜짐"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(colors = listOf(Color(0xFFB4E3FD), Color(0xFFCCFCFF)))
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            // 공간/집 선택 (드롭다운)
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDropdownExpanded = true }
                ) {
                    Text(
                        text = selectedSpace,
                        color = Gray900,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.goormsansbold))
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_page_move_under),
                        contentDescription = "목록 열기",
                        colorFilter = ColorFilter.tint(Gray600),
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    spaces.forEach { space ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = space,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily(Font(R.font.goormsansmedium))
                                    )
                                )
                            },
                            onClick = {
                                selectedSpace = space
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 날짜
            Text(
                text = selectedDate,
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(logs) { log ->
                        LogRecordItem(log)
                        // 마지막 아이템이 아닌 경우 Divider 추가
                        if (log != logs.last()) {
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