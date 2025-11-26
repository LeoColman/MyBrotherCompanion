package printer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale

class DatetimePrinter(
  executor: CommandExecutor = DefaultCommandExecutor(),
) : BasePrinter(executor) {

  override fun print(): Result<Unit> {
    val text = buildDatetimeText()
    return withTempFiles { pngFile, binFile ->
      systemCalls.runConvert("label:$text", pngFile, size = "696x300", pointSize = "75").getOrThrow()
      systemCalls.runBrotherQlCreate(model, labelSize, pngFile, binFile).getOrThrow()
      systemCalls.runLp(queue, binFile)
    }
  }

  private fun buildDatetimeText(): String {
    val now = ZonedDateTime.now()
    val dayOfWeek = now.format(ofPattern("E", Locale.of("pt", "BR"))).substring(0, 3).lowercase()
    val dateTime = now.format(ofPattern("dd/MM HH:mm"))
    return "$dayOfWeek $dateTime"
  }
}
