package httpServer.utils

import java.time.LocalDateTime
import scala.util.Try

object LocalDateVar {
    def unapply(string: String): Option[LocalDateTime] = {
        Try(LocalDateTime.parse(string)).toOption
    }
}
