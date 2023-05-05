import cats.effect.{ExitCode, IO, IOApp}
import config.Configuration
import director.HTTPDirector
import generator.InfoGenerator
import httpServer.{HttpClient, HttpServer}
import statSaver.StatisticSaver

object Main extends IOApp {
    val serverFunc = for {
        configuration <- IO.fromEither(Configuration.init())

        infoGenerator  = new InfoGenerator(configuration)
        statisticSaver = new StatisticSaver(configuration)
        director = configuration.testType match {
            case "http" =>
                new HTTPDirector(infoGenerator, new HttpClient(configuration), configuration, statisticSaver)
            case _ => new HTTPDirector(infoGenerator, new HttpClient(configuration), configuration, statisticSaver)
        }

        server           <- new HttpServer(configuration).run().start
        messageGenerator <- director.startRandomWorks().start

        _ <- server.join
        _ <- messageGenerator.join
    } yield ()

    override def run(args: List[String]): IO[ExitCode] =
        serverFunc.map(_ => ExitCode.Success)
}
