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
                openCSV ++
                grpc
    )

externalResolvers := Resolver.defaults
val Http4sVersion = "0.23.9"
lazy val http4sServer: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % Http4sVersion
)
lazy val config     = Seq("com.typesafe" % "config" % "1.4.2")
lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % "3.2.9")
lazy val openCSV = Seq("com.opencsv" % "opencsv" % "5.3")
lazy val grpc = Seq(
    "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
)

enablePlugins(JavaAppPackaging, Fs2Grpc)