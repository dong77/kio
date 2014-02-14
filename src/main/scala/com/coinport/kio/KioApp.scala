package com.coinport.kio

import akka.cluster._
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent._
import akka.routing.FromConfig
import akka.contrib.pattern.ClusterSingletonManager

object kioApp extends App {
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args(0)).
    withFallback(ConfigFactory.load("kio"))

  val system = ActorSystem("kio", config)
  val cluster = Cluster(system)

  val coreProcessorRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreProcessorRouter")
  val coreViewRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreViewRouter")

  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[CoreProcessor]),
    singletonName = "coreProcessor",
    terminationMessage = PoisonPill,
    role = Some("core_processor")),
    name = "singleton")

  if (cluster.selfRoles.contains("core_view")) {
    system.actorOf(Props(classOf[CoreView]), "coreView")
  }


  Thread.sleep(5000)

  coreProcessorRouter ! AddUser(User(id = "u001", name = "User 1"))
  coreProcessorRouter ! AddUser(User(id = "u002", name = "User 2"))

  coreProcessorRouter ! Deposit("u001", 123.01)

  coreProcessorRouter ! Withdraw("u001", 11.01)

  coreProcessorRouter ! CreateVoucher("u001", 50)
  coreProcessorRouter ! "dump"

  coreProcessorRouter ! TransferVoucher(100, "u001")

  coreProcessorRouter ! CashoutVoucher(100)

  coreProcessorRouter ! "dump"

  (1 to 100000) foreach { i =>
    coreViewRouter ! GetBalance(i)

    coreProcessorRouter ! "dump"
    Thread.sleep(2000)
  }

}