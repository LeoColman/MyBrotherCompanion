package printer

import java.io.File
import kotlin.io.path.createTempFile
import org.slf4j.LoggerFactory

// Parent class extracting common variables used by concrete printers
abstract class BasePrinter(
  protected val model: String = DEFAULT_MODEL,
  protected val labelSize: String = DEFAULT_LABEL_SIZE,
  protected val queue: String = DEFAULT_QUEUE,
  protected val executor: CommandExecutor = DefaultCommandExecutor(),
) : Printer {
  protected val logger = LoggerFactory.getLogger(this::class.java)
  // Helpers to generate fresh temp files for each print operation
  protected fun newPngTempFile(prefix: String = "label_", suffix: String = ".png"): File =
    createTempFile(prefix, suffix).toFile().also { logger.debug("Created PNG temp file: {}", it.absolutePath) }

  protected fun newBinTempFile(prefix: String = "label_", suffix: String = ".bin"): File =
    createTempFile(prefix, suffix).toFile().also { logger.debug("Created BIN temp file: {}", it.absolutePath) }

  companion object {
    const val DEFAULT_MODEL: String = "QL-800"
    const val DEFAULT_LABEL_SIZE: String = "62"
    const val DEFAULT_QUEUE: String = "Koda"
  }
}
