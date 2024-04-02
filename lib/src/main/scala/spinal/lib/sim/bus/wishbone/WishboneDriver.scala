package spinal.lib.wishbone.sim

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import spinal.lib.bus.wishbone._
import scala.collection.immutable._
import scala.util.Random

object WishboneDriver{
  def apply(bus: Wishbone, clockdomain: ClockDomain) = new WishboneDriver(bus,clockdomain)
}

/** This is a helping class for driving the wishbone bus
  * @param bus the wishbone bus to drive
  * @param clockdomain the clockdomain where the bus reside
  */
class WishboneDriver(bus: Wishbone, clockdomain: ClockDomain){
  val busStatus = WishboneStatus(bus)

  /** Drive the wishbone bus as master with a transaction.
    * @param transaction The transaction to send.
    */
  def sendAsMaster(transaction : WishboneTransaction, we: Boolean): Unit = {
    transaction.driveAsMaster(bus,we)
    clockdomain.waitSamplingWhere(busStatus.isRequestAck)
  }

  /** Drive the wishbone bus as master.
    * @param transactions a sequence of transactions that compouse the wishbone cycle
    */
  def sendBlockAsMaster(transactions: Seq[WishboneTransaction], we: Boolean): Unit = {
    bus.CYC #= true
    transactions.dropRight(1).foreach{ tran =>
      bus.STB #= true
      sendAsMaster(tran, we)
      if(!bus.config.isPipelined){
        bus.STB #= false
        clockdomain.waitSampling()
      }
    }
    bus.STB #= true
    sendAsMaster(transactions.last, we)
    bus.STB #= false
    bus.CYC #= false
  }

  /** Drive the wishbone bus as master in a pipelined way.
    * @param transactions a sequence of transactions that compouse the wishbone cycle
    */
  def sendPipelinedBlockAsMaster(transactions: Seq[WishboneTransaction], we: Boolean): Unit = {
    bus.CYC #= true
    bus.STB #= true
    val ackCounter = fork{
      var counter = 0
      while(counter < transactions.size){
        clockdomain.waitSamplingWhere(busStatus.isAck)
        counter = counter + 1
      }
    }
    transactions.foreach(sendAsMaster(_, we))
    bus.STB #= false
    ackCounter.join()
    bus.CYC #= false
  }

  /** Drive the wishbone bus as slave with a transaction, and acknoledge the master.
    * @param transaction The transaction to send.
    */
  def sendAsSlave(transaction : WishboneTransaction): Unit = {
    clockdomain.waitSamplingWhere(busStatus.isTransfer)
    transaction.driveAsSlave(bus)
    bus.ACK #= true
    waitUntil(!busStatus.isTransfer)
    bus.ACK #= false
  }

  /** Drive the wishbone bus as a slave.
    * this function can hang if the master require more transactions than specified
    * @param transactions a sequence of transactions that compouse the wishbone cycle
    */
  def sendBlockAsSlave(transactions: Seq[WishboneTransaction]): Unit = {
    transactions.foreach{ transaction =>
      sendAsSlave(transaction)
    }
  }

  /** Drive the wishbone bus as a slave in a pipelined way.
    * this function can hang if the master require more transactions than specified
    * @param transactions a sequence of transactions that compouse the wishbone cycle
    */
  def sendPipelinedBlockAsSlave(transactions: Seq[WishboneTransaction]): Unit = {
    bus.STALL #= false
    bus.ACK #= false
    transactions.foreach{ transaction =>
      sendAsSlave(transaction)
    }
    waitUntil(!busStatus.isTransfer)
    bus.ACK #= false
  }

  /** Drive the wishbone bus.
    * This will utomatically selects the wright function to use
    * @param transactions a sequence of transactions that compouse the wishbone cycle
    */
  def drive(transactions: Seq[WishboneTransaction], we: Boolean= true): Unit = {
    (bus.isMasterInterface,bus.config.isPipelined) match {
      case (false,false) => sendBlockAsMaster(transactions,we)
      case (false,true)  => sendPipelinedBlockAsMaster(transactions,we)
      case (true,false)  => sendBlockAsSlave(transactions)
      case (true,true)   => sendPipelinedBlockAsSlave(transactions)
    }
  }

  /** Dumb slave acknoledge.
    */
  def slaveAckResponse(): Unit = {
    clockdomain.waitSamplingWhere(busStatus.isTransfer)
    bus.ACK #= true
    waitUntil(!busStatus.isTransfer)
    bus.ACK #= false
  }

  /** Dumb acknoledge, as a pipelined slave.
    */
  def slaveAckPipelinedResponse(): Unit = {
    bus.STALL #= true
    clockdomain.waitSamplingWhere(busStatus.isCycle)
    val cycle = fork{
      fork{
        waitUntil(!busStatus.isCycle)
        bus.ACK #= false
        bus.STALL #= false
      }
      var requests = 0
      var acks = 0
      var timeout = 100
      while(busStatus.isCycle){
        timeout -= 1
        assert(timeout > 0, s"${this} ${bus} timed out")

        requests += busStatus.isRequestAck.toInt

        val stall = simRandom.nextBoolean
        bus.STALL #= stall
        val ack = simRandom.nextBoolean && (requests > 0)
        requests -= ack.toInt
        acks += ack.toInt
        bus.ACK #= ack

        clockdomain.waitSampling()
      }

      assert(requests == 0, "Bus ended cycle too early")
    }
    cycle.join()
  }

  /** Dumb slave acknoledge.
    * This will utomatically selects the wright function to use
    */
  def slaveSink(): Unit = {
    val dummy = fork{
      while(true){
        if(bus.config.isPipelined)  slaveAckPipelinedResponse()
        else                        slaveAckResponse()
      }
    }
  }

}
