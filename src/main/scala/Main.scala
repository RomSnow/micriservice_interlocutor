import cats.effect.{ExitCode, IO, IOApp}
import config.Configuration
import generator.InfoGenerator
import httpServer.{HttpClient, HttpServer}
import statSaver.StatisticSaver

object Main extends IOApp {
    val serverFunc = for {
        configuration <- IO.fromEither(Configuration.init())

        client        = new HttpClient(configuration)
        infoGenerator = new InfoGenerator(configuration, client)
        statisticSaver = new StatisticSaver(configuration)

        server           <- HttpServer.run(configuration, statisticSaver).start
        messageGenerator <- infoGenerator.startMessageGeneration().start

        _ <- server.join
        _ <- messageGenerator.join
    } yield ()

    override def run(args: List[String]): IO[ExitCode] =
        serverFunc.map(_ => ExitCode.Success)
}
