package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.theme.*

@Composable
fun DeviceRegistrationBrandScreen(
    navController: NavController? = null,
    onNext: (String) -> Unit = {}
) {
    var brand by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    val brandTrim = brand.trim()
    val modelTrim = model.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
    ) {
        // 헤더: 뒤로가기 + 중앙 타이틀 (다른 화면과 동일 패턴)
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
                text = "디바이스 등록",
                color = Gray900,
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(60.dp))

        // 입력 카드
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "디바이스 정보 입력",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold)),
                        color = Gray800
                    )
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "브랜드",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray600
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    placeholder = { Text("예: 삼성", fontSize = 14.sp, color = Gray400) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "모델명",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray600
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    placeholder = { Text("예: Q9000", fontSize = 14.sp, color = Gray400) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

            }
        }

        Spacer(Modifier.weight(1f))

        // 다음 버튼 (파란 배경)
        Button(
            onClick = { onNext("$brandTrim|$modelTrim") },
            enabled = brandTrim.isNotEmpty() && modelTrim.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "다음",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun DeviceRegistrationBrandPreview() {
    EeumTheme(dynamicColor = false) {
        DeviceRegistrationBrandScreen()
    }
}