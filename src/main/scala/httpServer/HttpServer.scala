package httpServer

import cats.effect._
import cipher.XORCipher
import config.Configuration.ConfigInstance
import director.{Client, Server}
import generator.GenerationInfo
import httpServer.utils.LocalDateVar
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import statSaver.StatisticSaver

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class HttpServer(config: ConfigInstance, client: Client, saver: StatisticSaver)
                (implicit val executionContext: ExecutionContext) extends Server {
    lazy val routes =
        HttpRoutes.of[IO] {

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

            case req @ POST -> Root / "redirect" / UUIDVar(id) / "from" / IntVar(host) / "key" / key => {
                def saveReturn(content: String): IO[Response[IO]] = {
                    saver.saveResultInFile(id, "redirectReturn", config.interlocutorsInfo.selfNumber,
                        System.currentTimeMillis, XORCipher.encryptOrDecrypt(content, key)) *>
                        Ok("")
                }

                def redirectToNext(nextNumber: Int, redirectPath: String, content: String) = {
                    val cryptContent = XORCipher.encryptOrDecrypt(content, key)
                    val info = GenerationInfo(id, nextNumber, cryptContent, key)
                    val infoWithRedirList = info.copy(source = redirectPath + "\n" + info.source)
                    for {
                        _ <- saver.saveResultInFile(id, "redirectNext",
                            nextNumber, System.currentTimeMillis, info.source, List(redirectPath))
                        _ <- client.makeRequest(infoWithRedirList, "redirect")
                        result <- Ok("")
                    } yield result
                }

                for {
                    body <- req.as[String]
                    nextStr = body.takeWhile(_ != '\n')
                    nextNumber = nextStr.split(';').headOption.flatMap(_.toIntOption)
                    content = body.dropWhile(_ != '\n').drop(1)
                    result <- nextNumber.fold(saveReturn(content)) { redirNext =>
                        redirectToNext(redirNext, nextStr.split(';').tail.mkString(";"), content)
                    }
                } yield result
            }
        }
    override def run(): IO[ExitCode] = {
        val router = Router("/" -> routes).orNotFound
        BlazeServerBuilder[IO](executionContext)
            .bindHttp(config.serverNetwork.port, config.serverNetwork.host)
            .withHttpApp(router)
            .resource.use(_ => IO.never)
    }
}
