package director

import cats.effect.IO
import cats.implicits.catsSyntaxFlatMapOps
import config.Configuration.ConfigInstance
import generator.InfoGenerator
import httpServer.HttpServer.timer

import scala.concurrent.duration.DurationInt
import scala.util.Random

trait Director {
    protected val infoGenerator: InfoGenerator
    protected val client: Client
    protected val config: ConfigInstance

    private val scripts: Seq[() => IO[String]] = List(() => startP2PScript())
    def startP2PScript(): IO[String]

    def startRandomWorks(): IO[Unit] = {
        for {
            number <- IO(Random.between(0, scripts.length))
            script <- IO(scripts(number))
            _      <- script()
            _      <- IO.sleep(config.generationInfo.sendDurationSec.seconds)
        } yield ()
    }.foreverM
}
