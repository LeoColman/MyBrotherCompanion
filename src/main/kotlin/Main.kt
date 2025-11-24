import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.statuspages.*
import printer.DatetimePrinter
import printer.ProverbPrinter
import java.nio.charset.StandardCharsets

private const val HOST = "0.0.0.0"
private const val PORT = 8088

fun main() {
  embeddedServer(Netty, port = PORT, host = HOST) {
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
        call.respondText("OK\n", ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8))
      }

      // GET /print-datetime-br -> print datetime using native pipeline
      get("/print-datetime-br") {
        val result = DatetimePrinter().print()
        if (result.success) {
          call.respondText(
            "OK\n",
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.OK
          )
        } else {
          val msg = "Print failed: ${result.errorMessage}\n"
          call.respondText(
            msg,
            ContentType.Text.Plain.withCharset(StandardCharsets.UTF_8),
            HttpStatusCode.InternalServerError
          )
        }
      }

      // GET /print-proverb-pt -> pick a proverb and print it using native pipeline
      get("/print-proverb-pt") {
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

