package com.example.eeum.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.eeum.R
import com.example.eeum.ui.theme.*

@Composable
fun DeviceDeleteDialog(
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "디바이스 삭제",
                        color = Gray900,
                        style = TextStyle(fontSize = 18.sp, fontFamily = FontFamily(Font(R.font.goormsansbold)))
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "해당 디바이스를 삭제하시겠습니까?",
                    color = Gray600,
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(40.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gray50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onCancel() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "취소",
                                color = Gray600,
                                style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)))
                            )
                        }
                    }
                    
                    // Confirm button
                    Surface(
                        color = Blue500,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onConfirm("") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "확인",
                                color = Color.White,
                                style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.goormsansbold)))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DeviceDeleteDialogPreview() {
    androidx.compose.material3.Surface(modifier = Modifier.background(Color(0x33000000))) {
        DeviceDeleteDialog(onConfirm = {}, onCancel = {})
    }
}
