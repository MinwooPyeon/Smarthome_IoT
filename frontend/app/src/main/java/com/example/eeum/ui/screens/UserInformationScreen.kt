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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun UserInformationScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 60.dp)
                .verticalScroll(rememberScrollState())
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
                    text = "사용자 정보",
                    color = Gray900,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold))
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.height(24.dp))

            // 프로필 사진 + 편집 아이콘
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = Color.White) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_user_photo),
                        contentDescription = "프로필 사진",
                        modifier = Modifier.size(120.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                // 카메라(사진 변경) 플로팅 버튼
                Surface(
                    shape = CircleShape,
                    color = Blue500,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 90.dp, bottom = 4.dp) // 우하단 배치
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = "사진 변경",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 정보 카드
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                    ReadonlyField(label = "이름", value = "윤경진")
                    Spacer(Modifier.height(24.dp))
NicknameField(value = "캐리좀", onClickChange = { _ /* newNickname */ -> /* TODO: 닉네임 변경 저장 */ })
                    Spacer(Modifier.height(24.dp))
                    ReadonlyField(label = "이메일", value = "email@email.com")
                    Spacer(Modifier.height(24.dp))
                    ReadonlyField(label = "AI 비서 이름", value = "제니")
                }
            }

            Spacer(Modifier.height(40.dp))

            // 주요 버튼들
            PrimaryWideButton(text = "비밀번호 변경", onClick = { /* TODO */ })
            Spacer(Modifier.height(24.dp))
            SecondaryWideButton(text = "로그아웃", onClick = { /* TODO */ })

            Spacer(Modifier.height(140.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "회원 탈퇴",
                    color = Gray600,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium))
                    )
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
        }
    }

@Preview
@Composable
private fun UserInformationScreenPreview() {
    EeumTheme(dynamicColor = false) {
        UserInformationScreen()
    }
}

@Composable
private fun ReadonlyField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Gray900,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.goormsansbold))
            )
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            color = Color(0xFFF9FAFB),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                color = Gray900,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun NicknameField(value: String, onClickChange: (String) -> Unit) {
    // 편집/완료 상태
    var editing by remember { mutableStateOf(false) }
    var draft by remember(value) { mutableStateOf(value) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "닉네임",
            color = Gray900,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.goormsansbold))
            )
        )
        Spacer(Modifier.height(8.dp))

        // 한 줄에 보이되, 버튼은 Row 밖(오버레이) 배치
        Box(modifier = Modifier.fillMaxWidth()) {
            // 입력 영역 카드
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (editing) Blue500 else Gray50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editing) {
                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                                color = Gray900
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 60.dp)
                        )
                    } else {
                        Text(
                            text = value,
                            color = Gray900,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.goormsansmedium))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 60.dp)
                        )
                    }
                }
            }

            // 변경/완료 버튼 (Row 외부, 같은 라인 우측에 배치)
            Surface(
                color = Blue500,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .width(40.dp)
                    .height(36.dp)
                    .clickable {
                        val wasEditing = editing
                        editing = !editing
                        if (wasEditing) {
                            // 완료 버튼 클릭: 변경 사항 전달
                            onClickChange(draft)
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (editing) "완료" else "변경",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.goormsansmedium))
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryWideButton(text: String, onClick: () -> Unit) {
    Surface(
        color = Blue500,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                )
            )
        }
    }
}

@Composable
private fun SecondaryWideButton(text: String, onClick: () -> Unit) {
    Surface(
        color = Gray50,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Gray900,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                )
            )
        }
    }
}
