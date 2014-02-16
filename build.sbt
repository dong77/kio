// import sbtprotobuf.{ProtobufPlugin=>PB}

name := "kio"

version := "1.0"

//fork := true

scalaVersion := "2.10.3"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= {
  val akkaVersion = "2.3.0-RC3"
  val spayVersion = "1.2.0"
  Seq(
    "org.scala-stm" 			%% 	"scala-stm" 					% "0.7",
    "com.github.ddevore" 		%% 	"akka-persistence-mongo-casbah"	% "0.3-SNAPSHOT",
    "org.fusesource.leveldbjni" % 	"leveldbjni-all" 				% "1.7",
    "com.google.protobuf" 		% 	"protobuf-java" 				% "2.5.0",
    "org.scalatest" 			% 	"scalatest_2.10" 				% "1.9.1" % "test",
    "io.spray" 					%% 	"spray-json" 		 			% "1.2.5",
    "io.spray" 					% 	"spray-can" 		 			% spayVersion,
    "io.spray" 					% 	"spray-routing" 		 		% spayVersion,
    "com.typesafe.akka"			%%	"akka-cluster"					% akkaVersion,
    "com.typesafe.akka"			%%	"akka-persistence-experimental"	% akkaVersion,
    "com.typesafe.akka"			%%	"akka-contrib"					% akkaVersion
  )
}

// seq(PB.protobufSettings: _*)