package com.coinport.kio

import scala.concurrent.duration._
import akka.actor._
import akka.persistence._

case class GetBalance(id: Long)

class CoreView extends View with ActorLogging {
  override def processorId = "kio_core_processor"
  println("--------------core view created:" + self.path)

  def receive = {
    case x: GetBalance => println("view received: " + x)
    case p @ Persistent(payload, _) => println("view catch up event: " + payload)
  }
}
