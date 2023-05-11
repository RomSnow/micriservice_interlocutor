package kafka.dto

import generator.GenerationInfo
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import nequi.circe.kafka._

import java.util.UUID

case class KafkaRequestInfo(
                               uuid: UUID,
                               key: String,
                               content: String
                           )

object KafkaRequestInfo {
    implicit val codec: Codec[KafkaRequestInfo] = deriveCodec

    val serializer: Serializer[KafkaRequestInfo] = implicitly
    val deserializer: Deserializer[KafkaRequestInfo] = implicitly

    def from(info: GenerationInfo): KafkaRequestInfo = KafkaRequestInfo(info.id, info.key, info.source)
}
