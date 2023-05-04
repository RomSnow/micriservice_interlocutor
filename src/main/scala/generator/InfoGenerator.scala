package generator

import config.Configuration.ConfigInstance

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Random

class InfoGenerator(config: ConfigInstance) {
    private def id: UUID = UUID.randomUUID()

    @tailrec
    private def getDestination(currentInt: Int): Int =
        if (currentInt == config.interlocutorsInfo.selfNumber)
            getDestination(Random.between(1, config.interlocutorsInfo.count + 1))
        else
            currentInt

    private def destination: Int = getDestination(config.interlocutorsInfo.selfNumber)

    private def generateSource(): LazyList[Char] =
        Random.alphanumeric.take(config.generationInfo.infoSize)


    def genInfo(): GenerationInfo = GenerationInfo(
        id, destination, generateSource()
    )
}

case class GenerationInfo(
    id: UUID,
    destination: Int,
    source: LazyList[Char]
)
