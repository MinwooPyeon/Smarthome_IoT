package com.example.eeum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.eeum.ui.theme.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
// Vico (Compose)
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
// YCharts 라이브러리
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData

@Composable
fun EnergyScreen(
    modifier: Modifier = Modifier,
    viewModel: EnergyViewModel = viewModel()
) {
    // 각 섹션의 독립적인 상태 관리
    var energyUsageSelectedPeriod by remember { mutableStateOf("일간") }
    var deviceReportSelectedPeriod by remember { mutableStateOf("일간") }
    
    // ViewModel 데이터 관찰
    val energySeries by viewModel.energySeries.observeAsState(emptyList())
    val totalKwh by viewModel.totalKwh.observeAsState(0.0)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    
    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.fetchEnergyUsageByPeriod("일간")
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
                        viewModel.fetchEnergyUsageByPeriod(period)
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
                    onPeriodChange = { deviceReportSelectedPeriod = it }
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
                    .height(200.dp)
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
                        if (selectedPeriod == "일간" || selectedPeriod == "주간") {
                            // 막대그래프
                            EnergyBarChart(
                                data = energyData.map { EnergyData(it.label, it.kwh.toFloat()) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // 라인차트 (월간, 연간)
                            EnergyLineChart(
                                data = energyData.map { EnergyData(it.label, it.kwh.toFloat()) },
                                modifier = Modifier.fillMaxSize()
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
    onPeriodChange: (String) -> Unit
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
                    DeviceUsagePieChart(
                        data = getSampleDevices(),
                        modifier = Modifier.size(250.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 제품별 사용량 목록
                DeviceUsageList()
            }
        }
    }
}

// 제품별 사용량 목록
@Composable
private fun DeviceUsageList() {
    val devices = getSampleDevices()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        devices.forEach { device ->
            DeviceUsageItem(device)
        }
    }
}

// 개별 제품 사용량 아이템
@Composable
private fun DeviceUsageItem(device: DeviceUsage) {
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
                    .background(device.color)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = "${device.usage} (${device.percentage.toInt()}%)",
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

// 데이터 클래스
private data class DeviceUsage(
    val name: String,
    val usage: String,
    val percentage: Float,
    val color: androidx.compose.ui.graphics.Color
)

// 시간별 에너지 사용량 데이터
private data class EnergyData(
    val time: String,
    val value: Float
)

// 6가지 디바이스의 샘플 데이터 생성
private fun getSampleDevices(): List<DeviceUsage> {
    return listOf(
        DeviceUsage("에어컨", "18.6 kWh", 45f, PastelBlue),
        DeviceUsage("텔레비전", "8.2 kWh", 20f, PastelOrange),
        DeviceUsage("선풍기", "6.4 kWh", 15f, PastelGreen),
        DeviceUsage("빔프로젝터", "4.1 kWh", 10f, PastelRed),
        DeviceUsage("공기청정기", "2.5 kWh", 6f, PastelPurple),
        DeviceUsage("조명", "1.6 kWh", 4f, PastelYellow)
    )
}


// 파이차트 컴포넌트 (YCharts 라이브러리 사용)
@Composable
private fun DeviceUsagePieChart(
    data: List<DeviceUsage>,
    modifier: Modifier = Modifier
) {
    var selectedSlice by remember { mutableStateOf<DeviceUsage?>(null) }
    
    Box(modifier = modifier) {
        val pieChartData = PieChartData(
            slices = data.map { device ->
                PieChartData.Slice(
                    label = device.name,
                    value = device.percentage,
                    color = device.color
                )
            },
            plotType = PlotType.Pie
        )
        
        val pieChartConfig = PieChartConfig(
            labelVisible = false,  // 라벨 숨김
            strokeWidth = 2f,
            labelColor = MaterialTheme.colorScheme.onSurface,
            activeSliceAlpha = .8f,
            isAnimationEnable = true,
            showSliceLabels = false,  // 슬라이스 라벨 숨김
            isClickOnSliceEnabled = true  // 슬라이스 클릭 활성화
        )
        
        PieChart(
            modifier = Modifier.fillMaxSize(),
            pieChartData = pieChartData,
            pieChartConfig = pieChartConfig,
            onSliceClick = { slice ->
                // 선택된 슬라이스 업데이트
                selectedSlice = data.find { it.name == slice.label }
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
                        containerColor = device.color.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = device.name,
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

// 누적 컴럼 차트 컴포넌트 (Vico 라이브러리 사용) - 백업용
@Composable
private fun DeviceUsageStackedColumnChart(
    data: List<DeviceUsage>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { ChartEntryModelProducer() }
    LaunchedEffect(data) {
        val series: List<List<com.patrykandpatrick.vico.core.entry.ChartEntry>> =
            data.map { device -> listOf(entryOf(0f, device.percentage)) }
        modelProducer.setEntries(series)
    }
    ProvideChartStyle(m3ChartStyle()) {
        Chart(
            chart = columnChart(mergeMode = ColumnChart.MergeMode.Stack),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
            modifier = modifier
        )
    }
}

// 에너지 사용량 막대 차트 (Vico)
@Composable
private fun EnergyBarChart(
    data: List<EnergyData>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { ChartEntryModelProducer() }
    LaunchedEffect(data) {
        val series = listOf(data.mapIndexed { index, e -> entryOf(index.toFloat(), e.value) })
        modelProducer.setEntries(series)
    }
    ProvideChartStyle(m3ChartStyle()) {
        Chart(
            chart = columnChart(),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
            modifier = modifier
        )
    }
}

// 에너지 사용량 라인 차트 (Vico)
@Composable
private fun EnergyLineChart(
    data: List<EnergyData>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { ChartEntryModelProducer() }
    LaunchedEffect(data) {
        val series = listOf(data.mapIndexed { index, e -> entryOf(index.toFloat(), e.value) })
        modelProducer.setEntries(series)
    }
    ProvideChartStyle(m3ChartStyle()) {
        Chart(
            chart = lineChart(),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
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
