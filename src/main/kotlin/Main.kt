import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.calllogging.*
import printer.DatetimePrinter
import printer.ProverbPrinter
import printer.WeeklyHouseRoutinePrinter
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.slf4j.LoggerFactory

private const val HOST = "0.0.0.0"
private const val PORT = 8088

fun main() {
  val log = LoggerFactory.getLogger("Main")
  log.info("Starting BrotherQL700Companion server on {}:{}", HOST, PORT)
  embeddedServer(Netty, port = PORT, host = HOST) {
    install(CallLogging)
    install(StatusPages) {
      status(HttpStatusCode.NotFound) { call, _ ->
        call.respondText(
          "Not found\n",
          ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
          HttpStatusCode.NotFound
        )
      }
    }
    routing {
      // Health check
      get("/health") {
        log.debug("Handling /health request")
        call.respondText("OK\n", ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8))
      }

      // GET /print-datetime-br -> print datetime using native pipeline
      get("/print-datetime-br") {
        log.info("Received /print-datetime-br request")
        val result = DatetimePrinter().print()
        if (result.isSuccess) {
          call.respondText(
            "OK\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.OK
          )
        } else {
          call.respondText(
            "Print failed: ${result.exceptionOrNull()?.message}\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.InternalServerError
          )
        }
      }

      // GET /print-proverb-pt -> pick a proverb and print it using native pipeline
      get("/print-proverb-pt") {
        log.info("Received /print-proverb-pt request")
        val result = ProverbPrinter().print()
        if (result.isSuccess) {
          val msg = "Printed proverb\n"
          call.respondText(
            msg,
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.OK
          )
        } else {
          call.respondText(
            "Print proverb failed: ${result.exceptionOrNull()?.message}\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.InternalServerError
          )
        }
      }

      // GET /print-weekly-house-routine -> print today's weekly house routine checklist
      get("/print-weekly-house-routine") {
        log.info("Received /print-weekly-house-routine request")
        val dayParam = call.request.queryParameters["day"]
        val printer = WeeklyHouseRoutinePrinter()
        val result = if (dayParam.isNullOrBlank()) {
          printer.print()
        } else {
          val targetDay = parseDayOfWeek(dayParam)
          if (targetDay == null) {
            call.respondText(
              "Invalid 'day' parameter. Use one of: MONDAY..SUNDAY, or numbers 1(Mon)-7(Sun).\n",
              ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
              HttpStatusCode.BadRequest
            )
            return@get
          }
          val date = LocalDate.now().with(TemporalAdjusters.nextOrSame(targetDay))
          printer.printForDate(date)
        }
        if (result.isSuccess) {
          val msg = "Printed weekly house routine\n"
          call.respondText(
            msg,
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.OK
          )
        } else {
          call.respondText(
            "Print weekly house routine failed: ${result.exceptionOrNull()?.message}\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.InternalServerError
          )
        }
      }
    }
  }.start(wait = true)
}

private fun parseDayOfWeek(input: String): DayOfWeek? {
  // Accept names like MONDAY, monday, MonDay, and numbers 1 (Mon) to 7 (Sun)
  val trimmed = input.trim()
  // Try numeric: 1..7 where 1=Monday per ISO
  val num = trimmed.toIntOrNull()
  if (num != null && num in 1..7) {
    return DayOfWeek.of(num)
  }
  return runCatching { DayOfWeek.valueOf(trimmed.uppercase()) }.getOrNull()
}

