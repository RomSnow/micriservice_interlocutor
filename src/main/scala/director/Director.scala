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

    private val scripts: Seq[() => IO[Unit]] = List(() => startP2PScript())
    def startP2PScript(): IO[Unit]

    def startRandomWorks(): IO[Unit] = {
        for {
            number <- IO(Random.between(0, scripts.length))
            script <- IO(scripts(number))
            _      <- script()
            _      <- IO.sleep(config.generationInfo.sendDurationSec.seconds)
        } yield ()
    }.foreverM
}
