package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.components.CustomButton
import com.example.eeum.ui.theme.*
import com.example.eeum.ui.components.QRScannerView

@Composable
fun DeviceRegistrationQRScreen(
    navController: NavController? = null,
    kind: String? = null,
    onManualInput: () -> Unit = {}
) {
    val isScanningState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scannedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var isScanning: Boolean by isScanningState
    var scanned: String? by scannedState
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
    ) {
        // 헤더: 뒤로가기 + 중앙 타이틀 (다른 화면과 동일)
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_page_move_left),
                contentDescription = "뒤로가기",
                colorFilter = ColorFilter.tint(Gray800),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .background(Color.Transparent)
                    .padding(0.dp)
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

        Spacer(Modifier.height(24.dp))

        // 안내 카드
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "디바이스의 QR코드를 스캔하세요",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold)),
                        color = Gray800
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "제품 뒷면에 있는 QR코드를 카메라로 스캔해주세요.",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray600
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 스캔 영역 카드
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR 프레임 플레이스홀더 (모서리에 색상 코너 표시) + 클릭 시 스캔 시작
                // 바깥(외부)에 코너를 배치하기 위해, 배경 박스와 코너들을 분리
                Box(modifier = Modifier.size(280.dp)) {
                    // 배경 + 클릭 영역 (내부)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                            .clickable { isScanning = true }
                    )

                    // 네 모서리 코너 라인 (외부 배치: 음수/양수 offset으로 바깥으로 조금 나가게)
                    val cornerSize: Dp = 32.dp
                    val stroke: Dp = 5.dp
                    val out = stroke / 2

                    CornerL(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = -out, y = -out),
                        cornerSize = cornerSize,
                        stroke = stroke,
                        color = Gray600,
                        rotation = 0f
                    )
                    CornerL(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = out, y = -out),
                        cornerSize = cornerSize,
                        stroke = stroke,
                        color = Gray600,
                        rotation = 90f
                    )
                    CornerL(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = out, y = out),
                        cornerSize = cornerSize,
                        stroke = stroke,
                        color = Gray600,
                        rotation = 180f
                    )
                    CornerL(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = -out, y = out),
                        cornerSize = cornerSize,
                        stroke = stroke,
                        color = Gray600,
                        rotation = 270f
                    )

                    if (isScanning) {
                        // 카메라 미리보기는 반드시 280dp 박스 안에서만 보이도록 클리핑
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            QRScannerView(
                                modifier = Modifier.fillMaxSize(),
                            ) { code ->
                                scanned = code
                                isScanning = false
                                // 스캔 성공 시 브랜드 입력 화면으로 이동
                                val k = kind ?: ""
                                navController?.navigate("device_registration_brand/$k?serial=$code") { launchSingleTop = true }
                            }
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_qr),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Gray400),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "QR코드를 화면에 맞춰주세요",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray500
                    )
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "디바이스에 있는 QR코드를 카메라로 인식시켜 주세요",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray600
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 또는
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = "또는",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                    color = Gray600
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // 직접 입력하기 버튼
        CustomButton(
            text = "직접 입력하기",
            onClick = onManualInput,
            backgroundColor = Color.White,
            textColor = Gray800,
            width = 350.dp,
            height = 48.dp,
            icon = painterResource(id = R.drawable.ic_keyboard),
            iconTint = Gray600,
            modifier = Modifier.fillMaxWidth()
        )
        // 스캐너 오버레이 (클릭 시 표시)
    }
}

// 외부에 정의된 코너 직각 컴포저블
@Composable
private fun CornerL(
    modifier: Modifier,
    cornerSize: Dp,
    stroke: Dp,
    color: Color,
    rotation: Float
) {
    Canvas(modifier.size(cornerSize)) {
        val len = size.minDimension
        val path = Path().apply {
            moveTo(0f, len)
            lineTo(0f, 0f)
            lineTo(len, 0f)
        }
        rotate(rotation) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Preview
@Composable
private fun DeviceRegistrationQRPreview() {
    com.example.eeum.ui.theme.EeumTheme(dynamicColor = false) {
        DeviceRegistrationQRScreen()
    }
}
