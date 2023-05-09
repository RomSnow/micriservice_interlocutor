import cats.effect.{ExitCode, IO, IOApp}
import config.Configuration
import config.Configuration.ConfigInstance
import director.{Client, HTTPDirector}
import generator.InfoGenerator
import grpc.{GRPCClient, GRPCServer}
import httpServer.{HttpClient, HttpServer}
import statSaver.StatisticSaver

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object Main extends IOApp {
    implicit val execContext: ExecutionContextExecutor = global

    val clientF = (configuration: ConfigInstance) =>
        configuration.testType match {
            case "http" => new HttpClient(configuration)
            case "grpc" => new GRPCClient(configuration)
            case _      => new HttpClient(configuration)
        }

    val serverF = (configuration: ConfigInstance, client: Client, statisticSaver: StatisticSaver) =>
        configuration.testType match {
            case "http" => new HttpServer(configuration, client, statisticSaver)
            case "grpc" => new GRPCServer(configuration)
            case _      => new HttpServer(configuration, client, statisticSaver)
        }

    val serverFunc = for {
        configuration <- IO.fromEither(Configuration.init())

        infoGenerator  = new InfoGenerator(configuration)
        statisticSaver = new StatisticSaver(configuration)
        client         = clientF(configuration)
        director       = new HTTPDirector(infoGenerator, client, configuration, statisticSaver)

        server           <- serverF(configuration, client, statisticSaver).run().start
        _                <- IO.sleep(5.seconds)
        messageGenerator <- director.startRandomWorks().start

        _ <- server.join
        _ <- messageGenerator.join
    } yield ()

    override def run(args: List[String]): IO[ExitCode] =
        serverFunc.map(_ => ExitCode.Success)
}
