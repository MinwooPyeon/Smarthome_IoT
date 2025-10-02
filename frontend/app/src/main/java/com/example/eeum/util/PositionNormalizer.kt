package com.example.eeum.util

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.eeum.data.model.dto.device.LocationItem
import com.example.eeum.data.model.response.device.DeviceItem
import kotlin.math.max
import kotlin.math.min


data class RenderMetrics(
    val container: IntSize,
    val imageIntrinsic: IntSize,
    val iconSizePx: Float
)

data class NormalizedPosition(
    val x: Double,
    val y: Double
)


object PositionNormalizer {

    /**
     * 주어진 아이콘의 좌측-상단 오프셋(픽셀) 기준으로, 아이콘 중심점을 계산한 뒤
     * 평면도가 실제로 표시된 영역(drawnRect) 기준 0..1 정규화 좌표로 변환합니다.
     */
    fun normalizeOffset(offset: IntOffset, metrics: RenderMetrics): NormalizedPosition {
        val container = metrics.container
        val image = metrics.imageIntrinsic
        val icon = metrics.iconSizePx

        if (container.width <= 0 || container.height <= 0 || image.width <= 0 || image.height <= 0) {
            return NormalizedPosition(0.0, 0.0)
        }

        // 이미지가 ContentScale.Fit로 그려졌을 때의 스케일과 렛터박스 마진 계산
        val scaleW = container.width.toFloat() / image.width.toFloat()
        val scaleH = container.height.toFloat() / image.height.toFloat()
        val scale = min(scaleW, scaleH)

        val drawnW = image.width * scale
        val drawnH = image.height * scale

        val leftMargin = (container.width - drawnW) / 2f
        val topMargin = (container.height - drawnH) / 2f

        // 아이콘의 중심 픽셀 좌표(컨테이너 기준)
        val centerX = offset.x + icon / 2f
        val centerY = offset.y + icon / 2f

        // drawnRect 내부 좌표로 변환 (0..drawnW, 0..drawnH 범위로 클램프)
        val xInImagePx = (centerX - leftMargin).coerceIn(0f, drawnW)
        val yInImagePx = (centerY - topMargin).coerceIn(0f, drawnH)

        val nx = if (drawnW > 0) xInImagePx / drawnW else 0f
        val ny = if (drawnH > 0) yInImagePx / drawnH else 0f

        return NormalizedPosition(nx.toDouble(), ny.toDouble())
    }

    /**
     * 드래그된 오프셋 맵(deviceId -> 오프셋)을 서버로 보낼 LocationItem 리스트로 변환합니다.
     * - devices: 현재 화면에 표시된 디바이스 목록(여기서 roomId를 가져옵니다)
     * - homeId: 현재 대표 집 혹은 선택된 집의 ID
     */
    fun buildLocationItemsFromOffsets(
        offsets: Map<String, IntOffset>,
        metrics: RenderMetrics,
        devices: List<DeviceItem>,
        homeId: Int
    ): List<LocationItem> {
        if (offsets.isEmpty()) return emptyList()

        // deviceId -> DeviceItem 매핑
        val deviceMap = devices.associateBy { it.deviceId }

        return offsets.mapNotNull { (key, offset) ->
            val deviceId = key.toIntOrNull() ?: return@mapNotNull null
            val device = deviceMap[deviceId] ?: return@mapNotNull null

            val pos = normalizeOffset(offset, metrics)

            LocationItem(
                deviceId = deviceId,
                homeId = homeId,
                roomId = device.roomId,
                x = pos.x,
                y = pos.y
            )
        }
    }
}
