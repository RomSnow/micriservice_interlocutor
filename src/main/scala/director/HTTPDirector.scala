package director

import cats.effect.{ContextShift, IO, Timer}
import config.Configuration.ConfigInstance
import generator.InfoGenerator
import httpServer.HttpClient
import statSaver.StatisticSaver

import java.time.LocalDateTime

class HTTPDirector(
    val infoGenerator: InfoGenerator,
    val client: HttpClient,
    val config: ConfigInstance,
    saver: StatisticSaver
)(implicit val timer: Timer[IO], context: ContextShift[IO]) extends Director {

    override def startP2PScript(): IO[Unit] = {
        val info = infoGenerator.genInfo()
        val path = s"/p2p/${info.id}/key/${info.key}"
        for {
            saveSend <- saver.saveResultInFile(info.id, "sendP2P", info.destination, LocalDateTime.now, info.source, List(info.key)).start
            result <- client.makeRequest(info, path)
            saveResult <- saver.saveResultInFile(info.id, "responseP2P", info.destination, LocalDateTime.now, result).start
            _ <- saveSend.join
            _ <- saveResult.join
        } yield ()
    }
}
