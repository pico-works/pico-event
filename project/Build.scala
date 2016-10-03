import sbt.Keys._
import sbt._

object Build extends sbt.Build {  
  val pico_atomic     = "org.pico"              %%  "pico-atomic"       % "0.2.1"
  val pico_disposal   = "org.pico"              %%  "pico-disposal"     % "1.0.5"
  val cats_core       = "org.typelevel"         %%  "cats-core"         % "0.7.2"
  val simulacrum      = "com.github.mpilquist"  %%  "simulacrum"        % "0.8.0"

  val specs2_core     = "org.specs2"            %%  "specs2-core"       % "3.8.4"

  implicit class ProjectOps(self: Project) {
    def standard(theDescription: String) = {
      self
          .settings(scalacOptions in Test ++= Seq("-Yrangepos"))
          .settings(publishTo := Some("Releases" at "s3://dl.john-ky.io/maven/releases"))
          .settings(description := theDescription)
          .settings(isSnapshot := true)
          .settings(resolvers += Resolver.sonatypeRepo("releases"))
          .settings(addCompilerPlugin("org.spire-math" % "kind-projector" % "0.8.0" cross CrossVersion.binary))
          .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full))
    }

    def notPublished = self.settings(publish := {}).settings(publishArtifact := false)

    def libs(modules: ModuleID*) = self.settings(libraryDependencies ++= modules)

    def testLibs(modules: ModuleID*) = self.libs(modules.map(_ % "test"): _*)
  }

  lazy val `pico-fake` = Project(id = "pico-fake", base = file("pico-fake"))
      .standard("Fake project").notPublished
      .testLibs(specs2_core)

  lazy val `pico-event` = Project(id = "pico-event", base = file("pico-event"))
      .standard("Tiny publish-subscriber library")
      .libs(pico_atomic, pico_disposal, cats_core)
      .testLibs(specs2_core)

  lazy val all = Project(id = "pico-event-project", base = file("."))
      .notPublished
      .aggregate(`pico-event`, `pico-fake`)
}
