package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R
import com.example.eeum.ui.theme.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation.NavController

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 60.dp)
        ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "우리 집",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold)),
                    color = Color(0xFF1F2937),
                )
            )
            Image(
                painter = painterResource(id = R.drawable.ic_alarm),
                contentDescription = "알림",
                colorFilter = ColorFilter.tint(Gray600),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { navController?.navigate("alarm_manage") }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Profile row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_user_photo),
                contentDescription = "프로필",
                modifier = Modifier
                    .size(50.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "태훈태훈",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(id = R.drawable.ic_page_move),
                contentDescription = "이동",
                colorFilter = ColorFilter.tint(Gray300),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(15.dp))

        // Membership pill
        Surface(
            color = Blue100,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "멤버십 없음",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Color(0xFF1D4ED8),
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Blue600,
                    border = BorderStroke(1.dp, Color(0xFF1D4ED8))
                ) {
                    Text(
                        text = "가입",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                            color = Color(0xFFFFFFFF),
                        ),
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Services section
        Text(
            text = "서비스",
            color = Gray600,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium)),
            ),
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.dp, Gray50),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceItem(
                    bg = Purple100, iconTint = Purple600,
                    icon = R.drawable.ic_search, label = "찾기"
                )
                Box(modifier = Modifier.clickable { navController?.navigate("routine") }) {
                    ServiceItem(
                        bg = Yellow200, iconTint = Amber600,
                        icon = R.drawable.ic_routine, label = "내 루틴"
                    )
                }
                Box(modifier = Modifier.clickable { navController?.navigate("log_manage") }) {
                    ServiceItem(
                        bg = Color(0xFFDEFEC3), iconTint = Color(0xFF14AE5C),
                        icon = R.drawable.ic_log_management, label = "로그 기록"
                    )
                }
                ServiceItem(
                    bg = Color(0xFFC3DCFE), iconTint = Blue600,
                    icon = R.drawable.ic_membership, label = "멤버십"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Extra section
        Text(
            text = "추가 기능",
            color = Gray600,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.dp, Gray50),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MenuListItem(icon = R.drawable.ic_announcement, tint = Orange500, text = "공지사항")
                androidx.compose.material3.Divider(color = Gray50)
                MenuListItem(icon = R.drawable.ic_user_manual, tint = Blue500, text = "사용 설명서")
                androidx.compose.material3.Divider(color = Gray50)
                MenuListItem(icon = R.drawable.ic_blueprint, tint = Color(0xFF9333EA), text = "평면도 관리")
                androidx.compose.material3.Divider(color = Gray50)
                MenuListItem(icon = R.drawable.ic_hub_management, tint = Color(0xFF14AE5C), text = "허브 관리")
                androidx.compose.material3.Divider(color = Gray50)
                MenuListItem(icon = R.drawable.ic_setting, tint = Gray500, text = "설정")
            }
        }
        }
    }

    return


}

@Composable
private fun ServiceItem(bg: Color, iconTint: Color, icon: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(9999.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = label,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Gray600,
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        )
    }
}

@Composable
private fun MenuListItem(icon: Int, tint: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = text,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Gray800,
            style = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        )
    }
}
