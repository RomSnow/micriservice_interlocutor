package httpServer

import cats.effect._
import config.Configuration.ConfigInstance
import httpServer.utils.LocalDateVar
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import statSaver.StatisticSaver

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.global

object HttpServer {
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)

    lazy val routes: StatisticSaver => HttpRoutes[IO] = (saver: StatisticSaver) =>
        HttpRoutes.of[IO] {
            case GET -> Root / "test" / msg => {
                println(msg)
                Ok("Hello")
            }
            case req @ POST -> Root / "message" / UUIDVar(id) / "time" / LocalDateVar(date) =>
                for {
                    body <- req.as[String]
                    _ <- saver.saveResultInFile(id, date, LocalDateTime.now, body)
                        .handleErrorWith(e => IO(println("Не удалось записать данные в файл " + e.getMessage)))
                    result <- Ok(id.toString)
                } yield result
        }

    def run(config: ConfigInstance, saver: StatisticSaver): IO[ExitCode] = {
        val router = Router("/" -> routes(saver)).orNotFound
        BlazeServerBuilder[IO](global)
            .bindHttp(config.serverNetwork.port, config.serverNetwork.host)
            .withHttpApp(router)
            .resource.use(_ => IO.never)
    }
}
