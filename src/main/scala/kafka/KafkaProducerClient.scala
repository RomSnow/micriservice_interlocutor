package kafka

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.Client
import generator.GenerationInfo
import kafka.dto.KafkaRequestInfo
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

class KafkaProducerClient(config: ConfigInstance) extends Client {
    private val producer: KafkaProducer[String, KafkaRequestInfo] =
        new KafkaProducer(config.kafkaConfigMap.asJava, new StringSerializer(), KafkaRequestInfo.serializer)

    private val topics =
        ((0, "broadcast") ::
            (1 to config.interlocutorsInfo.count)
            .filterNot(_ == config.interlocutorsInfo.selfNumber)
            .map(num => num -> s"${config.interlocutorsInfo.name}-$num-topic").toList).toMap

    private def getTopic(directionNum: Int, path: String) = IO.fromOption{
        path match {
            case "p2p" => topics.get(directionNum)
            case "broadcast" => topics.get(0)
            case _ => topics.get(directionNum)
        }
    }(new Throwable(s"get topic error path: $path, direction: $directionNum"))

    override def makeRequest(genInfo: GenerationInfo, path: String): IO[String] = {
        val requestInfo = KafkaRequestInfo.from(genInfo)
        for {
            topic <- getTopic(genInfo.destination, path)
            record = new ProducerRecord[String, KafkaRequestInfo](topic, requestInfo)
            _ <- IO.fromTry(Try(producer.send(record).get()))
        } yield ()
    }.handleErrorWith(e => IO.println("Kafka send error " + e.getMessage)).map(_ => "")
}
