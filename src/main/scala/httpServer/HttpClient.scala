package httpServer

import cats.effect.{ContextShift, IO}
import config.Configuration.ConfigInstance
import director.Client
import generator.GenerationInfo
import org.http4s.Method.POST
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Request, Uri}

import scala.concurrent.ExecutionContext.global

class HttpClient(config: ConfigInstance)(implicit val context: ContextShift[IO]) extends Client {
    private val httpClient = BlazeClientBuilder[IO](global).resource

    private def makeRequest(url: String, info: GenerationInfo): IO[String] = {
        val uri = Uri.fromString(url).toOption.get
        val request = Request[IO](uri = uri, method = POST)
            .withEntity(info.source)
        httpClient.use(client => client.expect[String](request))
    }

    private def getUrl(info: GenerationInfo, path: String) =
        s"http://${config.interlocutorsInfo.name}_${info.destination}:80" + path
    override def makeRequest(genInfo: GenerationInfo, path: String): IO[String] = {
        val url = getUrl(genInfo, path)
        println(url)
        makeRequest(url, genInfo)
    }
}
