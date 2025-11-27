package printer

import java.time.DayOfWeek
import java.time.LocalDate

class WeeklyHouseRoutinePrinter(
  executor: CommandExecutor = DefaultCommandExecutor(),
) : BasePrinter(executor) {

  override fun print(): Result<Unit> =
    printForDate(LocalDate.now())

  /**
   * Imprime a rotina diária da casa para a data fornecida.
   */
  fun printForDate(date: LocalDate): Result<Unit> {
    val text = buildDailyHouseText(date)
    val markup = toPangoMarkup(text, pointSize = 32)
    return withTempFiles { pngFile, binFile ->
      systemCalls.runConvertPango(markup, pngFile, size = "696x300").getOrThrow()
      systemCalls.runBrotherQlCreate(model, labelSize, pngFile, binFile).getOrThrow()
      systemCalls.runLp(queue, binFile)
    }
  }

  internal fun buildDailyHouseText(date: LocalDate = LocalDate.now()): String {
    val day = date.dayOfWeek
    val title = dayTitle(day)
    val items = tasksByDay[day].orEmpty()
    val checklist = items.joinToString(separator = "\n") { "[ ] $it" }

    return buildString {
      appendLine(title)
      appendLine()
      append(checklist)
    }
  }

  // Título simples, uma linha só, sem emojis
  private fun dayTitle(day: DayOfWeek): String =
    when (day) {
      DayOfWeek.MONDAY    -> "🏠✓ Rotina da casa - Segunda 🏠✓"
      DayOfWeek.TUESDAY   -> "🏠✓ Rotina da casa - Terça 🏠✓"  
      DayOfWeek.WEDNESDAY -> "🏠✓ Rotina da casa - Quarta 🏠✓" 
      DayOfWeek.THURSDAY  -> "🏠✓ Rotina da casa - Quinta 🏠✓" 
      DayOfWeek.FRIDAY    -> "🏠✓ Rotina da casa - Sexta 🏠✓"  
      DayOfWeek.SATURDAY  -> "🏠✓ Rotina da casa - Sábado 🏠✓" 
      DayOfWeek.SUNDAY    -> "🏠✓ Rotina da casa - Domingo 🏠✓"
    }

  // Só texto nas tarefas também
  private val tasksByDay: Map<DayOfWeek, List<String>> = mapOf(
    DayOfWeek.MONDAY to listOf(
      "Louça",
      "Limpar o banheiro",
      "Rodar robô aspirador",
    ),
    DayOfWeek.TUESDAY to listOf(
      "Louça",
      "Tirar o lixo",
      "Rodar robô aspirador",
    ),
    DayOfWeek.WEDNESDAY to listOf(
      "Louça",
      "Rodar robô aspirador",
    ),
    DayOfWeek.THURSDAY to listOf(
      "Louça",
      "Tirar o lixo",
      "Rodar robô aspirador",
    ),
    DayOfWeek.FRIDAY to listOf(
      "Louça",
      "Lavar minhas roupas do trabalho",
      "Rodar robô aspirador",
    ),
    DayOfWeek.SATURDAY to listOf(
      "Louça",
      "Rodar robô aspirador",
    ),
    DayOfWeek.SUNDAY to listOf(
      "Louça",
      "Tirar o lixo",
      "Rodar robô aspirador",
    ),
  )

  private fun toPangoMarkup(text: String, pointSize: Int): String {
    val primary = "Symbola"
    val escaped = text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
    return """<span font_desc=\"$primary $pointSize\">$escaped</span>"""
  }
}
