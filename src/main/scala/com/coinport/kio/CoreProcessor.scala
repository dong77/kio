package com.coinport.kio
import scala.concurrent.duration._
import akka.actor._
import akka.persistence._
import scala.collection.mutable
import java.util.Random

//------------domain objects
case class User(readPin: String = "0000", actionPin: String = "1111", name: String, id: String = null)
case class Account(uid: String, cashBalance: Double = 0)
case class Voucher(id: Long, amount: Double, owningAccountId: String, cashedOut: Boolean = false)

//------------commands
sealed trait Cmd
case class AddUser(user: User) extends Cmd
case class Deposit(uid: String, amount: Double) extends Cmd
case class Withdraw(uid: String, amount: Double) extends Cmd
case class CreateVoucher(uid: String, amount: Double) extends Cmd
case class CashoutVoucher(id: Long) extends Cmd
case class TransferVoucher(id: Long, newOwningAccountId: String) extends Cmd

//------------events
sealed trait Evt
case class UserAdded(user: User) extends Evt
case class DepositCreated(uid: String, amount: Double) extends Evt
case class WithdrawDone(uid: String, amount: Double) extends Evt
case class VoucherCreated(v: Voucher) extends Evt
case class VoucherCashouted(id: Long) extends Evt
case class VoucherTransfered(id: Long, newOwningAccountId: String) extends Evt

class CoreState {
  val users = mutable.Map.empty[String, User]
  val accounts = mutable.Map.empty[String, Account]
  accounts += "BANK" -> Account("BANK", 0.0)

  val vouchers = mutable.Map.empty[Long, Voucher]
  val userVouchers = mutable.Map.empty[String, mutable.Set[Voucher]]
  var nextVoucherId = 100L

  override def toString = {
    users.mkString("\tUsers:\n\n\t", "\n\t", "\n\t") +
      accounts.mkString("Accounts:\n\n\t", "\n\t", "\n\t") +
      vouchers.mkString("Vouchers:\n\n\t", "\n\t", "\n\t") +
      userVouchers.mkString("User Vouchers:\n\n\t", "\n\t", "\n\t") +
      "\nnextVoucherId: " + nextVoucherId
  }
}

class CoreProcessor extends EventsourcedProcessor with ActorLogging {
  override def processorId = "kio_core_processor"
  println("==============core processor created")

  val rand = new Random
  var state = new CoreState

  override val receiveRecover: Receive = {
    case SnapshotOffer(_, _) =>
    case evt: Evt => updateState(evt)
  }

  override val receiveCommand: Receive = {
    case "dump" =>
      println("\n{{{")
      println(state.toString)
      println("}}}\n")
    case "reset" => state = new CoreState

    case AddUser(u) =>
      val user = if (u.id != null) u else u.copy(id = rand.nextLong.toString)
      persist(UserAdded(user))(updateState)

    case Deposit(uid, amount) if amount > 0 =>
      persist(DepositCreated(uid, amount))(updateState)

    case Withdraw(uid, amount) if amount > 0 =>
      var account = state.accounts.getOrElse(uid, Account(uid))
      if (account.cashBalance >= amount) {
        persist(WithdrawDone(uid, amount))(updateState)
      }

    case CreateVoucher(uid, amount) if amount > 0 =>
      var account = state.accounts.getOrElse(uid, Account(uid))
      if (account.cashBalance >= amount) {
        val v = Voucher(state.nextVoucherId, amount, uid)
        persist(VoucherCreated(v))(updateState)
      }

    case CashoutVoucher(id) =>
      state.vouchers.get(id) match {
        case Some(v) =>
          persist(VoucherCashouted(id))(updateState)
        case None =>
      }

    case TransferVoucher(id, newOwningAccountId) =>
      state.vouchers.get(id) match {
        case Some(v) if v.owningAccountId != newOwningAccountId =>
          persist(VoucherTransfered(id, newOwningAccountId))(updateState)
        case _ =>
      }

  }

  def updateState(evt: Evt) = {
    evt match {
      case UserAdded(user) =>
        state.users += user.id -> user

      case DepositCreated(uid, amount) =>
        var account = state.accounts.getOrElse(uid, Account(uid))
        account = account.copy(cashBalance = account.cashBalance + amount)
        state.accounts += account.uid -> account

      case WithdrawDone(uid, amount) =>
        var account = state.accounts.getOrElse(uid, Account(uid))
        account = account.copy(cashBalance = account.cashBalance - amount)
        state.accounts += account.uid -> account

      case VoucherCreated(v) =>
        var account = state.accounts.getOrElse(v.owningAccountId, Account(v.owningAccountId))
        account = account.copy(cashBalance = account.cashBalance - v.amount)
        state.accounts += account.uid -> account

        var superAccount = state.accounts("BANK")
        superAccount = superAccount.copy(cashBalance = superAccount.cashBalance + v.amount)
        state.accounts += superAccount.uid -> superAccount

        state.vouchers += v.id -> v
        val vouchers = state.userVouchers.getOrElse(v.owningAccountId, mutable.Set.empty[Voucher])
        vouchers += v
        state.userVouchers += v.owningAccountId -> vouchers
        state.nextVoucherId = v.id + 1

      case VoucherCashouted(id) =>
        val v = state.vouchers(id)
        var account = state.accounts.getOrElse(v.owningAccountId, Account(v.owningAccountId))
        account = account.copy(cashBalance = account.cashBalance + v.amount)
        state.accounts += account.uid -> account

        var superAccount = state.accounts("BANK")
        superAccount = superAccount.copy(cashBalance = superAccount.cashBalance - v.amount)
        state.accounts += superAccount.uid -> superAccount

        state.vouchers -= v.id
        state.userVouchers(v.owningAccountId) -= v

      case VoucherTransfered(id, newOwningAccountId) =>
        var v = state.vouchers(id)
        state.userVouchers(v.owningAccountId) -= v
        v = v.copy(owningAccountId = newOwningAccountId)

        state.vouchers += v.id -> v
        val vouchers = state.userVouchers.getOrElse(v.owningAccountId, mutable.Set.empty[Voucher])
        vouchers += v
        state.userVouchers += v.owningAccountId -> vouchers
    }

  }
}
