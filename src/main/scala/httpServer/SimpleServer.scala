package httpServer

import cats.effect._
import config.Configuration.ConfigInstance
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext.global

object SimpleServer {
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)

    lazy val testRoutes: HttpRoutes[IO] =
        HttpRoutes.of[IO] {
            case GET -> Root / "test" / msg => {
                println(msg)
                Ok("Hello")
            }
        }

    def run(config: ConfigInstance): IO[ExitCode] = {
        val router = Router("/" -> testRoutes).orNotFound
        BlazeServerBuilder[IO](global)
            .bindHttp(config.serverNetwork.port, config.serverNetwork.host)
            .withHttpApp(router)
            .resource.use(_ => IO.never)
    }
}
