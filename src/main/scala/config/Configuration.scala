package config

import com.typesafe.config._

import scala.util.Try

object Configuration {
    case class ConfigInstance(
        test: String,
        serverNetwork: ServerNetwork,
        interlocutorsInfo: InterlocutorsInfo
    )

    object ConfigInstance {
        def from(config: Config): ConfigInstance =
            ConfigInstance(
                config.getString("test"),
                ServerNetwork.from(config),
                InterlocutorsInfo.from(config)
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
        count: Int
    )

    object InterlocutorsInfo {
        def from(config: Config): InterlocutorsInfo =
            InterlocutorsInfo(
                config.getString("interlocutors_info.interlocutors_name"),
                config.getInt("interlocutors_info.interlocutors_count")
            )
    }

    def init(): Either[Throwable, ConfigInstance] =
        Try {
            val config: Config = ConfigFactory.load()
            ConfigInstance.from(config)
        }.toEither

}
