package printer

import org.slf4j.Logger
import java.io.File
import org.slf4j.LoggerFactory

interface Printer {
  fun print(): Result<Unit>
}

abstract class BasePrinter(
  protected val executor: CommandExecutor = DefaultCommandExecutor(),
  protected val systemCalls: SystemCalls = SystemCalls(executor),
) : Printer {
  protected val model: String = DefaultModel
  protected val labelSize: String = DefaultLabelSize
  protected val queue: String = DefaultQueue
  
  protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Creates PNG and BIN temporary files, runs the provided [action],
   * and always cleans up the files afterwards. If [action] throws, the exception is wrapped
   * as an [IllegalArgumentException] with the original message to align with tests.
   */
  protected fun <T> withTempFiles(
    action: (pngFile: File, binFile: File) -> Result<T>,
  ): Result<T> {
    val pngFile = systemCalls.newPngTempFile()
    val binFile = systemCalls.newBinTempFile()
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
    val DefaultModel: String = "QL-800"
    val DefaultLabelSize: String = "62"
    val DefaultQueue: String = "Koda"
  }
}
