package printer

import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale
import org.slf4j.LoggerFactory

class DatetimePrinter : BasePrinter() {
  private val log = LoggerFactory.getLogger(DatetimePrinter::class.java)

  override fun print(): CmdResult {
    log.info("Starting datetime print on model={}, labelSize={}, queue={}", model, labelSize, queue)
    val now = ZonedDateTime.now()
    val dayOfWeek = now.format(ofPattern("E", Locale.of("pt", "BR"))).substring(0, 3).lowercase()
    val dateTime = now.format(ofPattern("dd/MM HH:mm"))
    val text = "$dayOfWeek $dateTime"
    log.debug("Formatted datetime text: {}", text)

    val pngFile = newPngTempFile("label_br_datetime_", ".png")
    val binFile = newBinTempFile("label_br_datetime_", ".bin")

    try {
      log.info("Running convert to generate PNG")
      runConvert(text, pngFile)
      log.info("Running brother_ql_create to generate binary")
      runBrotherQlCreate(pngFile, binFile)
      log.info("Sending job to CUPS via lp")
      val res = runLp(binFile)
      if (res.success) log.info("Datetime print completed successfully") else log.error("Datetime print failed: {}", res.errorMessage)
      return res
    } finally {
      runCatching { if (pngFile.exists()) { pngFile.delete(); log.debug("Deleted temp file: {}", pngFile.absolutePath) } }
      runCatching { if (binFile.exists()) { binFile.delete(); log.debug("Deleted temp file: {}", binFile.absolutePath) } }
    }
  }

  private fun runBrotherQlCreate(pngFile: File, binFile: File): CmdResult {
    val brotherArgs = listOf(
      "brother_ql_create",
      "--model", model,
      pngFile.absolutePath,
      "--label-size", labelSize
    )
    return executor.execute(brotherArgs, stdoutFile = binFile)
      .let { if (it.success) it else throw IllegalStateException("Calling brotherQlCreate didn't work") }
  }

  private fun runConvert(text: String, pngFile: File): CmdResult {
    val convertArgs = listOf(
      "convert",
      "-size", "696x300",
      "-gravity", "center",
      "-pointsize", "75",
      "-background", "white",
      "-fill", "black",
      "-font", "DejaVu-Sans",
      "caption:$text",
      pngFile.absolutePath
    )

    return executor.execute(convertArgs)
      .let { if (it.success) it else throw IllegalStateException("Calling convert didn't work") }
  }

  private fun runLp(binFile: File): CmdResult {
    val lpArgs = listOf(
      "lp",
      "-d", queue,
      "-o", "raw",
      binFile.absolutePath
    )
    return executor.execute(lpArgs)
  }
}
