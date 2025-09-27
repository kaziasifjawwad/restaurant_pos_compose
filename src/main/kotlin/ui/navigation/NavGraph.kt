package ui.navigation

import androidx.compose.runtime.Composable
import data.model.MenuItem
import ui.screens.PlaceholderScreen
import ui.screens.FoodItemListScreen

sealed class MenuDestination(val menuCode: String) {
    object FoodItem : MenuDestination("FOOD_ITEM")
    // add other destinations later
}

@Composable
fun NavigationHost(
    currentMenuCode: String,
    currentMenuItem: MenuItem? = null
) {
    when (currentMenuCode) {
        MenuDestination.FoodItem.menuCode -> {
            FoodItemListScreen()
        }
        else -> {
            PlaceholderScreen(menuItem = currentMenuItem)
        }
    }
}
