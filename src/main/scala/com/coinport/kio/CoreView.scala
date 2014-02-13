package com.coinport.kio

import scala.concurrent.duration._
import akka.actor._
import akka.persistence._

class CoreView extends View with ActorLogging {
  override def processorId = "kio_core_processor"
  println("core view created")

  def receive = {
    case p @ Persistent(payload, _) => println("view catch up event: " + payload)
  }
}
