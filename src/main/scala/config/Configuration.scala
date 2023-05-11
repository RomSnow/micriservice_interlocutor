package config

import com.typesafe.config._

import scala.jdk.CollectionConverters.{MapHasAsScala, SetHasAsScala}
import scala.util.Try

object Configuration {
    case class ConfigInstance(
        testType: String,
        serverNetwork: ServerNetwork,
        interlocutorsInfo: InterlocutorsInfo,
        generationInfo: GenerationConf,
        fileConf: StatFileConf,
        kafkaConfigMap: Map[String, AnyRef]
    )

    object ConfigInstance {
        def from(config: Config): ConfigInstance =
            ConfigInstance(
                config.getString("service_type"),
                ServerNetwork.from(config),
                InterlocutorsInfo.from(config),
                GenerationConf.from(config),
                StatFileConf.from(config),
                KafkaMapConfig.from(config)
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
        def from(config: Config): Map[String, AnyRef] = {
            val configItem = config.getObject("kafka-producer-config")
            configItem.asScala.map { case (k, v) => k -> v.unwrapped()}.toMap
        }
    }

    def init(): Either[Throwable, ConfigInstance] =
        Try {
            val config: Config = ConfigFactory.load()
            ConfigInstance.from(config)
        }.toEither

}
