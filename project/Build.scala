import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import com.typesafe.sbt.packager.universal.UniversalKeys
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys

object ApplicationBuild extends Build with UniversalKeys {

  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  override def rootProject = Some(scalajvm)

  val sharedSrcDir = "shared"

  lazy val scalajvm = Project(
    id = "scalajvm",
    base = file("scalajvm")
  ) enablePlugins (play.PlayScala) settings (scalajvmSettings: _*) aggregate (scalajs)

  lazy val scalajs = Project(
    id   = "scalajs",
    base = file("scalajs")
  ) settings (scalajsSettings: _*)

  lazy val sharedScala = Project(
    id = "shared",
    base = file(sharedSrcDir)
  ) settings (sharedScalaSettings: _*)

  lazy val scalajvmSettings =
    Seq(
      name := "algorithim",
      version := Versions.app,
      scalaVersion := Versions.scala,
      sources in (Compile,doc) := Seq.empty,
      scalajsOutputDir := (crossTarget in Compile).value / "classes" / "public" / "javascripts",
      compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in (scalajs, Compile)),
      dist <<= dist dependsOn (fullOptJS in (scalajs, Compile)),
      addSharedSrcSetting,
      resolvers += Resolver.url("scala-js-releases",url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(Resolver.ivyStylePatterns),
      libraryDependencies ++= Dependencies.scalajvm,
      EclipseKeys.skipParents in ThisBuild := false
    ) ++ (
      // ask scalajs project to put its outputs in scalajsOutputDir
      Seq(packageExternalDepsJS, packageInternalDepsJS, packageExportedProductsJS, packageLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
        crossTarget in (scalajs, Compile, packageJSKey) := scalajsOutputDir.value
      }
    )

  lazy val scalajsSettings =
    scalaJSSettings ++ Seq(
      name := "algorithimjs",
      version := Versions.app,
      scalaVersion := Versions.scala,
      persistLauncher := true,
      persistLauncher in Test := false,
      libraryDependencies ++= Dependencies.scalajs,
      addSharedSrcSetting
    )

  lazy val sharedScalaSettings =
    Seq(
      name := "shared-scala",
      scalaSource in Compile := baseDirectory.value,
      EclipseKeys.skipProject := true
    )

  lazy val addSharedSrcSetting = unmanagedSourceDirectories in Compile += new File((baseDirectory.value / ".." / sharedSrcDir).getCanonicalPath)
}

object Dependencies {
  val scalajvm = Seq(
    "com.typesafe.play" %% "play-ws" % "2.3.1",
    "org.webjars" % "bootstrap" % "3.1.1-1",
    "com.scalarx" %% "scalarx" % Versions.scalarx,
    "org.scalajs" %% "scalajs-pickling-play-json" % Versions.scalajsPickling
  )

  val scalajs = Seq(
    "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % Versions.scalajsDom,
    "com.scalatags" %%% "scalatags" % Versions.scalatags,
    "com.scalarx" %%% "scalarx" % Versions.scalarx,
    "org.scalajs" %%% "scalajs-pickling" % Versions.scalajsPickling
  )
}

object Versions {
  val app = "0.1.0-SNAPSHOT"
  val scala = "2.11.1"
  val scalajsDom = "0.6"
  val scalatags = "0.3.8"
  val scalarx = "0.2.5"
  val scalajsPickling = "0.3.1"
}
