package director

import cats.effect.{IO, Timer}
import cats.implicits.catsSyntaxFlatMapOps
import config.Configuration.ConfigInstance
import generator.InfoGenerator

import scala.concurrent.duration.DurationInt
import scala.util.Random

trait Director {
    protected val infoGenerator: InfoGenerator
    protected val client: Client
    protected val config: ConfigInstance
    implicit val timer: Timer[IO]

    private val scripts: Seq[() => IO[Unit]] = List(
        () => startP2PScript(),
        () => startRedirectScript(),
        () => startBroadcastScript()
    )
    def startP2PScript(): IO[Unit]
    def startBroadcastScript(): IO[Unit]
    def startRedirectScript(): IO[Unit]

    private def getScript(): IO[() => IO[Unit]] = {
        val seed = Random.between(1, 101)
        val script = seed match {
            case s if s <= 60 => scripts(0)
            case s if (60 < s) && (s <= 80) => scripts(1)
            case _ => scripts(2)
        }
        IO(script)
    }

    def startRandomWorks(): IO[Unit] = {
        for {
            script <- getScript()
            _      <- script()
            _      <- IO.sleep(config.generationInfo.sendDurationSec.seconds)
        } yield ()
    }.handleErrorWith(e => IO(println("ERROR " + e.getMessage))).foreverM
}
