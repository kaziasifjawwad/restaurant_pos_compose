package ui.screens.takeout.editor

import data.model.QuantityUnit

data class TakeoutBeverageEntry(
    val beverageId: Long,
    val beverageName: String,
    val quantity: Double,
    val amount: Int,
    val unit: QuantityUnit,
    val price: Double,
    val packagingInstruction: String? = null
) {
    val lineTotal: Double get() = price * amount
}
