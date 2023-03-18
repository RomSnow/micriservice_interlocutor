import httpServer.SimpleServer

object Main extends App {
    SimpleServer.run().unsafeRunSync()
}
