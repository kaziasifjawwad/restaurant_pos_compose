package ui.navigation

import androidx.compose.runtime.*
import data.model.MenuItem
import ui.screens.*
import ui.screens.dashboard.pro.DashboardScreenPro
import ui.screens.pos.*
import ui.viewmodel.PosViewModel

sealed class FoodItemDestination {
    object List : FoodItemDestination()
    object Create : FoodItemDestination()
    data class View(val itemId: Long) : FoodItemDestination()
    data class Edit(val itemId: Long) : FoodItemDestination()
}

sealed class BeverageDestination {
    object List : BeverageDestination()
    object Create : BeverageDestination()
    data class View(val beverageId: Long) : BeverageDestination()
    data class Edit(val beverageId: Long) : BeverageDestination()
}

sealed class PosDestination {
    object List : PosDestination()
    object Create : PosDestination()
    data class View(val orderId: Long) : PosDestination()
    data class Edit(val orderId: Long) : PosDestination()
}

sealed class ReportDestination {
    object List : ReportDestination()
    data class Detail(val orderId: String) : ReportDestination()
}

sealed class MenuDestination(val menuCode: String) {
    object Dashboard : MenuDestination("DASHBOARD")
    object FoodItem : MenuDestination("FOOD_ITEM")
    object Beverage : MenuDestination("BEVERAGE")
    object Pos : MenuDestination("POS")
    object Ingredients : MenuDestination("INGREDIENTS")
    object FoodCategory : MenuDestination("FOOD_CATEGORY")
    object SetupMenu : MenuDestination("SETUP_MENU")
    object MenuAssign : MenuDestination("MENU_ASSIGN")
    object MenuRole : MenuDestination("MENU_ROLE")
    object PermissionSetup : MenuDestination("PERMISSION_SETUP")
    object PermissionAssign : MenuDestination("PERMISSION_ASSIGN")
    object Permission : MenuDestination("PERMISSION")
    object RoleSetup : MenuDestination("ROLE")
    object UserRegistration : MenuDestination("USER_REGISTRATION")
    object Report : MenuDestination("REPORT")
    object ReportPos : MenuDestination("REPORT_POS")
    object CompleteFoodOrder : MenuDestination("COMPLETE_FOOD_ORDER")

    companion object {
        private val POS_VARIANTS = setOf("POS", "POS_ORDER", "POS_ORDERS", "POS_MANAGEMENT")
        fun isPosMenu(menuCode: String): Boolean = menuCode.uppercase() in POS_VARIANTS
    }
}

@Composable
fun NavigationHost(currentMenuCode: String, currentMenuItem: MenuItem? = null) {
    when {
        currentMenuCode == MenuDestination.Dashboard.menuCode -> DashboardScreenPro()
        currentMenuCode == MenuDestination.FoodItem.menuCode -> FoodItemNavigationHost()
        currentMenuCode == MenuDestination.Beverage.menuCode -> BeverageNavigationHost()
        MenuDestination.isPosMenu(currentMenuCode) -> PosNavigationHost()
        currentMenuCode == MenuDestination.Ingredients.menuCode -> ui.screens.inventory.IngredientsScreen()
        currentMenuCode == MenuDestination.FoodCategory.menuCode -> ui.screens.inventory.FoodCategoryScreen()
        currentMenuCode == MenuDestination.SetupMenu.menuCode -> ui.screens.menu.MenuSetupScreen()
        currentMenuCode == MenuDestination.MenuAssign.menuCode || currentMenuCode == MenuDestination.MenuRole.menuCode -> ui.screens.menu.MenuAssignScreen()
        currentMenuCode == MenuDestination.PermissionSetup.menuCode || currentMenuCode == MenuDestination.Permission.menuCode -> ui.screens.menu.MenuPermissionTypesScreen()
        currentMenuCode == MenuDestination.PermissionAssign.menuCode -> ui.screens.menu.MenuPermissionsScreen()
        currentMenuCode == MenuDestination.RoleSetup.menuCode -> ui.screens.menu.RoleSetupScreen()
        currentMenuCode == MenuDestination.UserRegistration.menuCode -> ui.screens.user.UserRegistrationScreen()
        currentMenuCode == MenuDestination.Report.menuCode || currentMenuCode == MenuDestination.ReportPos.menuCode -> ReportNavigationHost()
        currentMenuCode == MenuDestination.CompleteFoodOrder.menuCode -> ReportNavigationHost()
        else -> PlaceholderScreen(menuItem = currentMenuItem)
    }
}

@Composable
fun FoodItemNavigationHost() {
    var destination by remember { mutableStateOf<FoodItemDestination>(FoodItemDestination.List) }
    when (val current = destination) {
        is FoodItemDestination.List -> FoodItemListScreen(
            onNavigateToCreate = { destination = FoodItemDestination.Create },
            onNavigateToView = { destination = FoodItemDestination.View(it) },
            onNavigateToEdit = { destination = FoodItemDestination.Edit(it) }
        )
        is FoodItemDestination.Create -> FoodItemEditScreen(
            foodItemId = null,
            onNavigateBack = { destination = FoodItemDestination.List },
            onSaveSuccess = { destination = FoodItemDestination.List }
        )
        is FoodItemDestination.View -> FoodItemViewScreen(
            foodItemId = current.itemId,
            onNavigateBack = { destination = FoodItemDestination.List },
            onEdit = { destination = FoodItemDestination.Edit(it) }
        )
        is FoodItemDestination.Edit -> FoodItemEditScreen(
            foodItemId = current.itemId,
            onNavigateBack = { destination = FoodItemDestination.List },
            onSaveSuccess = { destination = FoodItemDestination.List }
        )
    }
}

@Composable
fun BeverageNavigationHost() {
    var destination by remember { mutableStateOf<BeverageDestination>(BeverageDestination.List) }
    when (val current = destination) {
        is BeverageDestination.List -> BeverageListScreen(
            onNavigateToCreate = { destination = BeverageDestination.Create },
            onNavigateToView = { destination = BeverageDestination.View(it) },
            onNavigateToEdit = { destination = BeverageDestination.Edit(it) }
        )
        is BeverageDestination.Create -> BeverageEditScreen(
            beverageId = null,
            onNavigateBack = { destination = BeverageDestination.List },
            onSaveSuccess = { destination = BeverageDestination.List }
        )
        is BeverageDestination.View -> BeverageViewScreen(
            beverageId = current.beverageId,
            onNavigateBack = { destination = BeverageDestination.List },
            onNavigateToEdit = { destination = BeverageDestination.Edit(it) }
        )
        is BeverageDestination.Edit -> BeverageEditScreen(
            beverageId = current.beverageId,
            onNavigateBack = { destination = BeverageDestination.List },
            onSaveSuccess = { destination = BeverageDestination.List }
        )
    }
}

@Composable
fun PosNavigationHost() {
    var destination by remember { mutableStateOf<PosDestination>(PosDestination.List) }
    val viewModel = remember { PosViewModel() }
    when (val current = destination) {
        is PosDestination.List -> PosOrderListScreen(
            viewModel = viewModel,
            onNavigateToCreate = { destination = PosDestination.Create },
            onNavigateToDetail = { destination = PosDestination.View(it) },
            onNavigateToEdit = { destination = PosDestination.Edit(it) }
        )
        is PosDestination.Create -> PosOrderEditorScreen(
            orderId = null,
            viewModel = viewModel,
            onNavigateBack = { destination = PosDestination.List }
        )
        is PosDestination.View -> PosOrderDetailScreen(
            orderId = current.orderId,
            viewModel = viewModel,
            onNavigateBack = { destination = PosDestination.List },
            onNavigateToEdit = { destination = PosDestination.Edit(it) }
        )
        is PosDestination.Edit -> PosOrderEditorScreen(
            orderId = current.orderId,
            viewModel = viewModel,
            onNavigateBack = { destination = PosDestination.List }
        )
    }
}

@Composable
fun ReportNavigationHost() {
    var destination by remember { mutableStateOf<ReportDestination>(ReportDestination.List) }
    when (val current = destination) {
        is ReportDestination.List -> ui.screens.report.PosReportPaymentFilterScreen(
            onNavigateToDetail = { destination = ReportDestination.Detail(it) }
        )
        is ReportDestination.Detail -> ui.screens.report.PosOrderDetailScreen(
            orderId = current.orderId,
            onNavigateBack = { destination = ReportDestination.List }
        )
    }
}
