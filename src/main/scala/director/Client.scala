package director

import cats.effect.IO
import generator.GenerationInfo

trait Client {
    def makeRequest(genInfo: GenerationInfo, path: String): IO[String]

}
