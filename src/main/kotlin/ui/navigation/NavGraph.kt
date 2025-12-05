package ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import data.model.MenuItem
import ui.screens.*
import ui.theme.AppAnimations

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
 * Menu destinations (codes from API)
 */
sealed class MenuDestination(val menuCode: String) {
    object FoodItem : MenuDestination("FOOD_ITEM")
    // Add other destinations later
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
    
    when (currentMenuCode) {
        MenuDestination.FoodItem.menuCode -> {
            FoodItemNavigationHost()
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
