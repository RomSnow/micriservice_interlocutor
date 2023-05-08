import cats.effect.{ExitCode, IO, IOApp}
import config.Configuration
import director.HTTPDirector
import generator.InfoGenerator
import httpServer.{HttpClient, HttpServer}
import statSaver.StatisticSaver

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object Main extends IOApp {
    implicit val execContext: ExecutionContextExecutor = global

    val serverFunc = for {
        configuration <- IO.fromEither(Configuration.init())

        infoGenerator  = new InfoGenerator(configuration)
        statisticSaver = new StatisticSaver(configuration)
        client         = new HttpClient(configuration)
        director = configuration.testType match {
            case "http" =>
                new HTTPDirector(infoGenerator, client, configuration, statisticSaver)
            case _ => new HTTPDirector(infoGenerator, client, configuration, statisticSaver)
        }

        server           <- new HttpServer(configuration, client, statisticSaver).run().start
        _                <- IO.sleep(5.seconds)
        messageGenerator <- director.startRandomWorks().start

        _ <- server.join
        _ <- messageGenerator.join
    } yield ()

    override def run(args: List[String]): IO[ExitCode] =
        serverFunc.map(_ => ExitCode.Success)
}
