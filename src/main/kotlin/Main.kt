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
import java.nio.charset.StandardCharsets
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
        if (result.success) {
          call.respondText(
            "OK\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.OK
          )
        } else {
          log.error("/print-datetime-br failed: {}", result.errorMessage)
          call.respondText(
            "Print failed: ${result.errorMessage}\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.InternalServerError
          )
        }
      }

      // GET /print-proverb-pt -> pick a proverb and print it using native pipeline
      get("/print-proverb-pt") {
        log.info("Received /print-proverb-pt request")
        val result = ProverbPrinter().print()
        if (result.success) {
          val msg = "Printed proverb\n"
          call.respondText(
            msg,
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.OK
          )
        } else {
          val msg = "Print proverb failed: ${result.errorMessage}\n"
          log.error("/print-proverb-pt failed: {}", result.errorMessage)
          call.respondText(
            msg,
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.InternalServerError
          )
        }
      }
    }
  }.start(wait = true)
}

