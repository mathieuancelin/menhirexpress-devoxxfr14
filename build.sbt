name := "menhirexpress"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
   "org.reactivemongo" %% "play2-reactivemongo" % "0.10.0"
)     

play.Project.playScalaSettings
