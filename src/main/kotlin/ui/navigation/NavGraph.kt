package ui.navigation

import androidx.compose.runtime.*
import data.model.MenuItem
import ui.screens.*
import ui.screens.dashboard.pro.DashboardScreenPro
import ui.screens.pos.*
import ui.screens.takeout.*
import ui.screens.takeout.report.TakeoutReportScreen
import ui.viewmodel.PosViewModel
import ui.viewmodel.TakeoutViewModel

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

sealed class TakeoutDestination {
    object List : TakeoutDestination()
    object Create : TakeoutDestination()
    data class View(val orderId: Long) : TakeoutDestination()
    data class Edit(val orderId: Long) : TakeoutDestination()
    object Mediums : TakeoutDestination()
    object Report : TakeoutDestination()
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
    object PosPrinter : MenuDestination("POS_PRINTER")
    object Takeout : MenuDestination("TAKEOUT")
    object TakeoutOrder : MenuDestination("TAKEOUT_ORDER")
    object TakeoutMedium : MenuDestination("TAKEOUT_MEDIUM")
    object TakeoutReport : MenuDestination("TAKEOUT_REPORT")
    object Ingredients : MenuDestination("INGREDIENTS")
    object FoodCategory : MenuDestination("FOOD_CATEGORY")
    object SetupMenu : MenuDestination("SETUP_MENU")
    object MenuAssign : MenuDestination("MENU_ASSIGN")
    object MenuRole : MenuDestination("MENU_ROLE")
    object PermissionSetup : MenuDestination("PERMISSION_SETUP")
    object PermissionAssign : MenuDestination("PERMISSION_ASSIGN")
    object Permission : MenuDestination("PERMISSION")
    object RoleSetup : MenuDestination("ROLE")
    object Users : MenuDestination("USER_INFO")
    object UserManagement : MenuDestination("USER_MANAGEMENT")
    object Report : MenuDestination("REPORT")
    object ReportPos : MenuDestination("REPORT_POS")
    object CompleteFoodOrder : MenuDestination("COMPLETE_FOOD_ORDER")

    companion object {
        private val POS_VARIANTS = setOf("POS", "POS_ORDER", "POS_ORDERS", "POS_MANAGEMENT")
        private val TAKEOUT_VARIANTS = setOf("TAKEOUT", "TAKEOUT_ORDER", "TAKEOUT_ORDERS", "TAKEOUT_MANAGEMENT", "PARCEL_ORDER", "DELIVERY_ORDER")
        fun isPosMenu(menuCode: String): Boolean = menuCode.uppercase() in POS_VARIANTS
        fun isTakeoutMenu(menuCode: String): Boolean = menuCode.uppercase() in TAKEOUT_VARIANTS
    }
}

@Composable
fun NavigationHost(currentMenuCode: String, currentMenuItem: MenuItem? = null) {
    when {
        currentMenuCode == MenuDestination.Dashboard.menuCode -> DashboardScreenPro()
        currentMenuCode == MenuDestination.FoodItem.menuCode -> FoodItemNavigationHost()
        currentMenuCode == MenuDestination.Beverage.menuCode -> BeverageNavigationHost()
        currentMenuCode == MenuDestination.PosPrinter.menuCode -> PrinterSettingsScreen()
        MenuDestination.isPosMenu(currentMenuCode) -> PosNavigationHost()
        currentMenuCode == MenuDestination.TakeoutMedium.menuCode -> TakeoutNavigationHost(TakeoutDestination.Mediums)
        currentMenuCode == MenuDestination.TakeoutReport.menuCode -> TakeoutNavigationHost(TakeoutDestination.Report)
        MenuDestination.isTakeoutMenu(currentMenuCode) -> TakeoutNavigationHost()
        currentMenuCode == MenuDestination.Ingredients.menuCode -> ui.screens.inventory.IngredientsScreen()
        currentMenuCode == MenuDestination.FoodCategory.menuCode -> ui.screens.inventory.FoodCategoryScreen()
        currentMenuCode == MenuDestination.SetupMenu.menuCode -> ui.screens.menu.MenuSetupScreen()
        currentMenuCode == MenuDestination.MenuAssign.menuCode || currentMenuCode == MenuDestination.MenuRole.menuCode -> ui.screens.menu.MenuAssignScreen()
        currentMenuCode == MenuDestination.PermissionSetup.menuCode || currentMenuCode == MenuDestination.Permission.menuCode -> ui.screens.menu.MenuPermissionTypesScreen()
        currentMenuCode == MenuDestination.PermissionAssign.menuCode -> ui.screens.menu.MenuPermissionsScreen()
        currentMenuCode == MenuDestination.RoleSetup.menuCode -> ui.screens.menu.RoleSetupScreen()
        currentMenuCode == MenuDestination.Users.menuCode -> ui.screens.user.UserManagementScreen()
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
fun TakeoutNavigationHost(initialDestination: TakeoutDestination = TakeoutDestination.List) {
    var destination by remember { mutableStateOf(initialDestination) }
    val viewModel = remember { TakeoutViewModel() }
    when (val current = destination) {
        is TakeoutDestination.List -> TakeoutOrderListScreen(
            viewModel = viewModel,
            onNavigateToCreate = { destination = TakeoutDestination.Create },
            onNavigateToDetail = { destination = TakeoutDestination.View(it) },
            onNavigateToEdit = { destination = TakeoutDestination.Edit(it) },
            onNavigateToMediums = { destination = TakeoutDestination.Mediums }
        )
        is TakeoutDestination.Create -> TakeoutOrderEditorScreen(
            orderId = null,
            viewModel = viewModel,
            onNavigateBack = { destination = TakeoutDestination.List }
        )
        is TakeoutDestination.View -> TakeoutOrderDetailScreen(
            orderId = current.orderId,
            viewModel = viewModel,
            onNavigateBack = { destination = TakeoutDestination.List },
            onNavigateToEdit = { destination = TakeoutDestination.Edit(it) }
        )
        is TakeoutDestination.Edit -> TakeoutOrderEditorScreen(
            orderId = current.orderId,
            viewModel = viewModel,
            onNavigateBack = { destination = TakeoutDestination.List }
        )
        is TakeoutDestination.Mediums -> TakeoutMediumListScreen(
            viewModel = viewModel,
            onNavigateBack = { destination = TakeoutDestination.List }
        )
        is TakeoutDestination.Report -> TakeoutReportScreen()
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
