package printer

import java.io.File
import kotlin.io.path.createTempFile

/**
 * Encapsulates system command invocations used by printers.
 *
 * This is a concrete utility; no interface is used because there's only a single implementation.
 */
class SystemCalls(
  private val executor: CommandExecutor = DefaultCommandExecutor(),
) {
  /**
   * Creates a temporary PNG file for intermediate label rendering.
   */
  fun newPngTempFile(): File = createTempFile("label_", ".png").toFile()

  /**
   * Creates a temporary BIN file for the printer binary output.
   */
  fun newBinTempFile(): File = createTempFile("label_", ".bin").toFile()

  /**
   * Runs the `lp` command targeting the given queue with raw option, sending the given BIN file.
   */
  fun runLp(queue: String, binFile: File): Result<Unit> {
    val lpArgs = listOf(
      "lp",
      "-d", queue,
      "-o", "raw",
      binFile.absolutePath,
    )
    return executor.execute(lpArgs)
  }

  /**
   * Runs ImageMagick `convert` to render text into a PNG file.
   * The [textArg] should already include the proper prefix, e.g., "label:" or "caption:".
   */
  fun runConvert(textArg: String, pngFile: File, size: String, pointSize: String): Result<Unit> {
    val convertArgs = listOf(
      "convert",
      "-size", size,
      "-gravity", "center",
      "-pointsize", pointSize,
      "-background", "white",
      "-fill", "black",
      // Use a font that includes checkbox glyphs (U+2610/U+2611) and allow fontconfig fallback
      // to Noto Sans (letters) and Noto Color Emoji (emojis). Noto Sans Symbols 2 has the
      // ballot box characters which ensures the "☐" renders correctly.
      "-font", "Symbola",
      // Ensure consistent non-alpha, truecolor output compatible with downstream tools
      "-alpha", "off",
      "-type", "TrueColor",
      "-depth", "8",
      // Force PNG writer to emit Truecolor
      "-define", "png:color-type=2",
      // Encourage grayscale-friendly output for thermal printers
      "-colorspace", "Gray",
      // Performance optimizations: strip metadata and use faster PNG compression
      "-strip",
      "-define", "png:compression-level=1",
      textArg,
      pngFile.absolutePath,
    )

    return executor.execute(convertArgs)
  }

  /**
   * Runs `brother_ql_create` to convert the PNG into the printer's binary format.
   * The output is redirected to [binFile] which is later sent to `lp`.
   */
  fun runBrotherQlCreate(model: String, labelSize: String, pngFile: File, binFile: File): Result<Unit> {
    val brotherArgs = listOf(
      "brother_ql_create",
      "--model", model,
      pngFile.absolutePath,
      "--label-size", labelSize,
    )

    return executor.execute(brotherArgs, stdoutFile = binFile)
  }
}
