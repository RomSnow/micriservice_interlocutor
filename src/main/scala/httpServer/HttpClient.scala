package httpServer

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.Client
import generator.GenerationInfo
import org.http4s.Method.POST
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.{Request, Uri}

import scala.concurrent.ExecutionContext.global

class HttpClient(config: ConfigInstance) extends Client {
    private val httpClient = BlazeClientBuilder[IO].withExecutionContext(global).resource

    private def makeRequest(url: String, info: GenerationInfo): IO[String] = {
        val uri = Uri.fromString(url).toOption.get
        val request = Request[IO](uri = uri, method = POST)
            .withEntity(info.source)
        httpClient.use(client => client.expect[String](request))
    }

    private def getUrlPath(path: String, info: GenerationInfo) =
        path match {
            case "p2p" => s"/p2p/${info.id}/key/${info.key}"
            case "broadcast" => s"/p2p/${info.id}/key/${info.key}"
            case "redirect" => s"/redirect/${info.id}/from/${config.interlocutorsInfo.selfNumber}/key/${info.key}"
            case _ => s"/p2p/${info.id}/key/${info.key}"
        }

    private def getUrl(info: GenerationInfo, path: String) =
        s"http://${config.interlocutorsInfo.name}-${info.destination}:${config.serverNetwork.port}" + path
    override def makeRequest(genInfo: GenerationInfo, path: String): IO[String] = {
        val urlPath = getUrlPath(path, genInfo)
        val url = getUrl(genInfo, urlPath)
        println(url)
        makeRequest(url, genInfo)
    }
}
