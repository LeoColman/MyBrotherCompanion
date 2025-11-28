import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.throwable.shouldHaveMessage
import java.io.File
import java.time.LocalDate
import printer.BasePrinter
import printer.WeeklyHouseRoutinePrinter

class WeeklyHouseRoutinePrinterTest : FunSpec({

  test("buildDailyHouseText builds expected text for a Monday") {
    val printer = WeeklyHouseRoutinePrinter()

    val date = LocalDate.of(2025, 11, 24) // Monday
    val text = printer.buildDailyHouseText(date)

    val expected = listOf(
      "🏠✓ Rotina da casa - Segunda 🏠✓",
      "",
      "[ ] 🍽 Louça",
      "[ ] 🧼 Limpar o banheiro",
      "[ ] ⊙ Rodar robô aspirador",
    ).joinToString("\n")

    text shouldBe expected
  }

  test("convert uses default convert with proper sizing and caption: for multiline text") {
    val (calls, executor) = capturingExecutor()

    WeeklyHouseRoutinePrinter(executor).print().shouldBeSuccess()

    calls.shouldHaveSize(3)
    val convert = calls[0]
    convert.args.first() shouldBe "convert"
    // sizing and style
    convert.args.containsAll(listOf("-size", "696x400", "-gravity", "center", "-pointsize", "40")).shouldBeTrue()
    // caption: content
    val captionArg = convert.args.first { it.startsWith("caption:") }
    captionArg.shouldStartWith("caption:")
    captionArg.contains("Rotina da casa").shouldBe(true)
  }

  test("brother_ql_create uses model, size and reads PNG while writing BIN to stdoutFile") {
    val (calls, executor) = capturingExecutor()

    WeeklyHouseRoutinePrinter(executor).print().shouldBeSuccess()

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

    WeeklyHouseRoutinePrinter(executor).print().shouldBeSuccess()

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

    WeeklyHouseRoutinePrinter(executor).print().shouldBeSuccess()

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

    val printer = WeeklyHouseRoutinePrinter(executor)

    shouldThrow<IllegalArgumentException> {
      printer.print()
    } shouldHaveMessage "simulated failure"

    calls shouldHaveSize 2
    pngPath shouldNotBe null 
    File(pngPath!!).shouldNotExist()
    binPath shouldNotBe null 
    File(binPath!!).shouldNotExist()
  }
})
