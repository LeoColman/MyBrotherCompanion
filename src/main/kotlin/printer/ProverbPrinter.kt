package printer

import kotlin.random.Random

class ProverbPrinter(
  executor: CommandExecutor = DefaultCommandExecutor(),
) : BasePrinter(executor) {

  override fun print(): Result<Unit> {
    // Use non-color Noto fonts only; enforce text (monochrome) presentation
    // to avoid selecting color emoji glyphs.
    val text = enforceTextPresentation(randomProverb())
    val markup = toPangoMarkup(text, pointSize = 40)
    return withTempFiles { pngFile, binFile ->
      systemCalls.runConvertPango(markup, pngFile, size = "696x400").getOrThrow()
      systemCalls.runBrotherQlCreate(model, labelSize, pngFile, binFile).getOrThrow()
      systemCalls.runLp(queue, binFile)
    }
  }

  companion object {
    fun randomProverb(random: Random = Random): String = Proverbs.random(random)

    private val Proverbs: List<String> by lazy {
      ProverbPrinter::class.java.getResourceAsStream("/proverbs.txt")!!.bufferedReader().readLines()
    }
  }

  private fun toPangoMarkup(text: String, pointSize: Int): String {
    // Prefer non-color font to ensure black-only glyphs; fontconfig will fallback
    // to other non-color fonts (e.g., Symbola, Noto Sans) as needed.
    val primary = "Noto Sans Symbols 2"
    val escaped = text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
    return """<span font_desc=\"$primary $pointSize\">$escaped</span>"""
  }

  // Ensure emojis use text (monochrome) presentation and not color glyphs
  private fun enforceTextPresentation(input: String): String {
    val vs16 = '\uFE0F' // emoji presentation
    val vs15 = '\uFE0E' // text presentation
    var s = input.replace(vs16, vs15)
    // Apply broadly to common emoji ranges by appending VS15 when neither VS15 nor VS16 present.
    // This is a conservative approach and safe for plain text.
    val emojiRegex = Regex("[\u231A-\u231B\u23E9-\u23F3\u23F8-\u23FA\u2600-\u27BF\u1F300-\u1F6FF\u1F900-\u1F9FF\u1FA70-\u1FAFF]")
    s = s.replace(emojiRegex) { it.value + vs15 }
    return s
  }
}
