package com.shilapi.performance.model

import java.util.Locale

data class CpuMetrics(
    val usagePercent: Int, // 0..100, -1 = 计算中或不可用
    val coreCount: Int,
    val frequencyMhz: Int, // <=0 = 不可用
)

data class MemoryMetrics(
    val usedBytes: Long,
    val totalBytes: Long,
    val isLowMemory: Boolean,
)

data class StorageMetrics(
    val usedBytes: Long,
    val totalBytes: Long,
)

data class LinkMetrics(
    val type: LinkType,
    val detail: String,
)

data class MediaMetrics(
    val rxFps: Float,
    val shownFps: Float,
    val touchLatencyMs: Long,
    val droppedFrames: Int,
    val displayRefreshHz: Float = -1f,
)

data class PerformanceSnapshot(
    val cpu: CpuMetrics,
    val memory: MemoryMetrics,
    val storage: StorageMetrics,
    val link: LinkMetrics,
    val media: MediaMetrics? = null,
) {
    val cpuText: String
        get() {
            val usage = if (cpu.usagePercent >= 0) "${cpu.usagePercent}%" else "..."
            val freq = if (cpu.frequencyMhz > 0) {
                if (cpu.frequencyMhz >= 1000) String.format(Locale.US, "%.1fGHz", cpu.frequencyMhz / 1000.0)
                else "${cpu.frequencyMhz}MHz"
            } else "N/A"
            return "CPU: $usage (${cpu.coreCount}核 @ $freq)"
        }

    val memoryText: String
        get() {
            if (memory.totalBytes <= 0) return "RAM: N/A"
            val usedGb = memory.usedBytes / (1024.0 * 1024 * 1024)
            val totalGb = memory.totalBytes / (1024.0 * 1024 * 1024)
            val pct = (memory.usedBytes * 100 / memory.totalBytes).toInt()
            val warn = if (memory.isLowMemory) " [低内存警告]" else ""
            return String.format(Locale.US, "RAM: %.1f GB / %.1f GB (%d%%)%s", usedGb, totalGb, pct, warn)
        }

    val storageText: String
        get() {
            if (storage.totalBytes <= 0) return "机身存储: N/A"
            val usedGb = storage.usedBytes / (1024.0 * 1024 * 1024)
            val totalGb = storage.totalBytes / (1024.0 * 1024 * 1024)
            val pct = (storage.usedBytes * 100 / storage.totalBytes).toInt()
            return String.format(Locale.US, "机身存储: %.1f GB / %.1f GB (%d%%)", usedGb, totalGb, pct)
        }

    val linkText: String
        get() = when (link.type) {
            LinkType.USB_WIRED -> "链路: 有线连接 (${link.detail})"
            LinkType.CAR_HOTSPOT -> "链路: 车机热点 · ${link.detail}"
            LinkType.WIFI_CLIENT -> "链路: Wi-Fi · ${link.detail}"
            LinkType.UNKNOWN -> "链路: ${link.detail}"
        }

    val mediaText: String
        get() {
            if (media == null) return "投屏: 待机"
            val touch = if (media.touchLatencyMs >= 0) " · 延迟: ${media.touchLatencyMs}ms" else ""
            val refresh = if (media.displayRefreshHz > 0f) {
                String.format(Locale.US, " · 屏幕刷新率 %.0fHz", media.displayRefreshHz)
            } else ""
            return String.format(
                Locale.US,
                "视频 FPS: 接收 %.1f / 提交 %.1f%s%s",
                media.rxFps,
                media.shownFps,
                touch,
                refresh,
            )
        }
}
