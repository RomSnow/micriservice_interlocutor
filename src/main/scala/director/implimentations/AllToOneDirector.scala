package director.implimentations

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.{Client, SpecialModeDirector}
import generator.InfoGenerator
import statSaver.StatisticSaver

class AllToOneDirector(val infoGenerator: InfoGenerator,
                       val client: Client,
                       val config: ConfigInstance,
                       saver: StatisticSaver) extends SpecialModeDirector {
    override def startSpecialScript(): IO[Unit] = {
        val info = infoGenerator.genInfo().copy(destination = config.specialModeConf.specialServerNumber)
        val path = "p2p"
        for {
            saveSend <- saver.saveResultInFile(info.id, "sendP2P", info.destination, System.currentTimeMillis, info.source, List(info.key)).start
            result <- client.makeRequest(info, path)
            saveResult <- saver.saveResultInFile(info.id, "responseP2P", info.destination, System.currentTimeMillis, result).start
            _ <- saveSend.join
            _ <- saveResult.join
        } yield ()
    }

    override def startP2PScript(): IO[Unit] = IO.unit

    override def startBroadcastScript(): IO[Unit] = IO.unit

    override def startRedirectScript(): IO[Unit] = IO.unit
}
