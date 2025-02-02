/*
 * Copyright 2015-2020 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import scala.sys.process._
import laika.rewrite.link.LinkConfig
import laika.rewrite.link.ApiLinks
import laika.theme.Theme

ThisBuild / tlBaseVersion := "0.18" // your current series x.y

ThisBuild / organization := "org.creativescala"
ThisBuild / organizationName := "Creative Scala"
ThisBuild / startYear := Some(2015)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("noelwelsh", "Noel Welsh")
)

// true by default, set to false to publish to s01.oss.sonatype.org
ThisBuild / tlSonatypeUseLegacyHost := true

lazy val scala213 = "2.13.10"
lazy val scala3 = "3.2.1"

ThisBuild / crossScalaVersions := List(scala3, scala213)
ThisBuild / scalaVersion := crossScalaVersions.value.head
ThisBuild / useSuperShell := false
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / tlSitePublishBranch := Some("main")

// Run this (build) to do everything involved in building the project
commands += Command.command("build") { state =>
  "dependencyUpdates" ::
    "compile" ::
    "test" ::
    "golden/test" ::
    "scalafixAll" ::
    "scalafmtAll" ::
    state
}

lazy val css = taskKey[Unit]("Build the CSS")

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    Dependencies.munit.value,
    Dependencies.munitScalaCheck.value,
    Dependencies.miniTest.value,
    Dependencies.miniTestLaws.value
  ),
  // scalacOptions ++= Seq("-release", "8"),
  // javacOptions ++= Seq("-source", "8", "-target", "8"),
  startYear := Some(2015),
  licenses := List(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
  ),
  libraryDependencies ++= (
    if (scalaBinaryVersion.value == "2.13")
      compilerPlugin(
        "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full
      ) :: Nil
    else Nil
  )
)

lazy val root = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(moduleName := "doodle")
lazy val rootJvm =
  root.jvm
    .settings(mimaPreviousArtifacts := Set.empty)
    .dependsOn(
      core.jvm,
      java2d,
      image.jvm,
      interact.jvm,
      reactor.jvm,
      turtle.jvm
    )
    .aggregate(
      core.jvm,
      java2d,
      image.jvm,
      interact.jvm,
      reactor.jvm,
      turtle.jvm,
      golden,
      unidocs
    )
lazy val rootJs =
  root.js
    .settings(mimaPreviousArtifacts := Set.empty)
    .dependsOn(
      core.js,
      image.js,
      interact.js,
      reactor.js,
      turtle.js
    )
    .aggregate(
      core.js,
      image.js,
      interact.js,
      reactor.js,
      turtle.js,
      golden,
      unidocs
    )

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Dependencies.catsCore.value,
      Dependencies.catsEffect.value,
      Dependencies.catsFree.value
    ),
    moduleName := "doodle-core"
  )

lazy val docs =
  project
    .in(file("docs"))
    .settings(
      laikaConfig := laikaConfig.value.withConfigValue(
        LinkConfig(apiLinks =
          Seq(
            ApiLinks(baseUri =
              "https://javadoc.io/doc/org.creativescala/doodle-docs_3/latest/"
            )
          )
        )
      ),
      mdocIn := file("docs/src/pages"),
      css := {
        val src = file("docs/src/css")
        val dest1 = mdocOut.value
        val dest2 = (laikaSite / target).value
        val cmd1 =
          s"npx tailwindcss -i ${src.toString}/creative-scala.css -o ${dest1.toString}/creative-scala.css"
        val cmd2 =
          s"npx tailwindcss -i ${src.toString}/creative-scala.css -o ${dest2.toString}/creative-scala.css"
        cmd1 !

        cmd2 !
      },
      Laika / sourceDirectories ++=
        Seq(
          file("docs/src/templates"),
          (examples.js / Compile / fastOptJS / artifactPath).value
            .getParentFile() / s"${(examples.js / moduleName).value}-fastopt"
        ),
      laikaTheme := Theme.empty,
      laikaExtensions ++= Seq(
        laika.markdown.github.GitHubFlavor,
        laika.parse.code.SyntaxHighlighting,
        CreativeScalaDirectives
      ),
      tlSite := Def
        .sequential(
          (examples.js / Compile / fastLinkJS),
          (Compile / run).toTask(""),
          mdoc.toTask(""),
          css,
          laikaSite
        )
        .value
    )
    .enablePlugins(TypelevelSitePlugin)
    .dependsOn(core.jvm, image.jvm)

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin) // also enables the ScalaUnidocPlugin
  .settings(
    name := "doodle-docs",
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inAnyProject -- inProjects(
        docs,
        core.js,
        interact.js,
        examples.js,
        golden,
        image.js,
        plot.js,
        reactor.js,
        turtle.js
      )
  )

lazy val interact = crossProject(JSPlatform, JVMPlatform)
  .in(file("interact"))
  .settings(
    commonSettings,
    libraryDependencies += Dependencies.fs2.value,
    moduleName := "doodle-interact"
  )
  .jvmConfigure(_.dependsOn(core.jvm % "compile->compile;test->test"))
  .jsConfigure(_.dependsOn(core.js % "compile->compile;test->test"))

lazy val java2d = project
  .in(file("java2d"))
  .settings(
    commonSettings,
    moduleName := "doodle-java2d",
    libraryDependencies ++= Seq(
      "de.erichseifert.vectorgraphics2d" % "VectorGraphics2D" % "0.13"
    ),
    libraryDependencies += Dependencies.fs2.value,
    libraryDependencies ++=
      (if (scalaBinaryVersion == "2.13")
         List(
           "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
         )
       else Nil)
  )
  .dependsOn(core.jvm, interact.jvm)

lazy val image = crossProject(JSPlatform, JVMPlatform)
  .in(file("image"))
  .settings(commonSettings, moduleName := "doodle-image")
  .jvmConfigure(_.dependsOn(core.jvm, java2d))
  .jsConfigure(_.dependsOn(core.js))

lazy val plot = crossProject(JSPlatform, JVMPlatform)
  .in(file("plot"))
  .settings(commonSettings, moduleName := "doodle-plot")
  .jvmConfigure(_.dependsOn(core.jvm, interact.jvm))
  .jsConfigure(_.dependsOn(core.js, interact.js))

lazy val plotJvm = plot.jvm
  .settings(mimaPreviousArtifacts := Set.empty)
  .dependsOn(core.jvm, interact.jvm)
lazy val plotJs = plot.js
  .settings(mimaPreviousArtifacts := Set.empty)
  .dependsOn(core.js, interact.js)

lazy val turtle = crossProject(JSPlatform, JVMPlatform)
  .in(file("turtle"))
  .settings(commonSettings, moduleName := "doodle-turtle")

lazy val turtleJvm = turtle.jvm.dependsOn(core.jvm, image.jvm)
lazy val turtleJs = turtle.js.dependsOn(core.js, image.js)

lazy val reactor = crossProject(JSPlatform, JVMPlatform)
  .in(file("reactor"))
  .settings(
    commonSettings,
    libraryDependencies += Dependencies.fs2.value,
    moduleName := "doodle-reactor"
  )
  .jvmConfigure(_.dependsOn(core.jvm, java2d, image.jvm, interact.jvm))
  .jsConfigure(_.dependsOn(core.js, image.js, interact.js))

// Just for testing
lazy val golden = project
  .in(file("golden"))
  .settings(
    commonSettings,
    moduleName := "doodle-golden",
    libraryDependencies ++= Seq(
      Dependencies.munit.value,
      Dependencies.batik.value
    ),
    // We don't publish golden so we don't need to do MiMA checks on it.
    mimaPreviousArtifacts := Set.empty,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(core.jvm, image.jvm, interact.jvm, java2d)

// To avoid including this in the core build
lazy val examples = crossProject(JSPlatform, JVMPlatform)
  .in(file("examples"))
  .settings(
    commonSettings,
    // To generate JS examples we depend on doodle-svg. This is a circular
    // dependency! Be prepared to comment this out when APIs are in flux.
    libraryDependencies ++= Seq(
      "org.creativescala" %%% "doodle-svg" % "0.14.0",
      Dependencies.catsCore.value
    ),
    // Tell sbt it's ok that the doodle-svg and doodle version don't match
    libraryDependencySchemes += "org.creativescala" %% "doodle-svg" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-svg_sjs1" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-core" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-interact" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-image" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-java2d" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-examples" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-core_sjs1" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-interact_sjs1" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-image_sjs1" % VersionScheme.Always,
    libraryDependencySchemes += "org.creativescala" %% "doodle-examples_sjs1" % VersionScheme.Always,
    moduleName := "doodle-examples"
  )
  .jvmConfigure(
    _.settings(mimaPreviousArtifacts := Set.empty)
      .dependsOn(core.jvm, java2d, image.jvm, interact.jvm)
  )
  .jsConfigure(
    _.settings(mimaPreviousArtifacts := Set.empty)
      .dependsOn(core.js, image.js, interact.js)
  )
