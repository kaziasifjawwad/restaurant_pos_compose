package ui.screens.takeout.editor

import data.model.TakeoutBeverageOrderRequest
import data.model.TakeoutFoodOrderRequest

fun List<TakeoutFoodEntry>.toFoodRequests(): List<TakeoutFoodOrderRequest> = map {
    TakeoutFoodOrderRequest(
        itemNumber = it.itemNumber,
        foodSize = it.requestedFoodSize,
        foodQuantity = it.quantity,
        packagingInstruction = it.packagingInstruction
    )
}

fun List<TakeoutBeverageEntry>.toBeverageRequests(): List<TakeoutBeverageOrderRequest> = map {
    TakeoutBeverageOrderRequest(
        beverageId = it.beverageId,
        quantity = it.quantity,
        unit = it.unit,
        amount = it.amount,
        packagingInstruction = it.packagingInstruction
    )
}
