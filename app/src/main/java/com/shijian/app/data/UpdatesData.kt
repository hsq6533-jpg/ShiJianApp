package com.shijian.app.data

/** 版本历史（更新公告，5.7） */
data class UpdateEntry(
    val version: String,
    val date: String,
    val isLatest: Boolean = false,
    val changes: List<String>
)

object UpdatesData {

    /** 当前版本由 BuildConfig 提供，历史在此维护 */
    val HISTORY = listOf(
        UpdateEntry(
            version = "v1.1.0",
            date = "2026-08-18",
            isLatest = true,
            changes = listOf(
                "新增「本月支出拆解」首页双卡，Top3 分类占比一目了然",
                "美食页新增按地址搜索模式，支持地址解析后周边搜索",
                "备份支持 AES-256-GCM 加密导出",
                "日历明细页增加结余显示与记一笔快捷入口"
            )
        ),
        UpdateEntry(
            version = "v1.0.0",
            date = "2026-08-01",
            changes = listOf(
                "首版上线：记账、日历、下班倒计时",
                "美食搜索：高德 Web API 多点位全量搜索 + 30 天缓存",
                "AI 资讯：DeepSeek 生成 + 定时推送",
                "SQLCipher 全库加密，纯本地运行，数据不出设备"
            )
        )
    )
}
