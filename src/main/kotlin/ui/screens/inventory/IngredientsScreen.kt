package ui.screens.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import data.network.FoodItemApiService

/**
 * Thin entry-point for the INGREDIENTS menu code.
 * All business-logic is delegated to [InventoryCrudScreen]; this composable
 * only supplies the service lambdas and presentation strings.
 */
@Composable
fun IngredientsScreen() {
    val api = remember { FoodItemApiService() }

    InventoryCrudScreen(
        title    = "Ingredients",
        subtitle = "Manage raw ingredients used in recipes",
        icon     = Icons.Outlined.Grass,
        pageSize = 20,

        onLoadPage = { page, size ->
            val page = api.getIngredientsPaged(page, size)
            val items = page.content.map { InventoryItem(it.id, it.name, it.description) }
            Pair(items, page.totalPages)
        },

        onGetById = { id ->
            val item = api.getIngredientById(id)
            InventoryItem(item.id, item.name, item.description)
        },

        onCreate = { name, desc ->
            val created = api.createIngredient(name, desc)
            InventoryItem(created.id, created.name, created.description)
        },

        onUpdate = { id, name, desc ->
            val updated = api.updateIngredient(id, name, desc)
            InventoryItem(updated.id, updated.name, updated.description)
        },

        onDelete = { id -> api.deleteIngredient(id) }
    )
}
