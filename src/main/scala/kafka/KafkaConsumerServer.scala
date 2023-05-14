package kafka

import cats.effect.{ExitCode, IO, Resource}
import cats.implicits.catsSyntaxParallelSequence1
import cipher.XORCipher
import config.Configuration.ConfigInstance
import director.{Client, Server}
import generator.GenerationInfo
import kafka.dto.KafkaRequestInfo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import statSaver.StatisticSaver

import java.time.{Duration, LocalDateTime}
import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsJava, SeqHasAsJava}

class KafkaConsumerServer(config: ConfigInstance, client: Client, saver: StatisticSaver)
    extends Server {
    private def consumer: Resource[IO, KafkaConsumer[String, KafkaRequestInfo]] = Resource
        .make(
            IO(
                new KafkaConsumer[String, KafkaRequestInfo](
                    config.kafkaConsumerConfigMap.asJava,
                    new StringDeserializer(),
                    KafkaRequestInfo.deserializer
                )
            )
        )(res => IO(res.close()))
        .evalTap(cons =>
            IO(
                cons.subscribe(
                    List(
                        "broadcast-topic",
                        s"${config.interlocutorsInfo.name}-${config.interlocutorsInfo.selfNumber}-topic"
                    ).asJava
                )
            )
        )

    private def returnAns(key: String, requestInfo: KafkaRequestInfo) = {
        val ansContent = XORCipher.encryptOrDecrypt(requestInfo.content, requestInfo.key)
        val ansInfo =
            GenerationInfo(requestInfo.uuid, requestInfo.host, ansContent, requestInfo.key)
        client.makeRequest(ansInfo, s"$key-response").void
    }

    private def saveInfo(key: String, requestInfo: KafkaRequestInfo) =
        saver.saveResultInFile(
            requestInfo.uuid,
            key,
            requestInfo.host,
            System.currentTimeMillis,
            requestInfo.content
        )

    private def redirectFunc(key: String, requestInfo: KafkaRequestInfo) = {
        def redirectToNext(destination: Int, redirectPath: String, content: String): IO[Unit] = {
            val cryptContent = XORCipher.encryptOrDecrypt(content, requestInfo.key)
            val info = GenerationInfo(requestInfo.uuid, destination, cryptContent, requestInfo.key)
            val infoWithRedirList = info.copy(source = redirectPath + "\n" + info.source)
            for {
                _ <- saver.saveResultInFile(requestInfo.uuid, "redirectNext",
                    destination, System.currentTimeMillis, info.source, List(redirectPath))
                _ <- client.makeRequest(infoWithRedirList, "redirect")
            } yield ()
        }

        for {
            body <- IO(requestInfo.content)
            nextStr = body.takeWhile(_ != '\n')
            nextNumber = nextStr.split(';').headOption.flatMap(_.toIntOption)
            content = body.dropWhile(_ != '\n').drop(1)
            _ <- nextNumber.fold(
                saveInfo("redirectReturn",
                    requestInfo.copy(content = XORCipher.encryptOrDecrypt(requestInfo.content, requestInfo.key)))
            ) { next =>
                redirectToNext(next, nextStr.split(';').tail.mkString(";"), content)
            }
        } yield ()
    }

    private def handleMessage(key: String, requestInfo: KafkaRequestInfo): IO[Unit] =
        key match {
            case "p2p"       => returnAns(key, requestInfo)
            case "redirect"  => redirectFunc(key, requestInfo)
            case "broadcast" => returnAns(key, requestInfo)
            case _           => saveInfo(key, requestInfo)
        }
    override def run(): IO[ExitCode] =
        consumer.use { cons =>
            (for {
                messages <- IO(cons.poll(Duration.ofMillis(100)).iterator().asScala)
                _ <- messages
                    .map(record => handleMessage(record.key, record.value))
                    .toList
                    .parSequence
            } yield ())
                .handleErrorWith(e => IO.println("Kafka consumer error " + e.getMessage))
                .foreverM
        }
}
