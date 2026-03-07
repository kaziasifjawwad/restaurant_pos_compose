package ui.screens.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import data.network.FoodItemApiService

/**
 * Thin entry-point for the FOOD_CATEGORY menu code.
 * All business-logic is delegated to [InventoryCrudScreen].
 */
@Composable
fun FoodCategoryScreen() {
    val api = remember { FoodItemApiService() }

    InventoryCrudScreen(
        title    = "Food Categories",
        subtitle = "Manage food classification categories",
        icon     = Icons.Outlined.Category,
        pageSize = 20,

        onLoadPage = { page, size ->
            val page = api.getCategoriesPaged(page, size)
            val items = page.content.map { InventoryItem(it.id, it.name, it.description) }
            Pair(items, page.totalPages)
        },

        onGetById = { id ->
            val item = api.getCategoryById(id)
            InventoryItem(item.id, item.name, item.description)
        },

        onCreate = { name, desc ->
            val created = api.createCategory(name, desc)
            InventoryItem(created.id, created.name, created.description)
        },

        onUpdate = { id, name, desc ->
            val updated = api.updateCategory(id, name, desc)
            InventoryItem(updated.id, updated.name, updated.description)
        },

        onDelete = { id -> api.deleteCategory(id) }
    )
}
