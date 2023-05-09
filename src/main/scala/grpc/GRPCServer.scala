package grpc

import cats.effect.{ExitCode, IO}
import cipher.XORCipher
import config.Configuration.ConfigInstance
import director.Server
import fs2.grpc.syntax.all._
import grpc.test.{InterlocutorGrpc, RequestInfo, RequestResult}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

import scala.concurrent.{ExecutionContext, Future}

class GRPCServer(config: ConfigInstance)(implicit val executionContext: ExecutionContext) extends Server {

    private class GRPCServiceImpl() extends InterlocutorGrpc.Interlocutor {
        override def p2P(request: RequestInfo): Future[RequestResult] = {
            val encryptedContent = XORCipher.encryptOrDecrypt(request.content, request.key)
            Future(RequestResult(content = encryptedContent))
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
