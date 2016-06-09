organization in ThisBuild := "org.pico"

scalaVersion in ThisBuild := "2.11.8"

crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.8")

version in ThisBuild := Process("./version.sh").lines.head.trim

resolvers in ThisBuild ++= Seq(
  "bintray/non"           at "http://dl.bintray.com/non/maven",
  "dl-john-ky-releases"   at "http://dl.john-ky.io/maven/releases",
  "dl-john-ky-snapshots"  at "http://dl.john-ky.io/maven/snapshots")
