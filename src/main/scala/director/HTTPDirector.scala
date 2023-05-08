package director

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits.{catsStdInstancesForList, catsSyntaxParallelSequence}
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

    override def startBroadcastScript(): IO[Unit] = {
        val info = infoGenerator.genInfo()
        val allInterlocutors = (1 to config.interlocutorsInfo.count)
            .filterNot(_ == config.interlocutorsInfo.selfNumber).toList
        val path = s"/p2p/${info.id}/key/${info.key}"
        allInterlocutors.map { dest =>
            for {
                saveSend <- saver.saveResultInFile(info.id, "sendBroadcast", dest, LocalDateTime.now, info.source, List(info.key)).start
                result <- client.makeRequest(info.copy(destination = dest), path)
                saveResult <- saver.saveResultInFile(info.id, "responseBroadcast", dest, LocalDateTime.now, result).start
                _ <- saveSend.join
                _ <- saveResult.join
            } yield ()
        }.parSequence.void
    }

    override def startRedirectScript(): IO[Unit] = {
        val redirectPath = infoGenerator.getRedirectPath()
        val info = infoGenerator.genInfo().copy(destination = redirectPath.head)
        val infoWithRedirList = info.copy(source = redirectPath.tail.mkString(";") + "\n" + info.source)
        val path = s"/redirect/${info.id}/from/${config.interlocutorsInfo.selfNumber}/key/${info.key}"
        for {
            save <- saver.saveResultInFile(info.id, "startRedirect", info.destination, LocalDateTime.now,
                info.source, List(info.key, redirectPath.mkString(";"))).start
            _ <- client.makeRequest(infoWithRedirList, path)
            _ <- save.join
        } yield ()
    }
}
