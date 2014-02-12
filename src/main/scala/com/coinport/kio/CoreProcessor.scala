package com.coinport.kio
import scala.concurrent.duration._
import akka.actor._
import akka.persistence._
import scala.collection.mutable

class CoreProcessor extends EventsourcedProcessor with ActorLogging {
  override def processorId = "kio_core_processor"

  val balances = mutable.HashMap[Long, Double]()

  override val receiveRecover: Receive = {
    case _ =>
  }

  override val receiveCommand: Receive = {
    case c @ ConfirmablePersistent(event, _, _) =>
      persist(event)(updateState)
      c.confirm() // confirm so that the channel won't try to re-deliver the same event.
    case _ =>
  }

  def updateState(event: Any) = {
    event match {

      case _ =>
    }
  }
}
