package printer

import java.io.File
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory

interface CommandExecutor {
  fun execute(args: List<String>, stdoutFile: File? = null, inputBytes: ByteArray? = null): Result<Unit>
}

class DefaultCommandExecutor : CommandExecutor {
  private val logger = LoggerFactory.getLogger(DefaultCommandExecutor::class.java)
  override fun execute(args: List<String>, stdoutFile: File?, inputBytes: ByteArray?): Result<Unit> {
    return try {
      val pb = ProcessBuilder(args)
      if (stdoutFile != null) pb.redirectOutput(stdoutFile)
      val process = pb.start()
      if (inputBytes != null) {
        process.outputStream.use { it.write(inputBytes) }
      } else {
        process.outputStream.close()
      }
      val exit = process.waitFor()
      if (exit == 0) {
        logger.info("Command succeeded: {}", args.firstOrNull() ?: "<unknown>")
        Result.success(Unit)
      } else {
        val err = process.errorStream.readBytes().toString(StandardCharsets.UTF_8)
        val msg = err.ifBlank { "exit code $exit" }
        Result.failure(RuntimeException(msg))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}