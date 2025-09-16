package com.example.eeum.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF007AFF),
    textColor: Color = Color.White,
    width: Dp = 330.dp,
    height: Dp = 50.dp,
    icon: Painter? = null, // 아이콘을 위한 Painter 파라미터 추가
    iconTint: Color = Color.Unspecified // 아이콘 틴트 색상 파라미터 추가
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 2.dp, spotColor = Color(0x1A000000), ambientColor = Color(0x1A000000))
            .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(size = 12.dp))
            .background(color = backgroundColor, shape = RoundedCornerShape(size = 12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.let {
                Image(
                    painter = it,
                    contentDescription = null, // contentDescription은 필요에 따라 설정
                    modifier = Modifier.size(24.dp), // 아이콘 크기 예시
                    colorFilter = if (iconTint != Color.Unspecified) androidx.compose.ui.graphics.ColorFilter.tint(iconTint) else null
                )
                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp)) // 아이콘과 텍스트 사이 간격
            }
            Text(
                text = text,
                color = textColor,
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 16.sp), fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        }
    }
}
