package com.coinport.kio

import akka.cluster._
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent._
import akka.routing.FromConfig

object kioApp extends App {
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args(0)).
    withFallback(ConfigFactory.parseString("akka.cluster.roles = [" + args(1) + "]")).
    withFallback(ConfigFactory.load("kio"))

  val system = ActorSystem("kio", config)
  val cluster = Cluster(system)

  val coreProcessorRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreProcessorRouter")
  val coreViewRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreViewRouter")

  system.actorOf(Props(new RoleSingletonManager("normal", Props(classOf[CoreProcessor]))), "coreProcessor")

  if (cluster.selfRoles.contains("normal")) {
    system.actorOf(Props(classOf[CoreView]), "coreView")
  }

  Thread.sleep(5000)
}