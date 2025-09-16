package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.theme.*

@Composable
fun DeviceRegistrationCompleteScreen(
    navController: NavController? = null,
    kind: String? = null,
    onRegister: () -> Unit = {}
) {
    val iconRes = iconResFor(kind)

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

        Text(
            text = "평면도",
            style = TextStyle(
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.goormsansbold)),
                color = Gray800
            )
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // 부모 박스 크기 측정 + 드래그 가능한 아이콘
            val density = LocalDensity.current
            val iconOuterSize = 20.dp
            val iconOuterPx = with(density) { iconOuterSize.toPx() }
            var parentSize = remember { mutableStateOf(IntSize.Zero) }
            var iconOffset = remember { mutableStateOf(Offset(Float.NaN, Float.NaN)) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        parentSize.value = size
                        if (iconOffset.value.x.isNaN()) {
                            // 최초에는 중앙 배치
                            val cx = (size.width - iconOuterPx) / 2f
                            val cy = (size.height - iconOuterPx) / 2f
                            iconOffset.value = Offset(cx, cy)
                        }
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.examplefloor),
                    contentDescription = "평면도",
                    modifier = Modifier.fillMaxSize()
                )
                if (iconRes != null && !iconOffset.value.x.isNaN()) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(iconOuterSize)
                            .offset { IntOffset(iconOffset.value.x.toInt(), iconOffset.value.y.toInt()) }
                            .pointerInput(parentSize.value, iconOuterPx) {
                                detectDragGestures { _, drag ->
                                    val maxX = (parentSize.value.width - iconOuterPx).coerceAtLeast(0f)
                                    val maxY = (parentSize.value.height - iconOuterPx).coerceAtLeast(0f)
                                    val nx = (iconOffset.value.x + drag.x).coerceIn(0f, maxX)
                                    val ny = (iconOffset.value.y + drag.y).coerceIn(0f, maxY)
                                    iconOffset.value = Offset(nx, ny)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onRegister() },
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
                text = "등록",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                )
            )
        }
    }
}

private fun iconResFor(kind: String?): Int? = when (kind) {
    "AIR_CONDITIONER" -> R.drawable.ic_icon_air_conditioning
    "FAN" -> R.drawable.ic_icon_electric_fan
    "TV" -> R.drawable.ic_icon_television
    "BEAM_PROJECTOR" -> R.drawable.ic_icon_beam_projector
    "AIR_PURIFIER" -> R.drawable.ic_icon_air_purifier
    "LIGHT" -> R.drawable.ic_icon_light
    "HUB" -> R.drawable.ic_icon_hub
    else -> null
}

@Preview
@Composable
private fun DeviceRegistrationCompletePreview() {
    EeumTheme(dynamicColor = false) {
        DeviceRegistrationCompleteScreen(kind = "AIR_CONDITIONER")
    }
}
