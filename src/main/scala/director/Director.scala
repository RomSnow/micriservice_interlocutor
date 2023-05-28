package director

import cats.effect.IO
import config.Configuration.ConfigInstance
import generator.InfoGenerator

import scala.concurrent.duration.DurationInt
import scala.util.Random

trait Director {
    protected val infoGenerator: InfoGenerator
    protected val client: Client
    protected val config: ConfigInstance

    protected val scripts: Seq[() => IO[Unit]] = List(
        () => startP2PScript(),
        () => startRedirectScript(),
        () => startBroadcastScript()
    )
    def startP2PScript(): IO[Unit]
    def startBroadcastScript(): IO[Unit]
    def startRedirectScript(): IO[Unit]

    protected def getScript(): IO[() => IO[Unit]] = IO{
        val seed = Random.between(1, 101)
        println("seed " + seed)
        seed match {
            case s if s <= 60 => scripts(0)
            case s if (60 < s) && (s <= 80) => scripts(1)
            case _ => scripts(2)
        }
    }

    def startRandomWorks(): IO[Unit] = {
        for {
            script <- getScript()
            _      <- script()
            _      <- IO.sleep(Random.between(500, config.generationInfo.sendDurationSec * 1000).milliseconds)
        } yield ()
    }.handleErrorWith(e => IO(println("DIRECTOR ERROR " + e.getMessage))).foreverM
}
