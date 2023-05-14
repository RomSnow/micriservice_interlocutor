package director

import cats.effect.IO
import cats.implicits.catsSyntaxParallelSequence1
import config.Configuration.ConfigInstance
import generator.InfoGenerator
import statSaver.StatisticSaver

import java.time.LocalDateTime

class KafkaDirector(
                       val infoGenerator: InfoGenerator,
                       val client: Client,
                       val config: ConfigInstance,
                       saver: StatisticSaver
                   ) extends Director {

    override def startP2PScript(): IO[Unit] = {
        val info = infoGenerator.genInfo()
        val path = "p2p"
        for {
            saveSend <- saver.saveResultInFile(info.id, "sendP2P", info.destination, System.currentTimeMillis, info.source, List(info.key)).start
            _ <- client.makeRequest(info, path)
            _ <- saveSend.join
        } yield ()
    }

    override def startBroadcastScript(): IO[Unit] = {
        val info = infoGenerator.genInfo()
        val path = "broadcast"
        for {
            saveSend <- saver.saveResultInFile(info.id, "sendBroadcast", 0, System.currentTimeMillis, info.source, List(info.key)).start
            _ <- client.makeRequest(info, path)
            _ <- saveSend.join
        } yield ()
    }

    override def startRedirectScript(): IO[Unit] = {
        val redirectPath = infoGenerator.getRedirectPath()
        val info = infoGenerator.genInfo().copy(destination = redirectPath.head)
        val infoWithRedirList = info.copy(source = redirectPath.tail.mkString(";") + "\n" + info.source)
        val path = "redirect"
        for {
            save <- saver.saveResultInFile(info.id, "startRedirect", info.destination, System.currentTimeMillis,
                info.source, List(info.key, redirectPath.mkString(";"))).start
            _ <- client.makeRequest(infoWithRedirList, path)
            _ <- save.join
        } yield ()
    }

}
