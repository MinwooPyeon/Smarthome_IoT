package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R
import com.example.eeum.ui.theme.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
    // New implementation matching 메뉴.png
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(colors = listOf(Color(0xFFB4E3FD), Color(0xFFCCFCFF))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 40.dp)
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
                modifier = Modifier.size(20.dp)
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
                    .size(80.dp)
            )

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

        Spacer(Modifier.height(12.dp))

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
                fontSize = 18.sp,
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
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceItem(
                    bg = Purple100, iconTint = Purple600,
                    icon = R.drawable.ic_search, label = "찾기"
                )
                ServiceItem(
                    bg = Yellow200, iconTint = Amber600,
                    icon = R.drawable.ic_routine, label = "내 루틴"
                )
                ServiceItem(
                    bg = SurfaceAlt, iconTint = Color(0xFF14AE5C),
                    icon = R.drawable.ic_log_management, label = "로그 기록"
                )
                ServiceItem(
                    bg = SurfaceAlt, iconTint = Blue600,
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
                fontSize = 18.sp,
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
                MenuListItem(icon = R.drawable.ic_setting, tint = Gray500, text = "설정")
                androidx.compose.material3.Divider(color = Gray50)
                MenuListItem(icon = R.drawable.ic_hub_management, tint = Color(0xFF14AE5C), text = "허브 관리")
            }
        }
        }
    }

    // Keep legacy code unreachable to avoid large refactor now
    return

    Surface(
        color = SurfaceBase,
        border = BorderStroke(0.dp, Gray50),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .requiredWidth(width = 385.dp)
                .requiredHeight(height = 969.dp)
        ) {
            Text(
                text = "우리 집",
                color = Gray800,
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 24.dp,
                        y = 58.dp)
                    .requiredWidth(width = 185.dp)
                    .requiredHeight(height = 24.dp))
            Surface(
                color = Color.Black,
                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 332.dp,
                        y = 60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(size = 20.dp)
                ) {
                    Surface(
                        color = Color.Black,
                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 2.dp,
                                y = (-4).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 16.dp)
                                .requiredHeight(height = 28.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 0.dp,
                                        y = 6.dp)
                                    .requiredWidth(width = 16.dp)
                                    .requiredHeight(height = 18.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_setting),
                                    contentDescription = "Setting",
                                    colorFilter = ColorFilter.tint(Gray600),
                                    modifier = Modifier
                                        .requiredWidth(width = 16.dp)
                                        .requiredHeight(height = 18.dp)
                                        .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                            }
                        }
                    }
                }
            }
            Surface(
                color = Color.Black,
                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 1.dp,
                        y = 118.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(width = 358.dp)
                        .requiredHeight(height = 64.dp)
                ) {
                    Surface(
                        color = Color.Black,
                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 17.dp,
                                y = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 155.dp)
                                .requiredHeight(height = 64.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_user_photo),
                                contentDescription = "User Photo",
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 1.dp,
                                        y = 4.dp)
                                    .requiredSize(size = 64.dp)
                                    .clip(shape = RoundedCornerShape(9999.dp))
                                    .border(border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        shape = RoundedCornerShape(9999.dp)))
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 80.dp,
                                        y = 18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 75.dp)
                                        .requiredHeight(height = 28.dp)
                                ) {
                                    Text(
                                        text = "태훈태훈",
                                        color = Color(0xff1f2937),
                                        style = TextStyle(
                                            fontSize = 18.sp,
                                            fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = (-0.33).dp,
                                                y = 4.dp)
                                            .requiredWidth(width = 128.dp)
                                            .requiredHeight(height = 28.dp))
                                }
                            }
                        }
                    }
                    Surface(
                        color = Color.Black,
                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 336.dp,
                                y = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 10.dp)
                                .requiredHeight(height = 16.dp)
                        ) {
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 1.dp,
                                        y = 0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 8.dp)
                                        .requiredHeight(height = 16.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 0.dp,
                                                y = 3.dp)
                                            .requiredWidth(width = 8.dp)
                                            .requiredHeight(height = 12.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_page_move),
                                            contentDescription = "Page Move",
                                            colorFilter = ColorFilter.tint(Gray300),
                                            modifier = Modifier
                                                .requiredWidth(width = 8.dp)
                                                .requiredHeight(height = 12.dp)
                                                .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xffeff6ff),
                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 19.dp,
                                y = 80.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 339.dp)
                                .requiredHeight(height = 50.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = Color(0xff2563eb),
                                border = BorderStroke(1.dp, Color(0xff1d4ed8)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 274.dp,
                                        y = 12.dp)
                                    .clip(shape = RoundedCornerShape(9999.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 50.dp)
                                        .requiredHeight(height = 26.dp)
                                ) {
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 14.dp,
                                                y = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 28.dp)
                                                .requiredHeight(height = 16.dp)
                                        ) {
                                            Text(
                                                text = "가입",
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = (-3.02).dp,
                                                        y = 2.dp)
                                                    .requiredWidth(width = 28.dp))
                                        }
                                    }
                                }
                            }
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 12.dp,
                                        y = 15.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 215.dp)
                                        .requiredHeight(height = 20.dp)
                                ) {
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 11.dp,
                                                y = 0.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 84.dp)
                                                .requiredHeight(height = 20.dp)
                                        ) {
                                            Text(
                                                text = "멤버십 없음",
                                                color = Color(0xff1d4ed8),
                                                style = TextStyle(
                                                    fontSize = 14.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 0.05.dp,
                                                        y = 2.dp)
                                                    .requiredWidth(width = 107.dp)
                                                    .requiredHeight(height = 20.dp))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 106.83.dp,
                                                y = 0.dp)
                                            .requiredWidth(width = 10.dp)
                                            .requiredHeight(height = 20.dp)
                                            .background(color = Color.Black)
                                            .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                    Box(
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 172.88.dp,
                                                y = 0.dp)
                                            .requiredWidth(width = 58.dp)
                                            .requiredHeight(height = 20.dp)
                                            .background(color = Color.Black)
                                            .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                }
                            }
                        }
                    }
                }
            }
            Surface(
                color = Color.Black,
                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 16.dp,
                        y = 270.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(width = 128.dp)
                        .requiredHeight(height = 20.dp)
                ) {
                    Text(
                        text = "서비스",
                        color = Color(0xff4b5563),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 0.dp,
                                y = 2.dp)
                            .requiredWidth(width = 59.dp)
                            .requiredHeight(height = 20.dp))
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 14.dp,
                        y = 306.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .shadow(elevation = 2.dp,
                        shape = RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(width = 348.dp)
                        .requiredHeight(height = 122.dp)
                ) {
                    Surface(
                        color = Color.Black,
                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 25.dp,
                                y = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 320.dp)
                                .requiredHeight(height = 106.dp)
                        ) {
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = (-1).dp,
                                        y = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 68.dp)
                                        .requiredHeight(height = 80.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = Color(0xfff3e8ff),
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = (-4).dp,
                                                y = 0.dp)
                                            .clip(shape = RoundedCornerShape(9999.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(size = 56.dp)
                                        ) {
                                            Surface(
                                                color = Color.Black,
                                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 19.dp,
                                                        y = 14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .requiredWidth(width = 18.dp)
                                                        .requiredHeight(height = 28.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .align(alignment = Alignment.TopStart)
                                                            .offset(x = 0.dp,
                                                                y = 6.25.dp)
                                                            .requiredSize(size = 18.dp)
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_search),
                                                            contentDescription = "Search",
                                                            colorFilter = ColorFilter.tint(Purple600),
                                                            modifier = Modifier
                                                                .requiredSize(size = 18.dp)
                                                                .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = (-2.5).dp,
                                                y = 64.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 51.dp)
                                                .requiredHeight(height = 16.dp)
                                        ) {
                                            Text(
                                                text = "찾기",
                                                color = Color(0xff4b5563),
                                                textAlign = TextAlign.Center,
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 2.16.dp,
                                                        y = 1.dp)
                                                    .requiredWidth(width = 50.dp)
                                                    .requiredHeight(height = 16.dp))
                                        }
                                    }
                                }
                            }
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 83.dp,
                                        y = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 68.dp)
                                        .requiredHeight(height = 80.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = Color(0xfffef9c3),
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = (-4).dp,
                                                y = 0.dp)
                                            .clip(shape = RoundedCornerShape(9999.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(size = 56.dp)
                                        ) {
                                            Surface(
                                                color = Color.Black,
                                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 20.13.dp,
                                                        y = 14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .requiredWidth(width = 16.dp)
                                                        .requiredHeight(height = 28.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .align(alignment = Alignment.TopStart)
                                                            .offset(x = 0.dp,
                                                                y = 6.25.dp)
                                                            .requiredWidth(width = 16.dp)
                                                            .requiredHeight(height = 18.dp)
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_routine),
                                                            contentDescription = "Routine",
                                                            colorFilter = ColorFilter.tint(Amber600),
                                                            modifier = Modifier
                                                                .requiredWidth(width = 16.dp)
                                                                .requiredHeight(height = 18.dp)
                                                                .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 3.5.dp,
                                                y = 64.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 41.dp)
                                                .requiredHeight(height = 16.dp)
                                        ) {
                                            Text(
                                                text = "내 루틴",
                                                color = Color(0xff4b5563),
                                                textAlign = TextAlign.Center,
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 1.48.dp,
                                                        y = 1.dp)
                                                    .requiredWidth(width = 40.dp)
                                                    .requiredHeight(height = 16.dp))
                                        }
                                    }
                                }
                            }
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 248.dp,
                                        y = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 68.dp)
                                        .requiredHeight(height = 80.dp)
                                ) {
                                    FloatingActionButton(
                                        onClick = { },
                                        containerColor = Color(0xffc3dcfe)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(size = 56.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_membership),
                                                contentDescription = "Membership",
                                                colorFilter = ColorFilter.tint(Blue600),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 16.dp,
                                                        y = 16.dp)
                                                    .requiredSize(size = 24.dp))
                                        }
                                    }
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = (-1).dp,
                                                y = 64.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 50.dp)
                                                .requiredHeight(height = 16.dp)
                                        ) {
                                            Text(
                                                text = "멤버쉭",
                                                color = Color(0xff4b5563),
                                                textAlign = TextAlign.Center,
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 1.5.dp,
                                                        y = 1.dp)
                                                    .requiredWidth(width = 49.dp)
                                                    .requiredHeight(height = 16.dp))
                                        }
                                    }
                                }
                            }
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 167.dp,
                                        y = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 68.dp)
                                        .requiredHeight(height = 80.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = Color(0xffdefec3),
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = (-4).dp,
                                                y = 0.dp)
                                            .clip(shape = RoundedCornerShape(9999.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(size = 56.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 20.13.dp,
                                                        y = 14.dp)
                                                    .requiredWidth(width = 16.dp)
                                                    .requiredHeight(height = 28.dp)
                                                    .background(color = Color.Black)
                                                    .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_log_management),
                                                contentDescription = "Log Management",
                                                colorFilter = ColorFilter.tint(Color(0xff14ae5c)),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 18.dp,
                                                        y = 18.dp)
                                                    .requiredSize(size = 20.dp))
                                        }
                                    }
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 3.dp,
                                                y = 64.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 53.dp)
                                                .requiredHeight(height = 16.dp)
                                        ) {
                                            Text(
                                                text = "로그 기록",
                                                color = Color(0xff4b5563),
                                                textAlign = TextAlign.Center,
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = (-5).dp,
                                                        y = 1.dp)
                                                    .requiredWidth(width = 52.dp)
                                                    .requiredHeight(height = 16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Surface(
                color = Color.Black,
                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 14.dp,
                        y = 464.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(width = 128.dp)
                        .requiredHeight(height = 20.dp)
                ) {
                    Text(
                        text = "추가 기능\n",
                        color = Color(0xff4b5563),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 0.dp,
                                y = 2.dp)
                            .requiredWidth(width = 119.dp)
                            .requiredHeight(height = 20.dp))
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 19.dp,
                        y = 505.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .shadow(elevation = 2.dp,
                        shape = RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(width = 342.dp)
                        .requiredHeight(height = 251.dp)
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 126.dp)
                                                .requiredHeight(height = 24.dp)
                                        ) {
                                            Text(
                                                text = "공지사항",
                                                color = Color(0xff1f2937),
                                                style = TextStyle(
                                                    fontSize = 16.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 0.dp,
                                                        y = 8.dp)
                                                    .requiredWidth(width = 121.dp)
                                                    .requiredHeight(height = 24.dp))
                                        }
                                    }
                                },
                                navigationIcon = {
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 18.dp)
                                                .requiredHeight(height = 28.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 0.dp,
                                                        y = 6.dp)
                                                    .requiredSize(size = 18.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_announcement),
                                                    contentDescription = "Announcement",
                                                    colorFilter = ColorFilter.tint(Orange500),
                                                    modifier = Modifier
                                                        .requiredSize(size = 18.dp)
                                                        .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 0.dp,
                                        y = 3.dp)
                                    .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .requiredWidth(width = 342.dp)
                                .requiredHeight(height = 62.dp)
                                .background(color = Color.Black)
                        ) {
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 16.dp,
                                        y = 18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 18.dp)
                                        .requiredHeight(height = 28.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 0.dp,
                                                y = 4.dp)
                                            .requiredSize(size = 18.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_user_manual),
                                            contentDescription = "User Manual",
                                            colorFilter = ColorFilter.tint(Blue500),
                                            modifier = Modifier
                                                .requiredSize(size = 18.dp)
                                                .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                    }
                                }
                            }
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 50.dp,
                                        y = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 86.dp)
                                        .requiredHeight(height = 24.dp)
                                ) {
                                    Text(
                                        text = "사용 설명서",
                                        color = Color(0xff1f2937),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 0.dp,
                                                y = 3.dp)
                                            .requiredWidth(width = 110.dp)
                                            .requiredHeight(height = 24.dp))
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 342.dp)
                                .requiredHeight(height = 62.dp)
                                .background(color = Color.Black)
                        ) {
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 16.dp,
                                        y = 18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 18.dp)
                                        .requiredHeight(height = 28.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 0.dp,
                                                y = 4.dp)
                                            .requiredSize(size = 18.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_setting),
                                            contentDescription = "Setting",
                                            colorFilter = ColorFilter.tint(Gray500),
                                            modifier = Modifier
                                                .requiredSize(size = 18.dp)
                                                .border(border = BorderStroke(0.dp, Color(0xffe5e7eb))))
                                    }
                                }
                            }
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 50.dp,
                                        y = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 148.dp)
                                        .requiredHeight(height = 24.dp)
                                ) {
                                    Text(
                                        text = "설정",
                                        color = Color(0xff1f2937),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 0.dp,
                                                y = 3.dp)
                                            .requiredWidth(width = 142.dp)
                                            .requiredHeight(height = 24.dp))
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .requiredWidth(width = 342.dp)
                                .requiredHeight(height = 62.dp)
                                .background(color = Color.Black)
                        ) {
                            Surface(
                                color = Color.Black,
                                border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                modifier = Modifier
                                    .align(alignment = Alignment.TopStart)
                                    .offset(x = 14.dp,
                                        y = 18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(width = 114.dp)
                                        .requiredHeight(height = 28.dp)
                                ) {
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 3.dp,
                                                y = (-2).dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(size = 20.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_hub_management),
                                                contentDescription = "Hub Management",
                                                colorFilter = ColorFilter.tint(Color(0xff14ae5c)),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 0.dp,
                                                        y = 5.dp)
                                                    .requiredSize(size = 20.dp))
                                        }
                                    }
                                    Surface(
                                        color = Color.Black,
                                        border = BorderStroke(0.dp, Color(0xffe5e7eb)),
                                        modifier = Modifier
                                            .align(alignment = Alignment.TopStart)
                                            .offset(x = 36.66.dp,
                                                y = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .requiredWidth(width = 75.dp)
                                                .requiredHeight(height = 24.dp)
                                        ) {
                                            Text(
                                                text = "허브 관리",
                                                color = Color(0xff1f2937),
                                                style = TextStyle(
                                                    fontSize = 16.sp,
                                                    fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                                                modifier = Modifier
                                                    .align(alignment = Alignment.TopStart)
                                                    .offset(x = 0.dp,
                                                        y = 3.dp)
                                                    .requiredWidth(width = 75.dp)
                                                    .requiredHeight(height = 24.dp))
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        )
    }
}
