package director.implimentations

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.{Client, SpecialModeDirector}
import generator.InfoGenerator
import statSaver.StatisticSaver

class OneToAllKafkaDirector(
                               infoGenerator: InfoGenerator,
                               client: Client,
                               config: ConfigInstance,
                               saver: StatisticSaver
                           ) extends KafkaDirector(infoGenerator, client, config, saver) with SpecialModeDirector {
    override def startSpecialScript(): IO[Unit] = startBroadcastScript()
}
