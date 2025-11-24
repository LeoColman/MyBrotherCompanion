package printer

import java.io.File
import java.nio.charset.StandardCharsets

interface CommandExecutor {
  fun execute(args: List<String>, stdoutFile: File? = null, inputBytes: ByteArray? = null): CmdResult
}

class DefaultCommandExecutor : CommandExecutor {
  override fun execute(args: List<String>, stdoutFile: File?, inputBytes: ByteArray?): CmdResult {
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
        CmdResult(true, null)
      } else {
        val err = process.errorStream.readBytes().toString(StandardCharsets.UTF_8)
        CmdResult(false, err.ifBlank { "exit code $exit" })
      }
    } catch (e: Exception) {
      CmdResult(false, e.message)
    }
  }
}