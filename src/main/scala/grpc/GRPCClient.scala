package grpc

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.Client
import generator.GenerationInfo
import grpc.interlocutor.InterlocutorGrpc.InterlocutorStub
import grpc.interlocutor.{InterlocutorGrpc, RequestInfo, RequestResult}
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

import scala.concurrent.Future

class GRPCClient(config: ConfigInstance) extends Client {

    private def getRequestFunction(stub: InterlocutorStub, path: String): RequestInfo => IO[Future[RequestResult]] =
        path match {
            case "p2p" => (requestInfo: RequestInfo) => IO(stub.p2P(requestInfo))
            case "redirect" => (requestInfo: RequestInfo) => IO(stub.redirect(requestInfo))
            case _ => (requestInfo: RequestInfo) => IO(stub.p2P(requestInfo))
        }

    override def makeRequest(genInfo: GenerationInfo, path: String): IO[String] = {
        val requestInfo = RequestInfo(
            uuid = genInfo.id.toString,
            key = genInfo.key,
            content = genInfo.source
        )
        val host = s"${config.interlocutorsInfo.name}-${genInfo.destination}"
        val channel = NettyChannelBuilder.forAddress(host, config.serverNetwork.port).usePlaintext().build()

        for {
            _ <- IO.unit
            stub = InterlocutorGrpc.stub(channel)
            requestFunction = getRequestFunction(stub, path)
            result <- IO.fromFuture(requestFunction(requestInfo))
        } yield result.content
    }

}
