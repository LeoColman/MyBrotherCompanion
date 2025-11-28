package printer

import java.time.DayOfWeek
import java.time.LocalDate

class WeeklyHouseRoutinePrinter(
  executor: CommandExecutor = DefaultCommandExecutor(),
) : BasePrinter(executor) {

  override fun print(): Result<Unit> =
    printForDate(LocalDate.now())

  /**
   * Prints the house's daily routine for the provided date.
   */
  fun printForDate(date: LocalDate): Result<Unit> {
    val text = buildDailyHouseText(date)
    return withTempFiles { pngFile, binFile ->
      // Multiline content: use caption: so ImageMagick does wrapping/multiline layout
      // Keep size and point size aligned with printer tests and legibility
      systemCalls.runConvert("caption:$text", pngFile, size = "696x400", pointSize = "32").getOrThrow()
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

  // Single-line title with emojis, localized to Portuguese (content required by tests)
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

  // Daily tasks per weekday (kept in Portuguese and with emojis to match label expectations)
  private val tasksByDay: Map<DayOfWeek, List<String>> = mapOf(
    DayOfWeek.MONDAY to listOf(
      "🍽 Louça",
      "🧼 Limpar o banheiro",
      "⊙ Rodar robô aspirador",
    ),
    DayOfWeek.TUESDAY to listOf(
      "🍽 Louça",
      "♲ Tirar o lixo",
      "⊙ Rodar robô aspirador",
      "⚘ Regar plantas",
    ),
    DayOfWeek.WEDNESDAY to listOf(
      "🍽 Louça",
      "⊙ Rodar robô aspirador",
    ),
    DayOfWeek.THURSDAY to listOf(
      "🍽 Louça",
      "♲ Tirar o lixo",
      "⊙ Rodar robô aspirador",
    ),
    DayOfWeek.FRIDAY to listOf(
      "🍽 Louça",
      "👔 Lavar minhas roupas do trabalho",
      "⊙ Rodar robô aspirador",
    ),
    DayOfWeek.SATURDAY to listOf(
      "🍽 Louça",
      "⊙ Rodar robô aspirador",
      "⚘ Regar plantas",
    ),
    DayOfWeek.SUNDAY to listOf(
      "🍽 Louça",
      "♲ Tirar o lixo",
      "⊙ Rodar robô aspirador",
    ),
  )
}
