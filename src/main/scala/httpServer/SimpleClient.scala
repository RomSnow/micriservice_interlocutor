package httpServer

import cats.effect.IO
import cats.implicits._
import config.Configuration.ConfigInstance
import httpServer.SimpleServer.{cs, timer}
import org.http4s.Method.GET
import org.http4s.{Request, Uri}
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

object SimpleClient {
    private val httpClient = BlazeClientBuilder[IO](global).resource

    private def makeRequest(url: String): IO[String] = {
        val uri = Uri.fromString(url).toOption.get
        val request = Request[IO](uri = uri, method = GET)
        httpClient.use(client => client.expect[String](request))
    }

    private def generateUrl(config: ConfigInstance) = IO{
        val recipientNumber = Random.between(1, config.interlocutorsInfo.count + 1)
        val portNumber = 5000 + recipientNumber
        val str = Iterator.continually(Random.nextPrintableChar()).filter(_.isLetterOrDigit).take(6).mkString
        s"http://${config.interlocutorsInfo.name}_$recipientNumber:80/test/$str"
    }

    def start(config: ConfigInstance) = {
        for {
            url <- generateUrl(config)
            _ <- IO(println(url))
            _ <- makeRequest(url)
                .handleErrorWith(err => IO(println(err.getMessage)))
            _ <- IO.sleep(3.seconds)
        } yield ()
    }.foreverM

}
