package generator

import cats.effect.IO
import cats.implicits.catsSyntaxFlatMapOps
import config.Configuration.ConfigInstance
import httpServer.HttpClient
import httpServer.HttpServer.timer

import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.Random

class InfoGenerator(config: ConfigInstance, client: HttpClient) {
    private def id: UUID = UUID.randomUUID()

    @tailrec
    private def getDestination(currentInt: Int): Int =
        if (currentInt == config.interlocutorsInfo.selfNumber)
            getDestination(Random.between(1, config.interlocutorsInfo.count + 1))
        else
            currentInt

    private def destination: Int = getDestination(config.interlocutorsInfo.selfNumber)

    private def generateSource(): Iterator[Char] =
        Iterator.continually(Random.nextPrintableChar()).filter(_.isLetterOrDigit).take(config.generationInfo.infoSize)


    private def getInfo() = GenerationInfo(
        id, destination, generateSource()
    )

    def startMessageGeneration(): IO[Unit] = {
        for {
            info <- IO(getInfo())
            _ <- IO(println(info))
            _ <- client.sendRequest(info)
            _ <- IO.sleep(config.generationInfo.sendDurationSec.seconds)
        } yield ()
    }.foreverM
}

case class GenerationInfo(
    id: UUID,
    destination: Int,
    source: Iterator[Char]
)
