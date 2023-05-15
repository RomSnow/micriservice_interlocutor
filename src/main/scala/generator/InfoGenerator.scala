package generator

import config.Configuration.ConfigInstance

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Random

class InfoGenerator(config: ConfigInstance) {
    private def id: UUID = UUID.randomUUID()

    @tailrec
    private def getDestination(nextInt: Int, currentInt: Int = config.interlocutorsInfo.selfNumber): Int =
        if (nextInt == currentInt)
            getDestination(Random.between(1, config.interlocutorsInfo.count + 1), currentInt)
        else
            nextInt

    private def destination: Int = getDestination(config.interlocutorsInfo.selfNumber)

    private def generateSource(): LazyList[Char] =
        Random.alphanumeric.take(config.generationInfo.infoSize)

    private def genKey(): String =
        Random.alphanumeric.take(config.generationInfo.keyMaxSize).mkString

    def getRedirectPath(): List[Int] =
        (1 until Random.between(2, config.generationInfo.redirectPathMaxSize)).foldLeft(List(config.interlocutorsInfo.selfNumber)) { case (l, _) =>
            val last = l.head
            val next = getDestination(last, last) match {
                case result if result == config.interlocutorsInfo.selfNumber => getDestination(last, last)
                case res => res
            }
            next :: l
        }

    def genInfo(): GenerationInfo = GenerationInfo(
        id, destination, generateSource().mkString, genKey()
    )
}

case class GenerationInfo(
    id: UUID,
    destination: Int,
    source: String,
    key: String
)
