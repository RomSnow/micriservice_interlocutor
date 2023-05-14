package director

import cats.effect.IO

trait SpecialModeDirector extends Director {

    override val scripts: Seq[() => IO[Unit]] = List(
        () => startSpecialScript()
    )

    override protected def getScript(): IO[() => IO[Unit]] =
        IO(() => startSpecialScript())

    def startSpecialScript(): IO[Unit]
}
