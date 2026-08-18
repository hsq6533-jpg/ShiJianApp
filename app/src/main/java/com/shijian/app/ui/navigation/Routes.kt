package com.shijian.app.ui.navigation

/** 路由常量 */
object Routes {
    const val HOME = "home"
    const val FOOD = "food"
    const val EXPENSE = "expense"
    const val NEWS = "news"
    const val PROFILE = "profile"

    const val EXPENSE_FILTER = "expense?filter={filter}"

    const val ADD_RECORD = "add_record?editId={editId}&type={type}&date={date}"
    const val CALENDAR_DETAIL = "calendar_detail/{date}"
    const val NEWS_SETTINGS = "news_settings"
    const val ADDRESS_MANAGE = "address_manage"
    const val FOOD_SETTINGS = "food_settings"
    const val FOOD_LIST = "food_list/{type}"
    const val STATS = "stats"
    const val UPDATES = "updates"
    const val PRIVACY = "privacy"

    val TABS = listOf(HOME, FOOD, EXPENSE, NEWS, PROFILE)
}
