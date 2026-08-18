package com.shijian.app.data.repo

import com.shijian.app.api.ApiClient
import com.shijian.app.api.DeepSeekChatRequest
import com.shijian.app.api.DeepSeekMessage
import com.shijian.app.data.db.dao.NewsDao
import com.shijian.app.data.db.entity.NEWS_CATEGORY_COLORS
import com.shijian.app.data.db.entity.NewsConfigEntity
import com.shijian.app.data.db.entity.NewsItemEntity
import com.shijian.app.data.prefs.SecurePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 新闻仓库：DeepSeek 生成 + 定时推送；对外不抛异常。 */
class NewsRepository(
    private val dao: NewsDao,
    private val securePrefs: SecurePrefs
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeNews(): Flow<List<NewsItemEntity>> = dao.observeAll()

    fun observeConfig(): Flow<NewsConfigEntity?> = dao.observeConfig()

    suspend fun getConfig(): NewsConfigEntity = runCatching { dao.getConfig() }.getOrNull() ?: NewsConfigEntity()

    suspend fun saveConfig(c: NewsConfigEntity) = runCatching { dao.saveConfig(c) }

    suspend fun clearNews() = runCatching { dao.clearAll() }

    suspend fun markRead(id: String) = runCatching { dao.markRead(id) }

    fun hasKey(): Boolean = !securePrefs.getDeepSeekKey().isNullOrBlank()

    /**
     * 调用 DeepSeek 生成新闻（6.3 / 7.2）
     * @return 生成条数；失败返回 0 并写入日志，不抛异常。
     */
    suspend fun generate(): Int = runCatching {
        val key = securePrefs.getDeepSeekKey()
            ?: return@runCatching 0
        val cfg = getConfig()
        val prompt = buildPrompt(cfg)

        val resp = ApiClient.deepseek.chat(
            authorization = "Bearer $key",
            body = DeepSeekChatRequest(messages = listOf(DeepSeekMessage("user", prompt)))
        )
        val content = resp.choices.firstOrNull()?.message?.content
            ?: return@runCatching 0

        val items = parseNews(content, cfg.specialKeywords)
        if (items.isEmpty()) return@runCatching 0
        runCatching { dao.insertAll(items) }
        runCatching { dao.saveConfig(cfg.copy(lastUpdatedAt = System.currentTimeMillis())) }
        items.size
    }.getOrDefault(0)

    private fun buildPrompt(cfg: NewsConfigEntity): String {
        val cats = cfg.categories.split(",").filter { it.isNotBlank() }.ifEmpty { listOf("国内") }
        val keywords = cfg.specialKeywords.split(",").filter { it.isNotBlank() }
        val maxLen = when (cfg.contentLength) {
            "SHORT" -> "40"
            "LONG" -> "120"
            else -> "80"
        }
        val kw = if (keywords.isEmpty()) "无" else keywords.joinToString("、")
        return """
            请作为中文资讯编辑，生成 5 条${cats.joinToString("、")}类的短资讯。
            用户特别关心关键词：$kw。
            每条包含：标题（不超过20字）、摘要（不超过${maxLen}字）、分类（必须从 ${cats.joinToString("、")} 中选择）。
            内容须为近期发生的合理事实类资讯，不要编造具体数字时政敏感内容。
            以 JSON 数组格式返回，例如：[{"title":"...","summary":"...","category":"国内"}]
        """.trimIndent()
    }

    private fun parseNews(content: String, keywordsCsv: String): List<NewsItemEntity> {
        val keywords = keywordsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        var text = content.trim()
        text = text.removePrefix("```json").removePrefix("```").trim()
        text = text.removeSuffix("```").trim()
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val arr = try {
            json.decodeFromString<List<GeneratedNews>>(text.substring(start, end + 1))
        } catch (e: Exception) {
            return emptyList()
        }
        val now = System.currentTimeMillis()
        return arr.mapIndexed { i, g ->
            NewsItemEntity(
                title = g.title.trim(),
                summary = g.summary.trim(),
                category = g.category.trim(),
                sourceHint = "DeepSeek 生成",
                publishedAt = now - i * 60_000L,
                isSpecial = keywords.any { k ->
                    g.title.contains(k) || g.summary.contains(k)
                }
            )
        }.filter { it.title.isNotBlank() && it.category in NEWS_CATEGORY_COLORS }
    }

    @Serializable
    private data class GeneratedNews(
        val title: String = "",
        val summary: String = "",
        val category: String = ""
    )
}
