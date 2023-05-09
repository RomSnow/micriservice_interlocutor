package director

import cats.effect.{ExitCode, IO}

trait Server {
    def run(): IO[ExitCode]
}
