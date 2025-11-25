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
    
  protected fun newPngTempFile(prefix: String = "label_"): File = createTempFile(prefix, ".png").toFile()

  protected fun newBinTempFile(prefix: String = "label_"): File = createTempFile(prefix, ".bin").toFile()

  companion object {
    val DefaultModel: String = "QL-800"
    val DefaultLabelSize: String = "62"
    val DefaultQueue: String = "Koda"
  }
}
