package httpServer

import cats.effect._
import cipher.XORCipher
import config.Configuration.ConfigInstance
import httpServer.utils.LocalDateVar
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import statSaver.StatisticSaver

import scala.concurrent.ExecutionContext.global

class HttpServer(config: ConfigInstance)(implicit time: Timer[IO], context: ContextShift[IO]) {
    lazy val routes =
        HttpRoutes.of[IO] {
            case GET -> Root / "test" / msg => {
                println(msg)
                Ok("Hello")
            }
            case req @ POST -> Root / "message" / UUIDVar(id) / "time" / LocalDateVar(date) =>
                for {
                    body <- req.as[String]
                    result <- Ok(id.toString)
                } yield result

            case req @ POST -> Root / "p2p" / UUIDVar(id) / "key" / key =>
                for {
                    body <- req.as[String]
                    result <- Ok(XORCipher.encryptOrDecrypt(body, key))
                } yield result
        }

    def run(): IO[ExitCode] = {
        val router = Router("/" -> routes).orNotFound
        BlazeServerBuilder[IO](global)
            .bindHttp(config.serverNetwork.port, config.serverNetwork.host)
            .withHttpApp(router)
            .resource.use(_ => IO.never)
    }
}
