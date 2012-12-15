name := "holonet"

version := "1.4"

scalaVersion := "2.9.2"

libraryDependencies += "org.codehaus.jackson" % "jackson-core-asl" % "1.4.3"

libraryDependencies += "org.akraievoy" % "couch" % "1.3.11"

libraryDependencies += "org.slf4j" % "jcl104-over-slf4j" % "1.4.3"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.4.3"

libraryDependencies += "log4j" % "log4j" % "1.2.13"

libraryDependencies += "com.google.guava" % "guava" % "11.0.2"

libraryDependencies += "commons-dbutils" % "commons-dbutils" % "1.1"

libraryDependencies += "com.h2database" % "h2" % "1.1.117"

libraryDependencies += "org.akraievoy" % "base-runner-spring" % "1.3.3"

libraryDependencies += "org.akraievoy" % "base" % "1.3.1"

libraryDependencies += "org.springframework" % "spring-context" % "2.5.6"

libraryDependencies += "net.sf.trove4j" % "trove4j" % "2.0.2"

libraryDependencies += "org.netlib" % "netlib-java" % "0.9.1"

libraryDependencies += "org.netlib" % "arpack-combo" % "0.1"

resolvers += "github-akraievoy-mvn_repo" at "https://raw.github.com/akraievoy/mvn_repo/master/releases/"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test->default"

parallelExecution in Test := false




