package com.coinport.kio

import akka.cluster._
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent._
import akka.routing.FromConfig
import akka.contrib.pattern.ClusterSharding

object kioApp extends App {
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args(0)).
    withFallback(ConfigFactory.load("kio"))

  val system = ActorSystem("kio", config)
  val cluster = Cluster(system)

  ClusterSharding(system).start(
    typeName = "CoreProcessor",
    entryProps = if (cluster.selfRoles.contains("normal")) Some(Props[CoreProcessor]) else None,
    idExtractor = { case msg => ("1", msg) },
    shardResolver = { case _ => "1" })

  if (cluster.selfRoles.contains("normal")) {
    system.actorOf(Props(classOf[CoreView]), "coreView")
  }

	val coreProcessor = ClusterSharding(system).shardRegion("CoreProcessor")
  val coreViewRouter = system.actorOf(FromConfig.props(Props.empty), name = "coreViewRouter")

  Thread.sleep(5000)

	if (args(0) == "2551") {
  coreProcessor ! AddUser(User(id = "u001", name = "User 1"))
  coreProcessor ! AddUser(User(id = "u002", name = "User 2"))

  coreProcessor ! Deposit("u001", 123.01)

  coreProcessor ! Withdraw("u001", 11.01)

  coreProcessor ! CreateVoucher("u001", 50)
  coreProcessor ! "dump"

  coreProcessor ! TransferVoucher(100, "u001")

  coreProcessor ! CashoutVoucher(100)
  
  coreProcessor ! "dump"
	
  (1 to 100000) foreach { i =>
    coreViewRouter ! GetBalance(i)
    Thread.sleep(1000)
		coreProcessor ! "dump"
  }
	}
}