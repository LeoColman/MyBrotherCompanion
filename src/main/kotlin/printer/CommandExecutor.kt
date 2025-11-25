package printer

import java.io.File
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory

interface CommandExecutor {
  fun execute(args: List<String>, stdoutFile: File? = null, inputBytes: ByteArray? = null): CmdResult
}

class DefaultCommandExecutor : CommandExecutor {
  private val logger = LoggerFactory.getLogger(DefaultCommandExecutor::class.java)
  override fun execute(args: List<String>, stdoutFile: File?, inputBytes: ByteArray?): CmdResult {
    logger.info("Executing command: {}{}{}", args.joinToString(" "),
      if (stdoutFile != null) " | stdout->${stdoutFile.absolutePath}" else "",
      if (inputBytes != null) " | with-input-bytes(${inputBytes.size})" else ""
    )
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
        CmdResult(true, null)
      } else {
        val err = process.errorStream.readBytes().toString(StandardCharsets.UTF_8)
        val msg = err.ifBlank { "exit code $exit" }
        logger.error("Command failed (exit {}): {} | error: {}", exit, args.joinToString(" "), msg)
        CmdResult(false, msg)
      }
    } catch (e: Exception) {
      logger.error("Command execution threw exception for {}: {}", args.joinToString(" "), e.toString())
      CmdResult(false, e.message)
    }
  }
}