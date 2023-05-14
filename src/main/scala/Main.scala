import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO, IOApp}
import config.Configuration
import config.Configuration.ConfigInstance
import director.implimentations.{AllToOneDirector, HTTPDirector, KafkaDirector, OneToAllHTTPDirector, OneToAllKafkaDirector}
import director.{Client, Director, Server}
import generator.{GenerationInfo, InfoGenerator}
import grpc.{GRPCClient, GRPCServer}
import httpServer.{HttpClient, HttpServer}
import kafka.{KafkaConsumerServer, KafkaProducerClient}
import statSaver.StatisticSaver

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object Main extends IOApp {
    implicit val execContext: ExecutionContextExecutor = global
    implicit val runtimeIO: IORuntime                  = cats.effect.unsafe.implicits.global

    private def getMessageGenerationFunction(director: Director, configInstance: ConfigInstance) = {
        val isSpecialServer = configInstance.interlocutorsInfo.selfNumber == configInstance.specialModeConf.specialServerNumber
        (configInstance.workingMode, isSpecialServer) match {
            case ("one_to_all", true) => director.startRandomWorks()
            case ("one_to_all", false) => IO.unit
            case ("all_to_one", true) => IO.unit
            case ("all_to_one", false) => director.startRandomWorks()
            case _ => director.startRandomWorks()
        }
    }

    private lazy val clientF: ConfigInstance => Client =
        (configuration: ConfigInstance) =>
            configuration.testType match {
                case "http" => new HttpClient(configuration)
                case "grpc" => new GRPCClient(configuration)
                case "kafka" => new KafkaProducerClient(configuration)
                case _ => new HttpClient(configuration)
            }

    private lazy val serverF: (ConfigInstance, Client, StatisticSaver) => Server =
        (configuration: ConfigInstance, client: Client, statisticSaver: StatisticSaver) =>
            configuration.testType match {
                case "http" => new HttpServer(configuration, client, statisticSaver)
                case "grpc" => new GRPCServer(configuration, client, statisticSaver)
                case "kafka" => new KafkaConsumerServer(configuration, client, statisticSaver)
                case _ => new HttpServer(configuration, client, statisticSaver)
            }

    private lazy val directorF: (InfoGenerator, Client, ConfigInstance, StatisticSaver) => Director =
        (infoGenerator: InfoGenerator, client: Client, configuration: ConfigInstance, statisticSaver: StatisticSaver) =>
            (configuration.testType, configuration.workingMode) match {
                case (_, "all_to_one") => new AllToOneDirector(infoGenerator, client, configuration, statisticSaver)
                case ("kafka", "one_to_all") => new OneToAllKafkaDirector(infoGenerator, client, configuration, statisticSaver)
                case ("kafka", _) => new KafkaDirector(infoGenerator, client, configuration, statisticSaver)
                case (_, "one_to_all") => new OneToAllHTTPDirector(infoGenerator, client, configuration, statisticSaver)
                case _ => new HTTPDirector(infoGenerator, client, configuration, statisticSaver)
            }

    private lazy val serverFunc = for {
        configuration <- IO.fromEither(Configuration.init())

        infoGenerator  = new InfoGenerator(configuration)
        statisticSaver = new StatisticSaver(configuration)
        client         = clientF(configuration)
        director       = directorF(infoGenerator, client, configuration, statisticSaver)
        server = serverF(configuration, client, statisticSaver)

        server           <- server.run().start
        _                <- IO.sleep(5.seconds)
        messageGenerator <- getMessageGenerationFunction(director, configuration).start

        _ <- server.join
        _ <- messageGenerator.join
    } yield ()

    override def run(args: List[String]): IO[ExitCode] =
        serverFunc.map(_ => ExitCode.Success)
}
