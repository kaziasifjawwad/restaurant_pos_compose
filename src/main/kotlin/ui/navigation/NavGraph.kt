package ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import data.model.MenuItem
import ui.screens.*
import ui.screens.pos.*
import ui.theme.AppAnimations
import ui.viewmodel.PosViewModel

/**
 * Navigation destinations for the Food Item module
 */
sealed class FoodItemDestination {
    object List : FoodItemDestination()
    object Create : FoodItemDestination()
    data class View(val itemId: Long) : FoodItemDestination()
    data class Edit(val itemId: Long) : FoodItemDestination()
}

/**
 * Navigation destinations for the Beverage module
 */
sealed class BeverageDestination {
    object List : BeverageDestination()
    object Create : BeverageDestination()
    data class View(val beverageId: Long) : BeverageDestination()
    data class Edit(val beverageId: Long) : BeverageDestination()
}

/**
 * Navigation destinations for the POS module
 */
sealed class PosDestination {
    object List : PosDestination()
    object Create : PosDestination()
    data class View(val orderId: Long) : PosDestination()
    data class Edit(val orderId: Long) : PosDestination()
}

/**
 * Menu destinations (codes from API)
 */
sealed class MenuDestination(val menuCode: String) {
    object FoodItem : MenuDestination("FOOD_ITEM")
    object Beverage : MenuDestination("BEVERAGE")
    object Pos : MenuDestination("POS")
    object SetupMenu : MenuDestination("SETUP_MENU")
    object MenuAssign : MenuDestination("MENU_ASSIGN")
    object PermissionSetup : MenuDestination("PERMISSION_SETUP")
    object PermissionAssign : MenuDestination("PERMISSION_ASSIGN")
    object Report : MenuDestination("REPORT")
    object CompleteFoodOrder : MenuDestination("COMPLETE_FOOD_ORDER")
    
    companion object {
        // POS menu code variants that should all route to POS
        val POS_VARIANTS = setOf("POS", "POS_ORDER", "POS_ORDERS")
        
        fun isPosMenu(menuCode: String): Boolean = menuCode.uppercase() in POS_VARIANTS
    }
}

/**
 * Main navigation host that handles menu code routing
 */
@Composable
fun NavigationHost(
    currentMenuCode: String,
    currentMenuItem: MenuItem? = null
) {
    println("[NavGraph] NavigationHost: menuCode=$currentMenuCode")
    
    when {
        currentMenuCode == MenuDestination.FoodItem.menuCode -> {
            FoodItemNavigationHost()
        }
        currentMenuCode == MenuDestination.Beverage.menuCode -> {
            BeverageNavigationHost()
        }
        MenuDestination.isPosMenu(currentMenuCode) -> {
            PosNavigationHost()
        }
        currentMenuCode == MenuDestination.SetupMenu.menuCode -> {
            ui.screens.menu.MenuSetupScreen()
        }
        currentMenuCode == MenuDestination.MenuAssign.menuCode -> {
            ui.screens.menu.MenuAssignScreen()
        }
        currentMenuCode == MenuDestination.PermissionSetup.menuCode -> {
            ui.screens.menu.MenuPermissionTypesScreen()
        }
        currentMenuCode == MenuDestination.PermissionAssign.menuCode -> {
            ui.screens.menu.MenuPermissionsScreen()
        }
        currentMenuCode == MenuDestination.Report.menuCode -> {
            ReportNavigationHost()
        }
        currentMenuCode == MenuDestination.CompleteFoodOrder.menuCode -> {
            ReportNavigationHost()
        }
        else -> {
            PlaceholderScreen(menuItem = currentMenuItem)
        }
    }
}

/**
 * Navigation host for Food Item module with internal navigation state
 */
@Composable
fun FoodItemNavigationHost() {
    var currentDestination by remember { mutableStateOf<FoodItemDestination>(FoodItemDestination.List) }
    
    println("[NavGraph] FoodItemNavigationHost: destination=${currentDestination::class.simpleName}")

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            when {
                // Going deeper (list -> view/create/edit)
                targetState is FoodItemDestination.Create ||
                targetState is FoodItemDestination.View ||
                targetState is FoodItemDestination.Edit -> {
                    (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                        slideInHorizontally(
                            initialOffsetX = { it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        )).togetherWith(
                        fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        )
                    )
                }
                // Going back (view/create/edit -> list)
                targetState is FoodItemDestination.List -> {
                    (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                        slideInHorizontally(
                            initialOffsetX = { -it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        )).togetherWith(
                        fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                        slideOutHorizontally(
                            targetOffsetX = { it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        )
                    )
                }
                else -> {
                    fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) togetherWith 
                    fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST))
                }
            }
        }
    ) { destination ->
        when (destination) {
            is FoodItemDestination.List -> {
                FoodItemListScreen(
                    onNavigateToCreate = {
                        println("[NavGraph] Navigate to Create")
                        currentDestination = FoodItemDestination.Create
                    },
                    onNavigateToView = { itemId ->
                        println("[NavGraph] Navigate to View: id=$itemId")
                        currentDestination = FoodItemDestination.View(itemId)
                    },
                    onNavigateToEdit = { itemId ->
                        println("[NavGraph] Navigate to Edit: id=$itemId")
                        currentDestination = FoodItemDestination.Edit(itemId)
                    }
                )
            }
            
            is FoodItemDestination.Create -> {
                FoodItemEditScreen(
                    foodItemId = null,
                    onNavigateBack = {
                        println("[NavGraph] Navigate back from Create to List")
                        currentDestination = FoodItemDestination.List
                    },
                    onSaveSuccess = {
                        println("[NavGraph] Create success - navigating to List")
                        currentDestination = FoodItemDestination.List
                    }
                )
            }
            
            is FoodItemDestination.View -> {
                FoodItemViewScreen(
                    foodItemId = destination.itemId,
                    onNavigateBack = {
                        println("[NavGraph] Navigate back from View to List")
                        currentDestination = FoodItemDestination.List
                    },
                    onEdit = { itemId ->
                        println("[NavGraph] Navigate from View to Edit: id=$itemId")
                        currentDestination = FoodItemDestination.Edit(itemId)
                    }
                )
            }
            
            is FoodItemDestination.Edit -> {
                FoodItemEditScreen(
                    foodItemId = destination.itemId,
                    onNavigateBack = {
                        println("[NavGraph] Navigate back from Edit to List")
                        currentDestination = FoodItemDestination.List
                    },
                    onSaveSuccess = {
                        println("[NavGraph] Edit success - navigating to List")
                        currentDestination = FoodItemDestination.List
                    }
                )
            }
        }
    }
}

/**
 * Navigation host for Beverage module with internal navigation state
 */
@Composable
fun BeverageNavigationHost() {
    var currentDestination by remember { mutableStateOf<BeverageDestination>(BeverageDestination.List) }
    
    println("[NavGraph] BeverageNavigationHost: destination=${currentDestination::class.simpleName}")

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            when {
                // Going deeper (list -> view/create/edit)
                targetState is BeverageDestination.Create ||
                targetState is BeverageDestination.View ||
                targetState is BeverageDestination.Edit -> {
                    (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                        slideInHorizontally(
                            initialOffsetX = { it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        )).togetherWith(
                        fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        )
                    )
                }
                // Going back (view/create/edit -> list)
                targetState is BeverageDestination.List -> {
                    (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                        slideInHorizontally(
                            initialOffsetX = { -it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        )).togetherWith(
                        fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                        slideOutHorizontally(
                            targetOffsetX = { it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        )
                    )
                }
                else -> {
                    fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) togetherWith 
                    fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST))
                }
            }
        }
    ) { destination ->
        when (destination) {
            is BeverageDestination.List -> {
                BeverageListScreen(
                    onNavigateToCreate = {
                        println("[NavGraph] Beverage: Navigate to Create")
                        currentDestination = BeverageDestination.Create
                    },
                    onNavigateToView = { beverageId ->
                        println("[NavGraph] Beverage: Navigate to View: id=$beverageId")
                        currentDestination = BeverageDestination.View(beverageId)
                    },
                    onNavigateToEdit = { beverageId ->
                        println("[NavGraph] Beverage: Navigate to Edit: id=$beverageId")
                        currentDestination = BeverageDestination.Edit(beverageId)
                    }
                )
            }
            
            is BeverageDestination.Create -> {
                BeverageEditScreen(
                    beverageId = null,
                    onNavigateBack = {
                        println("[NavGraph] Beverage: Navigate back from Create to List")
                        currentDestination = BeverageDestination.List
                    },
                    onSaveSuccess = {
                        println("[NavGraph] Beverage: Create success - navigating to List")
                        currentDestination = BeverageDestination.List
                    }
                )
            }
            
            is BeverageDestination.View -> {
                BeverageViewScreen(
                    beverageId = destination.beverageId,
                    onNavigateBack = {
                        println("[NavGraph] Beverage: Navigate back from View to List")
                        currentDestination = BeverageDestination.List
                    },
                    onNavigateToEdit = { beverageId ->
                        println("[NavGraph] Beverage: Navigate from View to Edit: id=$beverageId")
                        currentDestination = BeverageDestination.Edit(beverageId)
                    }
                )
            }
            
            is BeverageDestination.Edit -> {
                BeverageEditScreen(
                    beverageId = destination.beverageId,
                    onNavigateBack = {
                        println("[NavGraph] Beverage: Navigate back from Edit to List")
                        currentDestination = BeverageDestination.List
                    },
                    onSaveSuccess = {
                        println("[NavGraph] Beverage: Edit success - navigating to List")
                        currentDestination = BeverageDestination.List
                    }
                )
            }
        }
    }
}

/**
 * Navigation host for POS module with internal navigation state
 */
@Composable
fun PosNavigationHost() {
    var currentDestination by remember { mutableStateOf<PosDestination>(PosDestination.List) }
    val viewModel = remember { PosViewModel() }
    
    println("[NavGraph] PosNavigationHost: destination=${currentDestination::class.simpleName}")

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            when {
                // Going deeper (list -> view/create/edit)
                targetState is PosDestination.Create ||
                targetState is PosDestination.View ||
                targetState is PosDestination.Edit -> {
                    (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                        slideInHorizontally(
                            initialOffsetX = { it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        )).togetherWith(
                        fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        )
                    )
                }
                // Going back (view/create/edit -> list)
                targetState is PosDestination.List -> {
                    (fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) +
                        slideInHorizontally(
                            initialOffsetX = { -it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_NORMAL, easing = AppAnimations.EaseOutQuart)
                        )).togetherWith(
                        fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST)) +
                        slideOutHorizontally(
                            targetOffsetX = { it / 4 },
                            animationSpec = tween(AppAnimations.DURATION_FAST)
                        )
                    )
                }
                else -> {
                    fadeIn(animationSpec = tween(AppAnimations.DURATION_NORMAL)) togetherWith 
                    fadeOut(animationSpec = tween(AppAnimations.DURATION_FAST))
                }
            }
        }
    ) { destination ->
        when (destination) {
            is PosDestination.List -> {
                PosOrderListScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = {
                        println("[NavGraph] POS: Navigate to Create")
                        currentDestination = PosDestination.Create
                    },
                    onNavigateToDetail = { orderId ->
                        println("[NavGraph] POS: Navigate to View: id=$orderId")
                        currentDestination = PosDestination.View(orderId)
                    },
                    onNavigateToEdit = { orderId ->
                        println("[NavGraph] POS: Navigate to Edit: id=$orderId")
                        currentDestination = PosDestination.Edit(orderId)
                    }
                )
            }
            
            is PosDestination.Create -> {
                PosOrderEditorScreen(
                    orderId = null,
                    viewModel = viewModel,
                    onNavigateBack = {
                        println("[NavGraph] POS: Navigate back from Create to List")
                        currentDestination = PosDestination.List
                    }
                )
            }
            
            is PosDestination.View -> {
                PosOrderDetailScreen(
                    orderId = destination.orderId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        println("[NavGraph] POS: Navigate back from View to List")
                        currentDestination = PosDestination.List
                    },
                    onNavigateToEdit = { orderId ->
                        println("[NavGraph] POS: Navigate from View to Edit: id=$orderId")
                        currentDestination = PosDestination.Edit(orderId)
                    }
                )
            }
            
            is PosDestination.Edit -> {
                PosOrderEditorScreen(
                    orderId = destination.orderId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        println("[NavGraph] POS: Navigate back from Edit to List")
                        currentDestination = PosDestination.List
                    }
                )
            }
        }
    }
}

/**
 * Report navigation destinations
 */
sealed class ReportDestination {
    object List : ReportDestination()
    data class Detail(val orderId: String) : ReportDestination()
}

/**
 * Report navigation host - handles internal navigation between report screens
 */
@Composable
fun ReportNavigationHost() {
    var currentDestination by remember { mutableStateOf<ReportDestination>(ReportDestination.List) }
    
    when (val destination = currentDestination) {
        is ReportDestination.List -> {
            ui.screens.report.PosReportScreen(
                onNavigateToDetail = { orderId ->
                    println("[NavGraph] Report: Navigate from List to Detail: orderId=$orderId")
                    currentDestination = ReportDestination.Detail(orderId)
                }
            )
        }
        
        is ReportDestination.Detail -> {
            ui.screens.report.PosOrderDetailScreen(
                orderId = destination.orderId,
                onNavigateBack = {
                    println("[NavGraph] Report: Navigate back from Detail to List")
                    currentDestination = ReportDestination.List
                }
            )
        }
    }
}
