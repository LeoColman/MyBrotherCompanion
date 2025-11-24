package printer

import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.Locale

class DatetimePrinter : BasePrinter() {

    override fun print(): CmdResult {
        val now = ZonedDateTime.now()
        val dayOfWeek = now.format(ofPattern("E", Locale.of("pt", "BR"))).substring(0, 3).lowercase()
        val dateTime = now.format(ofPattern("dd/MM HH:mm"))
        val text = "$dayOfWeek $dateTime"

        val pngFile = newPngTempFile("label_br_datetime_", ".png")
        val binFile = newBinTempFile("label_br_datetime_", ".bin")

        try {
            runConvert(text, pngFile)
            runBrotherQlCreate(pngFile, binFile)
            return runLp(binFile)
        } finally {
            runCatching { pngFile.delete() }
            runCatching { binFile.delete() }
        }
    }

    private fun runBrotherQlCreate(pngFile: File, binFile: File): CmdResult {
        val brotherArgs = listOf(
            "brother_ql_create",
            "--model", model,
            pngFile.absolutePath,
            "--label-size", labelSize
        )
        return executor.execute(brotherArgs, stdoutFile = binFile).let { if (it.success) it else throw IllegalStateException("Calling brotherQlCreate didn't work") }
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
            "label:$text",
            pngFile.absolutePath
        )

        return executor.execute(convertArgs).let { if(it.success) it else throw IllegalStateException("Calling convert didn't work") }
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
