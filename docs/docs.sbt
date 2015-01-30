import sbt.Attributed._

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2",
  "org.webjars" % "jquery" % "1.9.0",
  "org.webjars" % "prettify" % "4-Mar-2013",
  "com.typesafe.play" %% "play-doc" % "1.2.1"
)

resolvers += Resolver.typesafeRepo("releases")

sources in generateDocs := (baseDirectory.value * "*.md").get

lazy val generateDocs = TaskKey[Seq[File]]("generateDocs")

generateDocs := {
  val outdir = target.value / "web" / "docs"
  val classpath = (fullClasspath in Compile).value
  val scalaRun = (runner in run).value
  val log = streams.value.log
  val baseDir = baseDirectory.value

  val markdownFiles = (sources in generateDocs).value

  toError(scalaRun.run("play.soap.docs.Generator",
    data(classpath), Seq(outdir.getAbsolutePath, baseDir.getAbsolutePath) ++ markdownFiles.map(_.getName.dropRight(3)),
    log))

  (outdir * "*.html").get
}

resourceGenerators in Assets += generateDocs.taskValue
managedResourceDirectories in Assets += target.value / "web" / "docs"

// This removes a circular dependency
WebKeys.exportedMappings in Assets := Nil

LessKeys.compress := true

pipelineStages := Seq(uglify)

watchSources ++= (sources in generateDocs).value