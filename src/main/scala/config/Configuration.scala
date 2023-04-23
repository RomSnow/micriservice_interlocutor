package config

import com.typesafe.config._

import scala.util.Try

object Configuration {
    case class ConfigInstance(
        serverNetwork: ServerNetwork,
        interlocutorsInfo: InterlocutorsInfo,
        generationInfo: GenerationConf,
        fileConf: StatFileConf
    )

    object ConfigInstance {
        def from(config: Config): ConfigInstance =
            ConfigInstance(
                ServerNetwork.from(config),
                InterlocutorsInfo.from(config),
                GenerationConf.from(config),
                StatFileConf.from(config)
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
        sendDurationSec: Int
    )

    object GenerationConf {
        def from(config: Config): GenerationConf =
            GenerationConf(
                infoSize = config.getInt("generation_info.info_symbol_count"),
                sendDurationSec = config.getInt("generation_info.send_duration_seconds")
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

    def init(): Either[Throwable, ConfigInstance] =
        Try {
            val config: Config = ConfigFactory.load()
            ConfigInstance.from(config)
        }.toEither

}
