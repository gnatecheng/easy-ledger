package com.qingjizhang.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.qingjizhang.app.R
import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.AccountType
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.RecurringFrequency
import com.qingjizhang.app.domain.TxnKind
import java.util.Locale

private val categoryNameToRes: Map<String, Int> = mapOf(
    "餐饮" to R.string.cat_dining,
    "交通" to R.string.cat_transport,
    "购物" to R.string.cat_shopping,
    "住房" to R.string.cat_housing,
    "日用" to R.string.cat_daily,
    "娱乐" to R.string.cat_entertainment,
    "医疗" to R.string.cat_medical,
    "教育" to R.string.cat_education,
    "通讯" to R.string.cat_communication,
    "人情" to R.string.cat_gifts,
    "宠物" to R.string.cat_pets,
    "工资" to R.string.cat_salary,
    "奖金" to R.string.cat_bonus,
    "理财" to R.string.cat_investment,
    "兼职" to R.string.cat_side_job,
    "红包" to R.string.cat_red_packet,
    "转账" to R.string.txn_transfer,
    "其他" to R.string.cat_other,
)

private val accountNameToRes: Map<String, Int> = mapOf(
    "现金" to R.string.account_type_cash,
    "银行卡" to R.string.account_type_bank,
    "信用卡" to R.string.account_type_credit_card,
    "支付宝" to R.string.account_type_alipay,
    "微信" to R.string.account_type_wechat,
)

@Composable
fun isEnglishUi(): Boolean {
    val lang = LocalConfiguration.current.locales[0].language
    return lang.startsWith("en")
}

fun localizedCategoryName(context: Context, storedName: String): String {
    val resId = categoryNameToRes[storedName] ?: return storedName
    val lang = context.resources.configuration.locales[0].language
    return if (lang.startsWith("en")) context.getString(resId) else storedName
}

@Composable
fun localizedCategoryName(storedName: String): String {
    val resId = categoryNameToRes[storedName] ?: return storedName
    return if (isEnglishUi()) stringResource(resId) else storedName
}

@Composable
fun Category.displayName(): String = localizedCategoryName(name)

@Composable
fun localizedAccountName(storedName: String, type: AccountType): String {
    if (!isEnglishUi()) return storedName
    accountNameToRes[storedName]?.let { return stringResource(it) }
    if (storedName == type.label) return type.localizedLabel()
    return storedName
}

@Composable
fun Account.displayName(): String = localizedAccountName(name, type)

@Composable
fun TxnKind.localizedLabel(): String = when (this) {
    TxnKind.EXPENSE -> stringResource(R.string.txn_expense)
    TxnKind.INCOME -> stringResource(R.string.txn_income)
    TxnKind.TRANSFER -> stringResource(R.string.txn_transfer)
}

@Composable
fun AccountType.localizedLabel(): String = when (this) {
    AccountType.CASH -> stringResource(R.string.account_type_cash)
    AccountType.BANK -> stringResource(R.string.account_type_bank)
    AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
    AccountType.ALIPAY -> stringResource(R.string.account_type_alipay)
    AccountType.WECHAT -> stringResource(R.string.account_type_wechat)
    AccountType.OTHER -> stringResource(R.string.account_type_other)
}

@Composable
fun RecurringFrequency.localizedLabel(): String = when (this) {
    RecurringFrequency.DAILY -> stringResource(R.string.freq_daily)
    RecurringFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
    RecurringFrequency.MONTHLY -> stringResource(R.string.freq_monthly)
}

@Composable
fun pieChartOtherCategoryName(): String = stringResource(R.string.cat_other)

fun formatLocale(): Locale =
    if (Locale.getDefault().language.startsWith("en")) Locale.US else Locale.CHINA
