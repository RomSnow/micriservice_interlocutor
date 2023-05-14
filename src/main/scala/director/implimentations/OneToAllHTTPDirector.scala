package director.implimentations

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.{Client, SpecialModeDirector}
import generator.InfoGenerator
import statSaver.StatisticSaver

class OneToAllHTTPDirector(
                              infoGenerator: InfoGenerator,
                              client: Client,
                              config: ConfigInstance,
                              saver: StatisticSaver
                          ) extends HTTPDirector(infoGenerator, client, config, saver) with SpecialModeDirector {
    override def startSpecialScript(): IO[Unit] = {
        println("startSpecialScript")
        startBroadcastScript()
    }
}
