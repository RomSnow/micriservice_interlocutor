package grpc

import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO}
import cipher.XORCipher
import config.Configuration.ConfigInstance
import director.{Client, Server}
import fs2.grpc.syntax.all._
import grpc.interlocutor.{InterlocutorGrpc, RequestInfo, RequestResult}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import statSaver.StatisticSaver
import generator.GenerationInfo

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GRPCServer(config: ConfigInstance, client: Client, saver: StatisticSaver)(
    implicit val executionContext: ExecutionContext, val runtime: IORuntime) extends Server {

    private class GRPCServiceImpl() extends InterlocutorGrpc.Interlocutor {
        override def p2P(request: RequestInfo): Future[RequestResult] = {
            val encryptedContent = XORCipher.encryptOrDecrypt(request.content, request.key)
            (for {
                _ <- IO.whenA(config.workingMode == "all_to_one")(
                    saver.saveResultInFile(UUID.fromString(request.uuid), "p2pGet",
                        0, System.currentTimeMillis, request.content)
                )
            } yield RequestResult(content = encryptedContent)).unsafeToFuture()
        }

        override def redirect(request: RequestInfo): Future[RequestResult] = {
            val id = UUID.fromString(request.uuid)

            def saveReturn(content: String): IO[RequestResult] = {
                saver.saveResultInFile(id, "redirectReturn", config.interlocutorsInfo.selfNumber,
                    System.currentTimeMillis, XORCipher.encryptOrDecrypt(content, request.key)) *>
                    IO(RequestResult())
            }

            def redirectToNext(nextNumber: Int, redirectPath: String, content: String) = {
                val cryptContent = XORCipher.encryptOrDecrypt(content, request.key)
                val info = GenerationInfo(id, nextNumber, cryptContent, request.key)
                val infoWithRedirList = info.copy(source = redirectPath + "\n" + info.source)
                for {
                    _ <- saver.saveResultInFile(id, "redirectNext",
                        nextNumber, System.currentTimeMillis, info.source, List(redirectPath))
                    _ <- client.makeRequest(infoWithRedirList, "redirect").start
                    result <- IO(RequestResult())
                } yield result
            }

            (for {
                body <- IO(request.content)
                nextStr = body.takeWhile(_ != '\n')
                nextNumber = nextStr.split(';').headOption.flatMap(_.toIntOption)
                content = body.dropWhile(_ != '\n').drop(1)
                result <- nextNumber.fold(saveReturn(content)) { redirNext =>
                    redirectToNext(redirNext, nextStr.split(';').tail.mkString(";"), content)
                }
            } yield result).unsafeToFuture
        }
    }


    override def run(): IO[ExitCode] =
        NettyServerBuilder
            .forPort(config.serverNetwork.port)
            .addService(InterlocutorGrpc.bindService(new GRPCServiceImpl, executionContext))
            .resource[IO]
            .evalMap(server => IO(server.start()))
            .useForever

}
