import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import java.io.File
import printer.BasePrinter
import printer.CommandExecutor
import printer.DatetimePrinter

class DatetimePrinterTest : FunSpec({
  
  test("convert builds label text with pt-BR short weekday and flags") {
    val (calls, executor) = capturingExecutor()

    val printer = DatetimePrinter(executor)

    printer.print()

    calls.shouldHaveSize(3)
    val convert = calls[0]
    convert.args.first() shouldBe "convert"
    convert.args.containsAll(listOf("-size", "696x300", "-gravity", "center", "-pointsize", "75")).shouldBeTrue()
    val labelArg = convert.args.first { it.startsWith("label:") }
    val labelText = labelArg.removePrefix("label:")
    labelText.shouldMatch(Regex("^[a-z]{3} \\d{2}/\\d{2} \\d{2}:\\d{2}$"))
  }

  test("brother_ql_create uses model, size and reads PNG while writing BIN to stdoutFile") {
    val (calls, executor) = capturingExecutor()

    val printer = DatetimePrinter(executor = executor)
    printer.print().shouldBeSuccess()

    calls.shouldHaveSize(3)
    val pngPath = calls[0].args.last()
    val brother = calls[1]
    brother.args.first() shouldBe "brother_ql_create"
    brother.args.containsAll(listOf("--model", BasePrinter.DefaultModel)).shouldBeTrue()
    brother.args.containsAll(listOf("--label-size", BasePrinter.DefaultLabelSize)).shouldBeTrue()
    brother.args.contains(pngPath).shouldBeTrue()
    (brother.stdoutFile != null).shouldBeTrue()
  }

  test("lp sends job to the correct queue with raw option and same BIN file") {
    val (calls, executor) = capturingExecutor()

    val printer = DatetimePrinter(executor = executor)
    printer.print().shouldBeSuccess()

    calls.shouldHaveSize(3)
    val brother = calls[1]
    val lp = calls[2]
    lp.args.first() shouldBe "lp"
    lp.args.containsAll(listOf("-d", BasePrinter.DefaultQueue, "-o", "raw")).shouldBeTrue()
    val binPathFromLp = lp.args.last()
    binPathFromLp shouldBe brother.stdoutFile!!.absolutePath
  }

  test("cleans temp PNG and BIN files on success") {
    val (calls, executor) = capturingExecutor()

    val printer = DatetimePrinter(executor = executor)
    printer.print().shouldBeSuccess()

    calls.shouldHaveSize(3)
    val pngPath = calls[0].args.last()
    val binPath = calls[2].args.last()
    File(pngPath).exists().shouldBeFalse()
    File(binPath).exists().shouldBeFalse()
  }

  test("cleans temp files when brother_ql_create fails and throws") {
    var pngPath: String? = null
    var binPath: String? = null

    val (calls, executor) = capturingExecutor { args, out ->
      when (args.first()) {
        "convert" -> {
          pngPath = args.last()
          Result.success(Unit)
        }
        "brother_ql_create" -> {
          binPath = out?.absolutePath
          Result.failure(IllegalStateException("simulated failure"))
        }
        else -> Result.success(Unit)
      }
    }

    val printer = DatetimePrinter(executor = executor)

    var thrown: Throwable? = null
    try {
      printer.print()
    } catch (t: Throwable) {
      thrown = t
    }
    (thrown is IllegalStateException).shouldBeTrue()

    // convert and brother_ql_create should be called; lp should NOT be invoked on failure
    calls.shouldHaveSize(2)
    (pngPath != null).shouldBeTrue()
    File(pngPath!!).exists().shouldBeFalse()
    (binPath != null).shouldBeTrue()
    File(binPath!!).exists().shouldBeFalse()
  }
})

data class ExecCall(val args: List<String>, val stdoutFile: File?)

fun capturingExecutor(
  handler: (args: List<String>, stdout: File?) -> Result<Unit> = { _, _ -> Result.success(Unit) },
): Pair<MutableList<ExecCall>, CommandExecutor> {
  val calls = mutableListOf<ExecCall>()
  val executor = mockk<CommandExecutor>()
  every { executor.execute(any(), any(), any()) } answers {
    val args = firstArg<List<String>>()
    val out = secondArg<File?>()
    calls += ExecCall(args.toList(), out)
    handler(args, out)
  }
  return calls to executor
}