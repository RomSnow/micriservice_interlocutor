package statSaver

import cats.effect.IO
import com.opencsv._
import config.Configuration.ConfigInstance

import java.io._
import java.time.LocalDateTime
import java.util.UUID
import scala.collection.JavaConverters._
import scala.util._

class StatisticSaver(config: ConfigInstance) {
    initFile()

    private val headers = List("id", "sendTime", "receivingTime", "content")
    def saveResultInFile(
        id: UUID,
        sendTime: LocalDateTime,
        receivingTime: LocalDateTime,
        content: String
    ): IO[Unit] = IO.fromTry {
        val s = List(id.toString, sendTime.toString, receivingTime.toString, content)
        writeCsvFile(config.fileConf.fullFileName, s)
    }

    private def initFile() = {
        new File(config.fileConf.fullFileName).createNewFile()

        Try(new CSVWriter(new BufferedWriter(new FileWriter(config.fileConf.fullFileName)))).flatMap(
            (csvWriter: CSVWriter) =>
                Try {
                    csvWriter.writeAll(
                        List(headers).map(_.toArray).asJava
                    )
                    csvWriter.close()
                } match {
                    case f@Failure(_) =>
                        Try(csvWriter.close()).recoverWith { case _ =>
                            f
                        }
                    case success =>
                        success
                }
        )
    }

    private def writeCsvFile(
        fileName: String,
        row: List[String]
    ): Try[Unit] =
        Try(new CSVWriter(new FileWriter(fileName, true))).flatMap(
            (csvWriter: CSVWriter) =>
                Try {
                    csvWriter.writeNext(
                        row.toArray
                    )
                    csvWriter.close()
                } match {
                    case f @ Failure(_) =>
                        Try(csvWriter.close()).recoverWith { case _ =>
                            f
                        }
                    case success =>
                        success
                }
        )

}
