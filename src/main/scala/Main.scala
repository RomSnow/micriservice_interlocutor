import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import config.Configuration
import httpServer.{SimpleClient, SimpleServer}

object Main extends IOApp {
    val serverFunc = for {
        configuration <- IO.fromEither(Configuration.init())
        server             <- SimpleServer.run(configuration).start
        client             <- SimpleClient.start(configuration).start

        _ <- server.join
        _ <- client.join
    } yield ()

    override def run(args: List[String]): IO[ExitCode] =
        serverFunc.map(_ => ExitCode.Success)
}
