package spinal.tester.scalatest

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.lib._
import spinal.core.sim._
import spinal.lib.sim.{ScoreboardInOrder, StreamDriver, StreamMonitor}


case class UnpackTestBundle() extends Bundle {
  val r = UInt(5 bits)
  val g = UInt(6 bits)
  val b = UInt(5 bits)
  val a = UInt(16 bits)
}

case class StreamUnpackFixture(bitWidth : Int, offset : Int = 0) extends Component {
  val io = new Bundle {
    val inStream = slave(new Stream(Bits(bitWidth bits)))
    val outData  = out(UnpackTestBundle())
    // Dones
    val dones    = out Bits(5 bits)
    val allDone  = out Bool()
  }

  val output = Reg(UnpackTestBundle())
  val stream = io.inStream.stage()

  val unpacker = StreamUnpacker[Bits](
    stream,
    List(
      output.r -> (0  + offset),
      output.g -> (8  + offset),
      output.b -> (16 + offset),
      output.a( 7 downto 0) -> 24,
      output.a(15 downto 8) -> 32
    )
  )

  io.outData := output

  // Done signals
  io.dones   := RegNext(unpacker.io.dones)
  io.allDone := RegNext(unpacker.io.allDone)
}


class StreamUnpackTester extends AnyFunSuite {

  case class UnpackTestUnit(var r: Int, var g: Int, var b: Int, var a: Int)

  def bitsTest(dut: StreamUnpackFixture): Unit = {
    dut.clockDomain.forkStimulus(10 )

    val scoreboard = ScoreboardInOrder[UnpackTestUnit]()

    // Wait until out of reset
    dut.clockDomain.waitSampling()

    // Stream Driver
    StreamDriver(dut.io.inStream, dut.clockDomain)(p => {p.randomize(); true})

    // Stream Monitor
    var curWord = 0
    var curR = 0
    var curG = 0
    var curB = 0
    var curA = 0
    StreamMonitor(dut.io.inStream, dut.clockDomain)(p => {
      curWord match {
        case 0 =>
          curR = (p.toInt >> dut.offset) & 0x1F
        case 1 =>
          curG = (p.toInt >> dut.offset) & 0x3F
        case 2 =>
          curB = (p.toInt >> dut.offset) & 0x1F
        case 3 =>
          curA = p.toInt & 0xFF
        case 4 =>
          curA += (p.toInt & 0xFF) << 8
          scoreboard.pushRef(UnpackTestUnit(curR, curG, curB, curA))
      }

      curWord = (curWord + 1) % 5
    })

    // Unpacked output
    for(i <- 0 to 100) {
      dut.clockDomain.waitSamplingWhere(dut.io.allDone.toBoolean)
      scoreboard.pushDut(UnpackTestUnit(
        dut.io.outData.r.toInt,
        dut.io.outData.g.toInt,
        dut.io.outData.b.toInt,
        dut.io.outData.a.toInt
      ))
      scoreboard.check()
    }

    simSuccess()
  }

  test("word aligned, bits") {
    SimConfig.compile(StreamUnpackFixture(8))
      .doSim("test")(bitsTest)
  }

  test("unaligned, bits") {
    SimConfig.compile(StreamUnpackFixture(8, 2))
      .doSim("test")(bitsTest)
  }
}