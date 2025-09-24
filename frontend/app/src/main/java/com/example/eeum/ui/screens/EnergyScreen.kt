package com.example.eeum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.eeum.ui.theme.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
// Vico (Compose)
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlin.math.roundToInt
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
// YCharts 라이브러리
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData

@Composable
fun EnergyScreen(
    modifier: Modifier = Modifier,
    energyViewModel: EnergyViewModel = viewModel(),
    deviceViewModel: EnergyDeviceViewModel = viewModel()
) {
    // 각 섹션의 독립적인 상태 관리
    var energyUsageSelectedPeriod by remember { mutableStateOf("일간") }
    var deviceReportSelectedPeriod by remember { mutableStateOf("일간") }
    
    // 전체 에너지 ViewModel 데이터 관찰
    val energySeries by energyViewModel.energySeries.observeAsState(emptyList())
    val totalKwh by energyViewModel.totalKwh.observeAsState(0.0)
    val isLoading by energyViewModel.isLoading.observeAsState(false)
    val error by energyViewModel.error.observeAsState()
    
    // 디바이스별 ViewModel 데이터 관찰
    val deviceItems by deviceViewModel.deviceItems.observeAsState(emptyList())
    val deviceTotalKwh by deviceViewModel.totalKwh.observeAsState(0.0)
    val deviceIsLoading by deviceViewModel.isLoading.observeAsState(false)
    val deviceError by deviceViewModel.error.observeAsState()
    
    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        energyViewModel.fetchEnergyUsageByPeriod("일간")
        deviceViewModel.fetchDeviceEnergyUsageByPeriod("일간")
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 60.dp, end = 16.dp)
    ) {
        // 상단 타이틀
        Text(
            text = "사용량",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 스크롤 가능한 컨텐츠
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 전체 에너지 사용량 섹션
                EnergyUsageSection(
                    selectedPeriod = energyUsageSelectedPeriod,
                    onPeriodChange = { period ->
                        energyUsageSelectedPeriod = period
                        energyViewModel.fetchEnergyUsageByPeriod(period)
                    },
                    energyData = energySeries,
                    totalKwh = totalKwh,
                    isLoading = isLoading,
                    error = error
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                // 제품별 사용 리포트 섹션
                DeviceReportSection(
                    selectedPeriod = deviceReportSelectedPeriod,
                    onPeriodChange = { period ->
                        deviceReportSelectedPeriod = period
                        deviceViewModel.fetchDeviceEnergyUsageByPeriod(period)
                    },
                    deviceItems = deviceItems,
                    totalKwh = deviceTotalKwh,
                    isLoading = deviceIsLoading,
                    error = deviceError
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                // AI 리포트 섹션
                AIReportSection()
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// 전체 에너지 사용량 섹션
@Composable
private fun EnergyUsageSection(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    energyData: List<com.example.eeum.data.model.response.device.EnergyTotalUsageDataList>,
    totalKwh: Double,
    isLoading: Boolean,
    error: String?
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "전체 에너지 사용량",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (totalKwh > 0) {
                Text(
                    text = "${totalKwh}kWh",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // 기간 선택 탭
        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 차트 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)  // 차트 높이 증가
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // 로딩 상태 및 에러 처리
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "로딩 중...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "데이터 로드 실패: $error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    energyData.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "데이터가 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    else -> {
                        // 기간에 따라 다른 차트 타입 사용
                        if (selectedPeriod == "일간") {
                            // 일간 - 3시간씩 집계 후 막대그래프 (가로 스크롤 & 동적 폭)
                            val threeHourData = groupHourlyDataToThreeHours(energyData)
                            val scrollState = rememberScrollState()
                            val chartWidth = (threeHourData.size * 50).dp
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(scrollState)
                            ) {
                                EnergyBarChart(
                                    data = threeHourData,
                                    modifier = Modifier
                                        .width(chartWidth)
                                        .fillMaxHeight(),
                                    period = selectedPeriod
                                )
                            }
                        } else if (selectedPeriod == "주간") {
                            // 주간 - 막대그래프
                            EnergyBarChart(
                                data = energyData.map { EnergyData(it.label, it.kwh.toFloat()) },
                                modifier = Modifier.fillMaxSize(),
                                period = selectedPeriod
                            )
                        } else if (selectedPeriod == "월간") {
                            // 월간 - 주차별 집계 후 라인차트
                            val weeklyData = groupDailyDataToWeekly(energyData)
            // 월간 레이블이 모두 보이도록 가로 스크롤과 넓은 폭 적용
                            val scrollState = rememberScrollState()
                            val chartWidth = (weeklyData.size * 80).dp
                            Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                                EnergyLineChart(
                                    data = weeklyData,
                                    modifier = Modifier
                                        .width(chartWidth)
                                        .fillMaxHeight(),
                                    period = selectedPeriod
                                )
                            }
                        } else {
                            // 라인차트 (연간)
                            EnergyLineChart(
                                data = energyData.map { EnergyData(it.label, it.kwh.toFloat()) },
                                modifier = Modifier.fillMaxSize(),
                                period = selectedPeriod
                            )
                        }
                    }
                }
            }
        }
    }
}

// 기간 선택 탭
@Composable
private fun PeriodSelector(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit
) {
    val periods = listOf("일간", "주간", "월간", "연간")
    val selectedIndex = periods.indexOf(selectedPeriod)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            periods.forEachIndexed { index, period ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onPeriodChange(period) },
                    text = {
                        Text(
                            text = period,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedIndex == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                )
            }
        }
    }
}

// 제품별 사용 리포트 섹션
@Composable
private fun DeviceReportSection(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    deviceItems: List<com.example.eeum.data.model.response.device.EnergyDeviceUsageDataList>,
    totalKwh: Double,
    isLoading: Boolean,
    error: String?
) {
    Column {
        Text(
            text = "제품별 사용 리포트",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 기간 선택 탭
        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 제품별 리포트 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 파이차트 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 로딩 상태 및 에러 처리
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "로딩 중...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "데이터 로드 실패: $error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        deviceItems.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "데이터가 없습니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        else -> {
                            DeviceUsagePieChart(
                                data = deviceItems,
                                modifier = Modifier.size(250.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 제품별 사용량 목록
                DeviceUsageList(deviceItems)
            }
        }
    }
}

// 제품별 사용량 목록
@Composable
private fun DeviceUsageList(
    devices: List<com.example.eeum.data.model.response.device.EnergyDeviceUsageDataList>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        devices.forEachIndexed { index, device ->
            DeviceUsageItem(device, getDeviceColor(index))
        }
    }
}

// 개별 제품 사용량 아이템
@Composable
private fun DeviceUsageItem(
    device: com.example.eeum.data.model.response.device.EnergyDeviceUsageDataList,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 색상 인디케이터
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = device.deviceType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = "${device.kwh}kWh (${device.percentage.toInt()}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

// AI 리포트 섹션
@Composable
private fun AIReportSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI 아이콘 (간단한 원으로 대체)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 8.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "AI 리포트",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // AI 분석 결과
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    Text(
                        text = "전원을 끄는 것보다 절전모드가 도움이 될 수 있어요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// 시간별 에너지 사용량 데이터
private data class EnergyData(
    val time: String,
    val value: Float
)

// 일간 데이터를 3시간씩 묶어서 집계하는 함수
private fun groupHourlyDataToThreeHours(
    data: List<com.example.eeum.data.model.response.device.EnergyTotalUsageDataList>
): List<EnergyData> {
    if (data.isEmpty()) return emptyList()
    
    val threeHourData = mutableMapOf<String, Float>()
    
    data.forEach { item ->
        try {
            // 시간 형식 처리 ("0시", "1시", ... 또는 "00:00", "01:00", ...)
            val hour = when {
                item.label.contains("시") -> item.label.replace("시", "").toIntOrNull() ?: 0
                item.label.matches(Regex("\\d{1,2}:00")) -> item.label.split(":")[0].toIntOrNull() ?: 0
                else -> item.label.toIntOrNull() ?: 0
            }
            
            // 3시간 구간으로 그룹화 (0-2시 -> 3시까지, 3-5시 -> 6시까지, ...)
            val groupStartHour = (hour / 3) * 3
            val groupEndHour = groupStartHour + 3
            val groupKey = if (groupEndHour == 24) "24:00" else "${groupEndHour}:00"
            
            threeHourData[groupKey] = (threeHourData[groupKey] ?: 0f) + item.kwh.toFloat()
        } catch (e: Exception) {
            // 시간 파싱 실패 시 원본 데이터 그대로 사용
        }
    }
    
    // 8개 그룹을 순서대로 정렬
    val orderedGroups = listOf(
        "3:00", "6:00", "9:00", "12:00", 
        "15:00", "18:00", "21:00", "24:00"
    )
    
    return orderedGroups.map { group ->
        EnergyData(group, threeHourData[group] ?: 0f)
    }
}

// 월간 데이터를 주차별로 집계하는 함수 (yyyy-MM-dd, MM-dd 지원)
private fun groupDailyDataToWeekly(
    data: List<com.example.eeum.data.model.response.device.EnergyTotalUsageDataList>
): List<EnergyData> {
    if (data.isEmpty()) return emptyList()

    val yyyyMmDd = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")
    val mmDd = Regex("^(\\d{2})-(\\d{2})$")

    // 월, 주차별 합계 (정렬을 위해 Pair(month, week)로 키 구성)
    val weeklyAgg = mutableMapOf<Pair<Int, Int>, Float>()

    data.forEach { item ->
        val label = item.label.trim()
        val (month, day) = when {
            yyyyMmDd.matches(label) -> {
                val m = yyyyMmDd.matchEntire(label)!!
                m.groupValues[2].toInt() to m.groupValues[3].toInt()
            }
            mmDd.matches(label) -> {
                val m = mmDd.matchEntire(label)!!
                m.groupValues[1].toInt() to m.groupValues[2].toInt()
            }
            else -> return@forEach // 지원하지 않는 포맷은 무시
        }
        val weekOfMonth = ((day - 1) / 7) + 1
        val key = month to weekOfMonth
        weeklyAgg[key] = (weeklyAgg[key] ?: 0f) + item.kwh.toFloat()
    }

    if (weeklyAgg.isEmpty()) return emptyList()

    // 월 → 주차 순으로 정렬하여 라벨 생성
    return weeklyAgg.entries
        .sortedWith(compareBy({ it.key.first }, { it.key.second }))
        .map { (mw, kwh) ->
            val (month, week) = mw
            EnergyData("${month}월${week}주차", kwh)
        }
}



// 파이차트 컴포넌트 (YCharts 라이브러리 사용)
@Composable
private fun DeviceUsagePieChart(
    data: List<com.example.eeum.data.model.response.device.EnergyDeviceUsageDataList>,
    modifier: Modifier = Modifier
) {
    var selectedSlice by remember { mutableStateOf<com.example.eeum.data.model.response.device.EnergyDeviceUsageDataList?>(null) }
    
    Box(modifier = modifier) {
        // API 데이터를 파이차트 데이터로 변환
        val pieChartData = PieChartData(
            slices = data.mapIndexed { index, device ->
                val color = getDeviceColor(index)
                PieChartData.Slice(
                    label = device.deviceType,
                    value = device.percentage.toFloat(),
                    color = color
                )
            },
            plotType = PlotType.Pie
        )
        
        val pieChartConfig = PieChartConfig(
            labelVisible = false,  // 라벨 숨김
            strokeWidth = 2f,
            labelColor = MaterialTheme.colorScheme.onSurface,
            activeSliceAlpha = 1.0f,  // 클릭 시 색상 변화 방지
            isAnimationEnable = true,
            showSliceLabels = false,  // 슬라이스 라벨 숨김
            isClickOnSliceEnabled = true  // 슬라이스 클릭 활성화
        )
        
        PieChart(
            modifier = Modifier.fillMaxSize(),
            pieChartData = pieChartData,
            pieChartConfig = pieChartConfig,
            onSliceClick = { slice ->
                // 선택된 슬라이스 업데이트 - 이미 선택된 경우 해제
                val clickedDevice = data.find { it.deviceType == slice.label }
                selectedSlice = if (selectedSlice?.deviceType == clickedDevice?.deviceType) {
                    null  // 이미 선택된 항목을 다시 클릭하면 선택 해제
                } else {
                    clickedDevice  // 새로운 항목 선택
                }
            }
        )
        
        // 선택된 디바이스 이름을 중앙에 표시
        selectedSlice?.let { device ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { selectedSlice = null }, // 터치하면 사라짐
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface  // 흰색 배경
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),  // 그림자 제거
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = getDeviceColor(data.indexOf(device))  // 선택된 디바이스의 색상으로 테두리 설정
                    )
                ) {
                    Text(
                        text = device.deviceType,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 디바이스 색상 가져오기 함수
private fun getDeviceColor(index: Int): androidx.compose.ui.graphics.Color {
    val colors = listOf(
        PastelBlue,
        PastelGreen,
        PastelOrange,
        PastelPurple,
        PastelRed,
        PastelYellow
    )
    return colors[index % colors.size]
}


// 에너지 사용량 막대 차트 (Vico)
@Composable
private fun EnergyBarChart(
    data: List<EnergyData>,
    modifier: Modifier = Modifier,
    period: String = ""
) {
    // 데이터가 비어 있으면 차트를 렌더링하지 않음
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "데이터가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    val modelProducer = remember { ChartEntryModelProducer() }
    var ready by remember { mutableStateOf(false) }

    // 데이터의 최댓값 계산 및 상한 추정 (Y축 라벨 표기 개선)
    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    val upperBound = if (maxValue <= 1f) 1f else kotlin.math.ceil(maxValue.toDouble()).toFloat()

    LaunchedEffect(data) {
        ready = false
        // 막대가 세로 구분선 위에 위치하도록 X축 값 조정
        val series = if (period == "일간") {
            // 일간의 경우 막대가 X축 라벨 위치에 정확히 위치하도록 설정
            // 0, 1, 2, 3, 4, 5, 6, 7 인덱스에 막대 배치
            listOf(data.mapIndexed { index, e -> entryOf(index.toFloat(), e.value) })
        } else {
            listOf(data.mapIndexed { index, e -> entryOf(index.toFloat(), e.value) })
        }
        modelProducer.setEntries(series)
        ready = true
    }

    if (!ready) {
        Box(modifier = modifier.fillMaxSize()) {}
        return
    }

    // 막대 클릭 및 값 표시를 위한 상태 변수
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    Box(modifier = modifier) {
        ProvideChartStyle(m3ChartStyle()) {
            Chart(
                chart = columnChart(),
                chartModelProducer = modelProducer,
                startAxis = rememberStartAxis(
                    guideline = null,  // Y축 격자선 제거
                    valueFormatter = { value, _ -> 
                        if (value < 0f) "" else if (maxValue <= 1f) {
                            String.format("%.1fkWh", value)
                        } else {
                            "${value.toInt()}kWh"
                        }
                    }
                ),
                bottomAxis = rememberBottomAxis(
                    guideline = null,  // X축 격자선 제거
                    valueFormatter = { value, _ ->
                        if (period == "일간") {
                            val index = value.toInt()
                            if (index >= 0 && index < data.size) {
                                data[index].time  // "3:00", "6:00" 형식
                            } else ""
                        } else {
                            val index = value.toInt()
                            if (index >= 0 && index < data.size) {
                                data[index].time
                            } else ""
                        }
                    }
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 차트 위에 클릭 감지 오버레이
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            data.forEachIndexed { idx, e ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)  // 차트 전체 높이에 맞게 클릭 영역 설정
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            selectedIndex = if (selectedIndex == idx) null else idx
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (selectedIndex == idx) {
                        Text(
                            text = String.format("%.2f", e.value),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// 에너지 사용량 라인 차트 (Vico)
@Composable
private fun EnergyLineChart(
    data: List<EnergyData>,
    modifier: Modifier = Modifier,
    period: String = ""
) {
    // 데이터가 비어 있으면 차트를 렌더링하지 않음
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "데이터가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    val modelProducer = remember { ChartEntryModelProducer() }
    var ready by remember { mutableStateOf(false) }
    
    // 데이터의 최댓값 계산 및 상한 추정 (Y축 라벨 표기 개선)
    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    val upperBound = if (maxValue <= 1f) 1f else kotlin.math.ceil(maxValue.toDouble()).toFloat()

    LaunchedEffect(data) {
        ready = false
        val series = listOf(data.mapIndexed { index, e -> entryOf(index.toFloat(), e.value) })
        modelProducer.setEntries(series)
        ready = true
    }

    if (!ready) {
        Box(modifier = modifier.fillMaxSize()) {}
        return
    }

    ProvideChartStyle(m3ChartStyle()) {
        Chart(
            chart = lineChart(),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(
                guideline = null,  // Y축 격자선 제거
                valueFormatter = { value, _ ->
                    if (value < 0f) "" else if (maxValue <= 1f) {
                        val roundedTenth = (value * 10f).roundToInt() / 10f
                        if (kotlin.math.abs(value - roundedTenth) < 0.01f) String.format("%.1fkWh", roundedTenth) else ""
                    } else {
                        "${kotlin.math.round(value).toInt()}kWh"
                    }
                }
            ),
            bottomAxis = rememberBottomAxis(
                guideline = null,  // X축 격자선 제거
                valueFormatter = { value, _ ->
                    val index = value.toInt()
                    if (index >= 0 && index < data.size) {
                        val time = data[index].time
                        // 연간 차트인 경우 월에 '월' 추가
                        if (period == "연간" && time.matches(Regex("\\d{1,2}"))) {
                            "${time}월"
                        }
                        // 월간 차트는 이미 주차별 데이터로 처리되어 들어오므로 그대로 표시
                        else time
                    } else ""
                }
            ),
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyScreenPreview() {
    EeumTheme {
        EnergyScreen()
    }
}
