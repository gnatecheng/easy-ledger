package com.qingjizhang.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qingjizhang.app.R

/** Known demo/seed transaction & recurring notes — display-only English; DB keeps Chinese. */
private val demoNoteToRes: Map<String, Int> = mapOf(
    "月薪到账" to R.string.demo_note_monthly_salary_deposit,
    "季度奖金" to R.string.demo_note_quarterly_bonus,
    "余额宝收益" to R.string.demo_note_fund_yield,
    "早餐" to R.string.demo_note_breakfast,
    "午餐" to R.string.demo_note_lunch,
    "晚饭" to R.string.demo_note_dinner,
    "晚餐" to R.string.demo_note_dinner,
    "咖啡" to R.string.demo_note_coffee,
    "外卖" to R.string.demo_note_delivery,
    "超市零食" to R.string.demo_note_grocery_snacks,
    "地铁" to R.string.demo_note_subway,
    "地铁上班" to R.string.demo_note_subway_commute,
    "公交" to R.string.demo_note_bus,
    "打车" to R.string.demo_note_taxi,
    "共享单车" to R.string.demo_note_bike_share,
    "日用品" to R.string.demo_note_household,
    "衣服" to R.string.demo_note_clothes,
    "数码配件" to R.string.demo_note_tech_accessory,
    "房租" to R.string.demo_note_rent,
    "本月房租" to R.string.demo_note_rent_this_month,
    "水电" to R.string.demo_note_utilities,
    "物业" to R.string.demo_note_property_fee,
    "洗衣液" to R.string.demo_note_laundry,
    "纸巾" to R.string.demo_note_tissues,
    "电影" to R.string.demo_note_movie,
    "会员" to R.string.demo_note_membership,
    "聚餐" to R.string.demo_note_group_dining,
    "话费" to R.string.demo_note_phone_bill,
    "宽带" to R.string.demo_note_broadband,
    "买药" to R.string.demo_note_medicine,
    "体检" to R.string.demo_note_checkup,
    "网课" to R.string.demo_note_online_course,
    "书籍" to R.string.demo_note_books,
    "礼物" to R.string.demo_note_gift,
    "红包" to R.string.demo_note_red_packet,
    "耳机" to R.string.demo_note_headphones,
    "午饭" to R.string.demo_note_lunch,
    "转入支付宝备用" to R.string.demo_note_to_alipay,
    "取现" to R.string.demo_note_cash_out,
    "转生活费" to R.string.demo_note_living_transfer,
    "月薪" to R.string.demo_note_monthly_salary,
    "地铁通勤" to R.string.demo_note_subway_commute_rule,
    "周期记账" to R.string.demo_note_recurring_generated,
)

fun localizedDemoNote(context: Context, stored: String): String {
    if (stored.isBlank()) return stored
    val lang = context.resources.configuration.locales[0].language
    if (!lang.startsWith("en")) return stored
    val resId = demoNoteToRes[stored.trim()] ?: return stored
    return context.getString(resId)
}

@Composable
fun localizedDemoNote(stored: String): String {
    if (stored.isBlank()) return stored
    if (!isEnglishUi()) return stored
    val resId = demoNoteToRes[stored.trim()] ?: return stored
    return stringResource(resId)
}
