package com.example.eeum.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.theme.Blue600
import com.example.eeum.ui.theme.Gray800

/**
 * 허브가 DB에 없을 때 띄우는 확인 다이얼로그.
 * - 확인을 누르면 DeviceRegistrationScreen으로 돌아갑니다.
 */
@Composable
fun CheckHubDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = onDismiss,
    navController: NavController? = null
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        colorFilter = null, // png 본래 색상 사용
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "허브를 설정해야 합니다",
                        color = Gray800,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(R.font.goormsansmedium))
                        )
                    )
                }

                Spacer(modifier = Modifier.padding(top = 24.dp))

                // 공통 버튼 적용 (MainButton.kt의 CustomButton)
                CustomButton(
                    text = "확인",
                    onClick = {
                        // DeviceRegistrationScreen으로 돌아가기
                        navController?.popBackStack()
                        onConfirm()
                    },
                    backgroundColor = Blue600,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CheckFloorPlanPreview() {
    val (visible, setVisible) = remember { mutableStateOf(true) }
    Box {
        // 배경 (프리뷰용)
        Surface(color = Color(0xFFDBF2FD)) { Box(modifier = Modifier.fillMaxWidth()) }
        CheckHubDialog(
            visible = visible,
            onDismiss = { setVisible(false) }
        )
    }
}
