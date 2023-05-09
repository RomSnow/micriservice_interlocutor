package grpc

import cats.effect.IO
import config.Configuration.ConfigInstance
import director.Client
import generator.GenerationInfo
import grpc.test.InterlocutorGrpc.InterlocutorStub
import grpc.test.{InterlocutorGrpc, RequestInfo, RequestResult}
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

import scala.concurrent.Future

class GRPCClient(config: ConfigInstance) extends Client {
//    private val channels: Map[Int, ManagedChannel] =
//        (1 to config.interlocutorsInfo.count)
//            .filterNot(_ == config.interlocutorsInfo.selfNumber)
//            .map { num =>
//                val host = s"${config.interlocutorsInfo.name}-$num"
//                num -> NettyChannelBuilder.forAddress(host, config.serverNetwork.port).usePlaintext().build()
//            }.toMap

    private def getRequestFunction(stub: InterlocutorStub, path: String): RequestInfo => IO[Future[RequestResult]] =
        path match {
            case "p2p" => (requestInfo: RequestInfo) => IO(stub.p2P(requestInfo))
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
//            channel <- IO.fromOption(channels.get(genInfo.destination))(new Throwable("Destination channel not found"))
            stub = InterlocutorGrpc.stub(channel)
            requestFunction = getRequestFunction(stub, path)
            result <- IO.fromFuture(requestFunction(requestInfo))
        } yield result.content
    }

}
