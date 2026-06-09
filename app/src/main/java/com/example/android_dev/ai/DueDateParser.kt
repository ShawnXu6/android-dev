package com.example.android_dev.ai

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// 自然语言截止日期解析功能：从「下周五前完成期末论文」这类输入里提取目标日期。
// 纯本地规则实现，供本地兜底拆解与快速捕捉复用。
object DueDateParser {

    // 解析功能：尽量从文本里识别出一个截止日期，识别不到返回 null。
    fun parse(text: String, today: LocalDate = LocalDate.now()): LocalDate? {
        val t = text.lowercase()

        relativeKeyword(t, today)?.let { return it }
        weekdayExpression(t, today)?.let { return it }
        explicitDate(text, today)?.let { return it }
        withinDays(t, today)?.let { return it }

        return null
    }

    // 相对关键词：今天 / 明天 / 后天 / 大后天。
    private fun relativeKeyword(t: String, today: LocalDate): LocalDate? = when {
        t.contains("今天") || t.contains("今日") -> today
        t.contains("明天") || t.contains("明日") -> today.plusDays(1)
        t.contains("后天") -> today.plusDays(2)
        t.contains("大后天") -> today.plusDays(3)
        else -> null
    }

    // 星期表达：本周X / 下周X / 周X / 星期X，自动取未来最近的那天。
    private fun weekdayExpression(t: String, today: LocalDate): LocalDate? {
        val target = weekdayOf(t) ?: return null
        val nextThisOrFuture = today.with(TemporalAdjusters.nextOrSame(target))
        return when {
            t.contains("下周") || t.contains("下星期") || t.contains("下礼拜") ->
                today.with(TemporalAdjusters.next(target)).let {
                    // 确保落在下一个自然周。
                    if (it == nextThisOrFuture) it.plusWeeks(1) else it
                }
            else -> nextThisOrFuture
        }
    }

    private fun weekdayOf(t: String): DayOfWeek? = when {
        t.contains("周一") || t.contains("星期一") || t.contains("礼拜一") -> DayOfWeek.MONDAY
        t.contains("周二") || t.contains("星期二") || t.contains("礼拜二") -> DayOfWeek.TUESDAY
        t.contains("周三") || t.contains("星期三") || t.contains("礼拜三") -> DayOfWeek.WEDNESDAY
        t.contains("周四") || t.contains("星期四") || t.contains("礼拜四") -> DayOfWeek.THURSDAY
        t.contains("周五") || t.contains("星期五") || t.contains("礼拜五") -> DayOfWeek.FRIDAY
        t.contains("周六") || t.contains("星期六") || t.contains("礼拜六") -> DayOfWeek.SATURDAY
        t.contains("周日") || t.contains("周天") || t.contains("星期日") || t.contains("星期天") -> DayOfWeek.SUNDAY
        else -> null
    }

    // 显式日期：6月20日 / 6-20 / 2026-06-20 / 6/20。
    private val isoRegex = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""")
    private val cnRegex = Regex("""(\d{1,2})\s*月\s*(\d{1,2})\s*[日号]""")
    private val slashRegex = Regex("""(?<!\d)(\d{1,2})[-/](\d{1,2})(?!\d)""")

    private fun explicitDate(text: String, today: LocalDate): LocalDate? {
        isoRegex.find(text)?.let { m ->
            return safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        cnRegex.find(text)?.let { m ->
            return monthDayInFuture(m.groupValues[1].toInt(), m.groupValues[2].toInt(), today)
        }
        slashRegex.find(text)?.let { m ->
            return monthDayInFuture(m.groupValues[1].toInt(), m.groupValues[2].toInt(), today)
        }
        return null
    }

    // N 天后 / N 天内 / 一周内。
    private val withinRegex = Regex("""(\d{1,2})\s*天[后内]""")
    private fun withinDays(t: String, today: LocalDate): LocalDate? {
        if (t.contains("一周内") || t.contains("一周后") || t.contains("一个星期")) return today.plusWeeks(1)
        withinRegex.find(t)?.let { return today.plusDays(it.groupValues[1].toLong()) }
        return null
    }

    private fun monthDayInFuture(month: Int, day: Int, today: LocalDate): LocalDate? {
        val candidate = safeDate(today.year, month, day) ?: return null
        // 若该日期已过，认为指向明年同一天。
        return if (candidate.isBefore(today)) candidate.plusYears(1) else candidate
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, month, day) }.getOrNull()
}
