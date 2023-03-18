package httpServer

import cats._
import cats.effect._
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
            case GET -> Root / "test" => Ok("Hello")
        }

    def run(): IO[ExitCode] = {
        val router = Router("/" -> testRoutes).orNotFound
        BlazeServerBuilder[IO](global)
            .bindHttp(8080, "localhost")
            .withHttpApp(router)
            .resource.use(_ => IO.never)
    }
}
