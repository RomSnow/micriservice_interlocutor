package director

import cats.effect.IO
import config.Configuration.ConfigInstance
import generator.InfoGenerator
import httpServer.HttpClient

import java.time.LocalDateTime

class HTTPDirector(
    val infoGenerator: InfoGenerator,
    val client: HttpClient,
    val config: ConfigInstance
) extends Director {

    override def startP2PScript(): IO[String] = {
        val info = infoGenerator.genInfo()
        val path = s"/message/${info.id}/time/${LocalDateTime.now}"
        client.makeRequest(info, path)
    }
}
