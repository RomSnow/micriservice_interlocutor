ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"
val http4sVersion = "0.20.22"

lazy val root = (project in file("."))
    .settings(
        name := "microservice_interlocutor",
        libraryDependencies ++=
            catsEffect ++
                http4sServer ++
                config ++
                openCSV
    )

externalResolvers := Resolver.defaults
lazy val Http4sVersion = "0.21.7"
lazy val http4sServer: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % Http4sVersion
)
lazy val config     = Seq("com.typesafe" % "config" % "1.4.2")
lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % "2.1.4")
lazy val openCSV = Seq("com.opencsv" % "opencsv" % "5.3")

enablePlugins(JavaAppPackaging)