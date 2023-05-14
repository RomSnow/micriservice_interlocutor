package config

import com.typesafe.config._

import scala.jdk.CollectionConverters.{MapHasAsScala, SetHasAsScala}
import scala.util.Try

object Configuration {
    case class ConfigInstance(
        testType: String,
        workingMode: String,
        serverNetwork: ServerNetwork,
        interlocutorsInfo: InterlocutorsInfo,
        generationInfo: GenerationConf,
        fileConf: StatFileConf,
        kafkaProducerConfigMap: Map[String, AnyRef],
        kafkaConsumerConfigMap: Map[String, AnyRef],
        specialModeConf: SpecialModeConf
    )

    object ConfigInstance {
        def from(config: Config): ConfigInstance =
            ConfigInstance(
                config.getString("service_type"),
                config.getString("working_mode"),
                ServerNetwork.from(config),
                InterlocutorsInfo.from(config),
                GenerationConf.from(config),
                StatFileConf.from(config),
                KafkaMapConfig.from(config, "kafka-producer-config"),
                KafkaMapConfig.from(config, "kafka-consumer-config"),
                SpecialModeConf.from(config)
            )
    }

    case class ServerNetwork(
        host: String,
        port: Int
    )

    object ServerNetwork {
        def from(config: Config): ServerNetwork =
            ServerNetwork(
                host = config.getString("server_network.host"),
                port = config.getInt("server_network.port")
            )
    }

    case class InterlocutorsInfo(
        name: String,
        count: Int,
        selfNumber: Int
    )

    object InterlocutorsInfo {
        def from(config: Config): InterlocutorsInfo =
            InterlocutorsInfo(
                config.getString("interlocutors_info.interlocutors_name"),
                config.getInt("interlocutors_info.interlocutors_count"),
                config.getInt("interlocutors_info.interlocutor_self_number")
            )
    }

    case class GenerationConf(
        infoSize: Int,
        sendDurationSec: Int,
        keyMaxSize: Int,
        redirectPathMaxSize: Int
    )

    object GenerationConf {
        def from(config: Config): GenerationConf =
            GenerationConf(
                infoSize = config.getInt("generation_info.info_symbol_count"),
                sendDurationSec = config.getInt("generation_info.send_duration_seconds"),
                keyMaxSize = config.getInt("generation_info.key_max_size"),
                redirectPathMaxSize = config.getInt("generation_info.redirect_path_max_size")
            )
    }

    case class StatFileConf(
        fullFileName: String
    )

    object StatFileConf {
        def from(config: Config): StatFileConf =
            StatFileConf(
                config.getString("statistic_file_conf.file_full_name")
            )
    }

    object KafkaMapConfig {
        def from(config: Config, objectName: String): Map[String, AnyRef] = {
            val configItem = config.getObject(objectName)
            configItem.asScala.map { case (k, v) => k -> v.unwrapped() }.toMap
        }
    }

    case class SpecialModeConf(
                                  specialServerNumber: Int
                              )

    object SpecialModeConf {
        def from(config: Config): SpecialModeConf =
            SpecialModeConf(config.getInt("special-mode-options.server_number"))
    }

    def init(): Either[Throwable, ConfigInstance] =
        Try {
            val config: Config = ConfigFactory.load()
            ConfigInstance.from(config)
        }.toEither

}
