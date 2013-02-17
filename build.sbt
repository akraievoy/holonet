name := "holonet"

version := "1.4"

scalaVersion := "2.9.2"

libraryDependencies += "org.slf4j" % "jcl104-over-slf4j" % "1.4.3"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.4.3"

libraryDependencies += "log4j" % "log4j" % "1.2.13"

libraryDependencies += "com.google.guava" % "guava" % "11.0.2"

libraryDependencies += "org.akraievoy" % "base" % "1.3.1"

libraryDependencies += "net.sf.trove4j" % "trove4j" % "2.0.2"

libraryDependencies += "org.netlib" % "netlib-java" % "0.9.1"

libraryDependencies += "org.netlib" % "arpack-combo" % "0.1"

libraryDependencies += "org.scalaz" % "scalaz-core_2.9.1" % "6.0.4"

resolvers += "github-akraievoy-mvn_repo" at "https://raw.github.com/akraievoy/mvn_repo/master/releases/"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test->default"

mainClass in (Compile, run) := Some("org.akraievoy.holonet.exp.Runner")

javaOptions += "-Xmx3G"

// parallelExecution in Test := false




