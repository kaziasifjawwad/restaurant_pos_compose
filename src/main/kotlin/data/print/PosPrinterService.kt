package data.print

import data.model.BeverageOrder
import data.model.FoodOrder
import data.model.FoodOrderByCustomer
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.text.DecimalFormat
import javax.print.PrintService
import javax.print.PrintServiceLookup

object PosPrinterService {
    private const val LINE_HEIGHT = 14
    private const val LEFT_PADDING = 18
    private const val MAX_CHARS = 42
    private val amountFormat = DecimalFormat("#,##0.00")

    fun printBill(order: FoodOrderByCustomer, printerModelName: String): Result<Unit> = printText(
        printerModelName = printerModelName,
        title = "POS Bill #${order.id}",
        lines = buildBillLines(order)
    )

    fun printKitchenMemo(order: FoodOrderByCustomer, printerModelName: String): Result<Unit> = printText(
        printerModelName = printerModelName,
        title = "Kitchen Memo #${order.id}",
        lines = buildKitchenMemoLines(order)
    )

    private fun printText(printerModelName: String, title: String, lines: List<String>): Result<Unit> = try {
        val printService = findPrinter(printerModelName)
            ?: return Result.failure(IllegalStateException("Printer not found: $printerModelName"))
        val job = PrinterJob.getPrinterJob()
        job.printService = printService
        job.jobName = title
        job.setPrintable(TextReceiptPrintable(lines))
        job.print()
        Result.success(Unit)
    } catch (ex: PrinterException) {
        Result.failure(ex)
    } catch (ex: Exception) {
        Result.failure(ex)
    }

    private fun findPrinter(printerModelName: String): PrintService? {
        val target = printerModelName.trim()
        return PrintServiceLookup.lookupPrintServices(null, null)
            .firstOrNull { it.name.equals(target, ignoreCase = true) }
            ?: PrintServiceLookup.lookupPrintServices(null, null)
                .firstOrNull { it.name.contains(target, ignoreCase = true) }
    }

    private fun buildBillLines(order: FoodOrderByCustomer): List<String> = buildList {
        header(order, "CUSTOMER BILL")
        add("Items")
        add(separator())
        order.foodOrders.forEach { addFoodLine(it) }
        order.beverageOrders.forEach { addBeverageLine(it) }
        add(separator())
        add(pair("Total", money(order.totalAmount)))
        add(pair("Discount", money(order.discount)))
        add(pair("Status", order.orderStatus.displayName))
        add(pair("Payment", order.paymentMethod?.displayName ?: "N/A"))
        add(separator())
        add(center("Thank you"))
    }

    private fun buildKitchenMemoLines(order: FoodOrderByCustomer): List<String> = buildList {
        header(order, "KITCHEN MEMO")
        add("Food")
        add(separator())
        if (order.foodOrders.isEmpty()) add("No food items")
        order.foodOrders.forEach { food ->
            add("${food.foodQuantity} x ${safe(food.foodName, "Food #${food.itemNumber}")}")
            add("  Size: ${food.foodSize.name}")
        }
        if (order.beverageOrders.isNotEmpty()) {
            add(separator())
            add("Beverage")
            order.beverageOrders.forEach { beverage ->
                add("${beverage.amount} x ${safe(beverage.beverageName, "Beverage #${beverage.beverageId}")}")
                add("  ${amountFormat.format(beverage.quantity)} ${beverage.unit?.name.orEmpty()}")
            }
        }
        add(separator())
        add(center("Prepare with care"))
    }

    private fun MutableList<String>.header(order: FoodOrderByCustomer, title: String) {
        add(center("Restaurant POS"))
        add(center(title))
        add(separator())
        add(pair("Order", "#${order.id}"))
        add(pair("Table", order.tableNumber.toString()))
        add(pair("Waiter", order.waiterName ?: "Unknown"))
        add(separator())
    }

    private fun MutableList<String>.addFoodLine(item: FoodOrder) {
        val name = safe(item.foodName, "Food #${item.itemNumber}")
        add(trimLine("${item.foodQuantity} x $name"))
        add(pair("  ${item.foodSize.name}", money(item.foodPrice * item.foodQuantity)))
    }

    private fun MutableList<String>.addBeverageLine(item: BeverageOrder) {
        val name = safe(item.beverageName, "Beverage #${item.beverageId}")
        add(trimLine("${item.amount} x $name"))
        add(pair("  ${amountFormat.format(item.quantity)} ${item.unit?.name.orEmpty()}", money(item.price * item.amount)))
    }

    private fun pair(left: String, right: String): String {
        val safeLeft = trimLine(left)
        if (safeLeft.length + right.length >= MAX_CHARS) return "$safeLeft $right"
        val spaces = " ".repeat(MAX_CHARS - safeLeft.length - right.length)
        return safeLeft + spaces + right
    }

    private fun center(text: String): String {
        if (text.length >= MAX_CHARS) return trimLine(text)
        val padding = (MAX_CHARS - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun money(amount: Double): String = "Tk ${amountFormat.format(amount)}"
    private fun separator(): String = "-".repeat(MAX_CHARS)
    private fun safe(value: String?, fallback: String): String = value?.takeIf { it.isNotBlank() } ?: fallback
    private fun trimLine(value: String): String = if (value.length <= MAX_CHARS) value else value.take(MAX_CHARS - 3) + "..."

    private class TextReceiptPrintable(private val lines: List<String>) : Printable {
        override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE
            val g2d = graphics as Graphics2D
            g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
            g2d.font = Font(Font.MONOSPACED, Font.PLAIN, 10)
            var y = 20
            lines.forEach { line ->
                g2d.drawString(line, LEFT_PADDING, y)
                y += LINE_HEIGHT
            }
            return Printable.PAGE_EXISTS
        }
    }
}
