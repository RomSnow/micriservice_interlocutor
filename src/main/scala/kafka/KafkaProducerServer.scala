package kafka

import cats.effect.{ExitCode, IO}
import config.Configuration.ConfigInstance
import director.Server
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import kafka.dto.KafkaRequestInfo
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import nequi.circe.kafka._

import scala.jdk.CollectionConverters.MapHasAsJava

class KafkaProducerServer(config: ConfigInstance) extends Server {
    implicit val stringCodec: Codec[String] = deriveCodec
    private val keySerializer: Serializer[String] = implicitly
    private val keyDeserializer: Deserializer[String] = implicitly

    val producer = new KafkaProducer(config.kafkaConfigMap.asJava, keySerializer, KafkaRequestInfo.serializer)
    override def run(): IO[ExitCode] = ???
}
