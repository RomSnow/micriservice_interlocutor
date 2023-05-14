package statSaver

import cats.effect.IO
import com.opencsv._
import config.Configuration.ConfigInstance

import java.io._
import java.util.UUID
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.util._

class StatisticSaver(config: ConfigInstance) {
    initFile()

    private val headers = List("id", "actionType", "destination", "time", "content", "variables")
    def saveResultInFile(
        id: UUID,
        actionType: String,
        destination: Int,
        time: Long,
        content: String,
        variables: List[String] = Nil
    ): IO[Unit] = IO.fromTry {
        val s = List(id.toString, actionType, destination.toString, time.toString, content, variables.mkString("##"))
        writeCsvFile(config.fileConf.fullFileName, s)
    }.handleErrorWith(e => IO(println("Не удалось записать данные в файл " + e.getMessage)))

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
