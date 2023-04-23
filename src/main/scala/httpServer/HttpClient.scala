package httpServer

import cats.effect.IO
import config.Configuration.ConfigInstance
import generator.GenerationInfo
import httpServer.HttpServer.cs
import org.http4s.Method.POST
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Request, Uri}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.global

class HttpClient(config: ConfigInstance) {
    private val httpClient = BlazeClientBuilder[IO](global).resource

    private def makeRequest(url: String, info: GenerationInfo): IO[String] = {
        val uri = Uri.fromString(url).toOption.get
        val request = Request[IO](uri = uri, method = POST)
            .withEntity(info.source.mkString)
        httpClient.use(client => client.expect[String](request))
    }

    private def getUrl(info: GenerationInfo) =
        s"http://${config.interlocutorsInfo.name}_${info.destination}:80/message/${info.id}/time/${LocalDateTime.now}"

    def sendRequest(info: GenerationInfo): IO[String] = {
        val url = getUrl(info)
        println(url)
        makeRequest(url, info)
    }

}
