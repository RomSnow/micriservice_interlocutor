ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"
val http4sVersion = "0.20.22"

lazy val root = (project in file("."))
    .settings(
        name := "microservice_interlocutor",
        libraryDependencies ++= http4sServer

    )

//externalResolvers := Resolver.defaults
lazy val Http4sVersion = "0.21.7"
lazy val http4sServer: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-dsl" % Http4sVersion,
    "org.http4s" %% "http4s-circe" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % Http4sVersion
)
