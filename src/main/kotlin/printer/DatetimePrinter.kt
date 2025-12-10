package printer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern as dateTimeFormatter
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
    val dayOfWeek = formattedDayOfWeek(now)
    val dateTime = formattedDateTime(now)
    return "$dayOfWeek $dateTime"
  }

  private fun formattedDayOfWeek(date: ZonedDateTime) = date.format(dayOfWeekFormatter).substring(0, 3).lowercase()
  private fun formattedDateTime(date: ZonedDateTime) = date.format(dateTimeFormatter)

  companion object {
    private val dayOfWeekFormatter = dateTimeFormatter("E", Locale.of("pt", "BR"))
    private val dateTimeFormatter = dateTimeFormatter("dd/MM HH:mm")

  }
}
