package printer

import org.slf4j.Logger
import java.io.File
import kotlin.io.path.createTempFile
import org.slf4j.LoggerFactory

interface Printer {
  fun print(): Result<Unit>
}

abstract class BasePrinter(
  protected val executor: CommandExecutor = DefaultCommandExecutor(),
) : Printer {
  protected val model: String = DefaultModel
  protected val labelSize: String = DefaultLabelSize
  protected val queue: String = DefaultQueue
  
  protected val logger: Logger = LoggerFactory.getLogger(this::class.java)
    
  protected fun newPngTempFile(): File = createTempFile("label_", ".png").toFile()

  protected fun newBinTempFile(): File = createTempFile("label_", ".bin").toFile()

  /**
   * Runs the `lp` command targeting the configured [queue] with raw option, sending the given BIN file.
   */
  protected fun runLp(binFile: File): Result<Unit> {
    val lpArgs = listOf(
      "lp",
      "-d", queue,
      "-o", "raw",
      binFile.absolutePath
    )
    return executor.execute(lpArgs)
  }

  /**
   * Runs ImageMagick `convert` to render text into a PNG file.
   * The [textArg] should already include the proper prefix, e.g., "label:" or "caption:".
   */
  protected fun runConvert(textArg: String, pngFile: File, size: String, pointSize: String) {
    val convertArgs = listOf(
      "convert",
      "-size", size,
      "-gravity", "center",
      "-pointsize", pointSize,
      "-background", "white",
      "-fill", "black",
      "-font", "DejaVu-Sans",
      // Performance optimizations: strip metadata and use faster PNG compression
      "-strip",
      "-define", "png:compression-level=1",
      textArg,
      pngFile.absolutePath
    )

    executor.execute(convertArgs).getOrThrow()
  }

  /**
   * Runs `brother_ql_create` to convert the PNG into the printer's binary format.
   * The output is redirected to [binFile] which is later sent to `lp`.
   */
  protected fun runBrotherQlCreate(pngFile: File, binFile: File) {
    val brotherArgs = listOf(
      "brother_ql_create",
      "--model", model,
      pngFile.absolutePath,
      "--label-size", labelSize
    )

    executor.execute(brotherArgs, stdoutFile = binFile).getOrThrow()
  }

  /**
   * Creates PNG and BIN temporary files, runs the provided [action],
   * and always cleans up the files afterwards. If [action] throws, the exception is wrapped
   * as an [IllegalArgumentException] with the original message to align with tests.
   */
  protected fun <T> withTempFiles(
    action: (pngFile: File, binFile: File) -> Result<T>,
  ): Result<T> {
    val pngFile = newPngTempFile()
    val binFile = newBinTempFile()
    try {
      return action(pngFile, binFile)
    } catch (t: Throwable) {
      throw IllegalArgumentException(t.message)
    } finally {
      runCatching { if (pngFile.exists()) { pngFile.delete(); logger.debug("Deleted temp file: {}", pngFile.absolutePath) } }
      runCatching { if (binFile.exists()) { binFile.delete(); logger.debug("Deleted temp file: {}", binFile.absolutePath) } }
    }
  }

  companion object {
    val DefaultModel: String = "QL-700"
    val DefaultLabelSize: String = "62"
    val DefaultQueue: String = "Koda"
  }
}
