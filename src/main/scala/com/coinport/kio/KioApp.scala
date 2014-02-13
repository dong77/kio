package com.coinport.kio

import akka.cluster._
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent._
import akka.routing.FromConfig

object kioApp extends App {
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args(0)).
    withFallback(ConfigFactory.load("kio"))

  val system = ActorSystem("kio", config)
  val cluster = Cluster(system)

  system.actorOf(Props(new RoleSingletonManager("normal", Props(classOf[CoreProcessor]))), "coreProcessor")

  if (cluster.selfRoles.contains("normal")) {
    system.actorOf(Props(classOf[CoreView]), "coreView")
  }

  val coreProcessorRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreProcessorRouter")
  val coreViewRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreViewRouter")

  Thread.sleep(2000)

  coreProcessorRouter ! AddUser(User(id = "u001", name = "User 1"))
  coreProcessorRouter ! AddUser(User(id = "u002", name = "User 2"))

  coreProcessorRouter ! Deposit("u001", 123.01)

  coreProcessorRouter ! Withdraw("u001", 11.01)

  coreProcessorRouter ! CreateVoucher("u001", 50)
  coreProcessorRouter ! "dump"

  coreProcessorRouter ! TransferVoucher(100, "u001")

  coreProcessorRouter ! CashoutVoucher(100)
  
  coreProcessorRouter ! "dump"

}