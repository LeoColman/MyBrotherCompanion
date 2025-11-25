package printer

import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale

class DatetimePrinter(
  executor: CommandExecutor = DefaultCommandExecutor(),
) : BasePrinter(executor) {

  override fun print(): Result<Unit> {
    val text = buildDatetimeText()

    val pngFile = newPngTempFile("label_br_datetime_")
    val binFile = newBinTempFile("label_br_datetime_")

    try {
      runConvert(text, pngFile)
      runBrotherQlCreate(pngFile, binFile)
      return runLp(binFile)
    } finally {
      runCatching { if (pngFile.exists()) { pngFile.delete(); logger.debug("Deleted temp file: {}", pngFile.absolutePath) } }
      runCatching { if (binFile.exists()) { binFile.delete(); logger.debug("Deleted temp file: {}", binFile.absolutePath) } }
    }
  }

  private fun buildDatetimeText(): String {
    val now = ZonedDateTime.now()
    val dayOfWeek = now.format(ofPattern("E", Locale.of("pt", "BR"))).substring(0, 3).lowercase()
    val dateTime = now.format(ofPattern("dd/MM HH:mm"))
    return "$dayOfWeek $dateTime"
  }

  private fun runBrotherQlCreate(pngFile: File, binFile: File) {
    val brotherArgs = listOf(
      "brother_ql_create",
      "--model", model,
      pngFile.absolutePath,
      "--label-size", labelSize
    )
    return executor.execute(brotherArgs, binFile).getOrThrow()
  }

  private fun runConvert(text: String, pngFile: File) {
    val convertArgs = listOf(
      "convert",
      "-size", "696x300",
      "-gravity", "center",
      "-pointsize", "75",
      "-background", "white",
      "-fill", "black",
      "-font", "DejaVu-Sans",
      "label:$text",
      pngFile.absolutePath
    )

    return executor.execute(convertArgs).getOrThrow()
  }

  private fun runLp(binFile: File): Result<Unit> {
    val lpArgs = listOf(
      "lp",
      "-d", queue,
      "-o", "raw",
      binFile.absolutePath
    )
    return executor.execute(lpArgs)
  }
}
