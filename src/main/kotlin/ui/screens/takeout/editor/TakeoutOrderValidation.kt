package ui.screens.takeout.editor

import data.model.TakeoutMediumResponse

fun validateTakeoutOrderFields(
    medium: TakeoutMediumResponse?,
    externalOrderId: String,
    customerPhone: String,
    customerAddress: String,
    riderName: String,
    riderPhone: String
): String? {
    if (medium == null) return "Select an order medium"
    if (medium.requiresExternalOrderId && externalOrderId.isBlank()) return "External order ID is required for ${medium.displayName}"
    if (medium.requiresCustomerPhone && customerPhone.isBlank()) return "Customer phone is required for ${medium.displayName}"
    if (medium.requiresCustomerAddress && customerAddress.isBlank()) return "Customer address is required for ${medium.displayName}"
    if (medium.requiresRiderInfo && riderName.isBlank() && riderPhone.isBlank()) return "Rider name or phone is required for ${medium.displayName}"
    return null
}
