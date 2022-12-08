package spinal.lib

import spinal.core._
import spinal.core.formal._
import spinal.lib.formal._

import spinal.tester.{SpinalTesterCocotbBase, SpinalTesterGhdlBase}

object StreamTester{
  case class BundleA(aaa : Int) extends Bundle{
    val a = UInt(8 bit)
    val b = Bool()
  }
}

import StreamTester._

class StreamTester extends Component {
  val io = new Bundle {
    val slave0 = slave Stream new BundleA(8)
    val master0 = master Stream new BundleA(8)
    val fifo0_occupancy = out UInt()
  }

  val fifo0 = new StreamFifo(new BundleA(8),16)
  fifo0.io.push << io.slave0
  fifo0.io.pop >/-> io.master0
  io.fifo0_occupancy := fifo0.io.occupancy

  assert(3 == LatencyAnalysis(io.slave0.a,io.master0.a))
  assert(2 == LatencyAnalysis(io.master0.ready,io.slave0.ready))



  val forkInput = slave Stream(Bits(8 bits))
  val forkOutputs = Vec(master Stream(Bits(8 bits)),3)
  (forkOutputs , StreamFork(forkInput,3)).zipped.foreach(_ << _)

  val dispatcherInOrderInput = slave Stream(Bits(8 bits))
  val dispatcherInOrderOutputs = Vec(master Stream(Bits(8 bits)),3)
  (dispatcherInOrderOutputs , StreamDispatcherSequencial(dispatcherInOrderInput,3)).zipped.foreach(_ << _)

  val streamFlowArbiterInputStream = slave Stream(Bits(8 bits))
  val streamFlowArbiterInputFlow = slave Flow(Bits(8 bits))
  val streamFlowArbiterOutput = master Flow(Bits(8 bits))
  streamFlowArbiterOutput << StreamFlowArbiter(streamFlowArbiterInputStream,streamFlowArbiterInputFlow)

  val arbiterInOrderInputs =  Vec(slave Stream(Bits(8 bits)),3)
  val arbiterInOrderOutput =  master Stream(Bits(8 bits))
  arbiterInOrderOutput << StreamArbiterFactory.sequentialOrder.on(arbiterInOrderInputs)

  val arbiterLowIdPortFirstInputs =  Vec(slave Stream(Bits(8 bits)),3)
  val arbiterLowIdPortFirstOutput =  master Stream(Bits(8 bits))
  arbiterLowIdPortFirstOutput << StreamArbiterFactory.lowerFirst.on(arbiterLowIdPortFirstInputs)

  val arbiterRoundRobinInputs =  Vec(slave Stream(Bits(8 bits)),3)
  val arbiterRoundRobinOutput =  master Stream(Bits(8 bits))
  arbiterRoundRobinOutput << StreamArbiterFactory.roundRobin.on(arbiterRoundRobinInputs)


  val arbiterLowIdPortFirstNoLockInputs =  Vec(slave Stream(Bits(8 bits)),3)
  val arbiterLowIdPortFirstNoLockOutput =  master Stream(Bits(8 bits))
  arbiterLowIdPortFirstNoLockOutput << StreamArbiterFactory.lowerFirst.noLock.on(arbiterLowIdPortFirstNoLockInputs)

  val arbiterLowIdPortFirstFragmentLockInputs =  Vec(slave Stream(Fragment(Bits(8 bits))),3)
  val arbiterLowIdPortFirstFragmentLockOutput =  master Stream(Fragment(Bits(8 bits)))
  arbiterLowIdPortFirstFragmentLockOutput << StreamArbiterFactory.lowerFirst.fragmentLock.on(arbiterLowIdPortFirstFragmentLockInputs)


  //  val muxSelect = in UInt(2 bits)
//  val muxInputs = Vec(slave Stream(Bits(8 bits)),3)
//  val muxOutput = master Stream(Bits(8 bits))
//  muxOutput << StreamMux(muxSelect,muxInputs)

//  val joinInputs = Vec(slave Stream(Bits(8 bits)),3)
//  val joinOutput = master.Event
//  joinOutput << StreamJoin(joinInputs)
}



class StreamTesterGhdlBoot extends SpinalTesterGhdlBase {
  override def getName: String = "StreamTester"
  override def createToplevel: Component = new StreamTester
}

class StreamTesterCocotbBoot extends SpinalTesterCocotbBase {
  override def getName: String = "StreamTester"
  override def pythonTestLocation: String = "tester/src/test/python/spinal/StreamTester"
  override def createToplevel: Component = new StreamTester
  override def noVhdl = true
}

object StreamTester2 {
  case class BundleA() extends Bundle{
    val a = UInt(8 bit)
    val b = Bool()
  }

  class StreamTester2 extends Component{
    val fifoA = new StreamFifo(BundleA(),16)
    val fifoAPush = slave(cloneOf(fifoA.io.push))
    val fifoAPop = master(cloneOf(fifoA.io.pop))
    val fifoAOccupancy = out(cloneOf(fifoA.io.occupancy))
    fifoA.io.push << fifoAPush
    fifoA.io.pop >> fifoAPop
    fifoA.io.occupancy <> fifoAOccupancy
    assert(2 == LatencyAnalysis(fifoAPush.a,fifoAPop.a))
    assert(1 == LatencyAnalysis(fifoAPop.ready,fifoAPush.ready))


    val fifoB = new StreamFifoLowLatency(BundleA(),16)
    val fifoBPush = slave(cloneOf(fifoB.io.push))
    val fifoBPop = master(cloneOf(fifoB.io.pop))
    val fifoBOccupancy = out(cloneOf(fifoB.io.occupancy))
    fifoB.io.push << fifoBPush
    fifoB.io.pop >> fifoBPop
    fifoB.io.occupancy <> fifoBOccupancy
    assert(0 == LatencyAnalysis(fifoBPush.a,fifoBPop.a))
    assert(1 == LatencyAnalysis(fifoBPop.ready,fifoBPush.ready))
  }
}

class StreamTester2CocotbBoot extends SpinalTesterCocotbBase {
  override def getName: String = "StreamTester2"
  override def pythonTestLocation: String = "tester/src/test/python/spinal/StreamTester2"
  override def createToplevel: Component = new StreamTester2.StreamTester2
  override def backendConfig(config: SpinalConfig): SpinalConfig = config
}

class FormalArbiterTester extends SpinalFormalFunSuite {
  def usualVerify[T <: Data](
      inputs: Vec[Stream[T]],
      output: Stream[T],
      portCount: Int,
      select: UInt,
      selectOH: Bits,
      reset: Bool,
      withLock: Boolean,
      locked: Bool
  ) = new Area {
    output.formalCovers(2)
    when(reset || past(reset)) {
      for (i <- 0 until portCount) {
        assume(inputs(i).valid === False)
      }
      assert(select === 0)
      if (withLock) assert(!locked)
    }

    for (i <- 0 until portCount) {
      inputs(i).formalAssumesSlave()
    }

    assert(select < portCount)
    assert(select === OHToUInt(selectOH))
    assert(selectOH === OHMasking.first(selectOH))
    assert(output.fire === inputs(select).fire)
    assert(output.payload === inputs(select).payload)
  }

  def prepareContext(withLock: Boolean) = new Area {
    val portCount = 5
    val select = UInt(log2Up(portCount) bit)
    val selectOH = Bits(portCount bit)
    val dataType = Bits(8 bits)
    val locked = Bool()

    val reset = ClockDomain.current.isResetActive
    val notReset = !(reset || past(reset))
    cover(notReset)
    assumeInitial(reset)

    val inputs = Vec(slave(Stream(dataType)), portCount)
    val output = master(Stream(dataType))
    val usual = usualVerify(inputs, output, portCount, select, selectOH, reset, withLock, locked)

    val inputsValid = Vec(inputs.map(_.valid))
    val selStableCond = past(output.isStall) && notReset
    val selectUnLockCond = !locked || past(output.fire)
  }

  def prepareContextFragment(withLock: Boolean) = new Area {
    val portCount = 5
    val select = UInt(log2Up(portCount) bit)
    val selectOH = Bits(portCount bit)
    val dataType = Fragment(Bits(8 bits))
    val locked = Bool()

    val reset = ClockDomain.current.isResetActive
    val notReset = !(reset || past(reset))
    cover(notReset)
    assumeInitial(reset)

    val inputs = Vec(slave(Stream(dataType)), portCount)
    val output = master(Stream(dataType))
    val usual = usualVerify(inputs, output, portCount, select, selectOH, reset, withLock, locked)

    val inputsValid = Vec(inputs.map(_.valid))
    val selStableCond = !past((inputsValid.asBits === 0) || output.last || reset)
    val selectUnLockCond = !locked || past(output.last && output.fire)
  }

  def sequentialOrderVerify(select: UInt, portCount: Int) = new Area {
    val d1 = anyconst(UInt(log2Up(portCount) bit))
    assume(d1 < portCount)
    val d2 = UInt(log2Up(portCount) bit)
    d2 := (d1 + 1) % portCount

    val cntSeqCheck = changed(select) && (select === d2)
    cover(cntSeqCheck)
    when(cntSeqCheck) {
      assert(past(select) === d1)
    }
  }

  def lowFirstVerify(
      selectOH: Bits,
      inputsvalid: Vec[Bool],
      unLockCond: Bool
  ) = new Area {
    val inputsLowerFirst = OHMasking.first(inputsvalid).asBits
    cover(unLockCond)
    when(unLockCond) {
      assert(selectOH === inputsLowerFirst)
    }
  }

  def roundRobinVerify(
      selectOH: Bits,
      inputsValid: Vec[Bool],
      portCount: Int,
      maskLocked: Vec[Bool],
      unLockCond: Bool
  ) = new Area {
    val requests = inputsValid.asBits.asUInt
    val uGranted = selectOH.asUInt
    val doubleRequests = requests @@ requests
    val doubleGrant = doubleRequests & ~(doubleRequests - uGranted)
    val masked = doubleGrant(portCount, portCount bits) | doubleGrant(0, portCount bits)
    val inputsRoundRobinOH = masked.asBits

    cover(unLockCond)
    when(unLockCond) {
      assert(selectOH === inputsRoundRobinOH)
    }
    // used for Prove
    assert(maskLocked === OHMasking.first(maskLocked))
  }

  def selStableVerify(selectOH: Bits, selStableCond: Bool, withLock: Boolean) = new Area {
    if (withLock) {
      cover(selStableCond)
      when(selStableCond) {
        assert(stable(selectOH))
      }
    }
  }

  def withAssertsVerify[T <: Data](outisStall: Bool, verifyCond: Vec[Bool], output: Stream[T], withLock: Boolean) =
    new Area {
      if (withLock) {
        val stallStableSel = outisStall && stable(verifyCond)
        cover(stallStableSel)
        when(stallStableSel) {
          output.formalAssertsMaster()
        }
      }
    }

  test("Arbiter-sequentialOrder-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val withLock = true
        val context = prepareContext(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.sequentialOrder,
            StreamArbiter.Lock.none
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }

        val withAssert = withAssertsVerify(context.output.isStall, context.inputsValid, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
        val sequentialOrder = sequentialOrderVerify(context.select, context.portCount)
      })
  }

  test("Arbiter-lowerfirst-none-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val withLock = false
        val context = prepareContext(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.lowerFirst,
            StreamArbiter.Lock.none
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }

        val withAssert = withAssertsVerify(context.output.isStall, context.inputsValid, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
        val lowFirst = lowFirstVerify(context.selectOH, context.inputsValid, context.selectUnLockCond)
      })
  }

  test("Arbiter-lowerfirst-transactionLock-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {

        val withLock = true
        val context = prepareContext(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.lowerFirst,
            StreamArbiter.Lock.transactionLock
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }
        // used for Prove
        when(context.notReset) {
          assert(past(context.output.isStall) === dut.locked)
        }

        val withAssert = withAssertsVerify(context.output.isStall, context.inputsValid, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
        val lowFirst = lowFirstVerify(context.selectOH, context.inputsValid, context.selectUnLockCond)
      })
  }

  test("Arbiter-lowerfirst-fragment-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val withLock = true
        val context = prepareContextFragment(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.lowerFirst,
            StreamArbiter.Lock.fragmentLock
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }

        val withAssert = withAssertsVerify(context.output.isStall, context.inputsValid, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
        val lowFirst = lowFirstVerify(context.selectOH, context.inputsValid, context.selectUnLockCond)
      })
  }

  test("Arbiter-roundrobin-none-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val withLock = false
        val context = prepareContext(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.roundRobin,
            StreamArbiter.Lock.none
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }

        val roundRobin = roundRobinVerify(
          context.selectOH,
          context.inputsValid,
          context.portCount,
          dut.maskLocked,
          context.selectUnLockCond
        )
        val withAssert = withAssertsVerify(context.output.isStall, roundRobin.masked.asBools, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
      })
  }

  test("Arbiter-roundrobin-transactionLock-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      .withDebug
      .doVerify(new Component {
        val withLock = true
        val context = prepareContext(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.roundRobin,
            StreamArbiter.Lock.transactionLock
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }
        // used for Prove
        when(context.notReset) {
          assert(past(context.output.isStall) === dut.locked)
        }
        val roundRobin = roundRobinVerify(
          context.selectOH,
          context.inputsValid,
          context.portCount,
          dut.maskLocked,
          context.selectUnLockCond
        )
        val withAssert = withAssertsVerify(context.output.isStall, roundRobin.masked.asBools, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
      })
  }

  test("Arbiter-roundrobin-fragment-verify") {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val withLock = true
        val context = prepareContextFragment(withLock)
        val dut = FormalDut(
          new StreamArbiter(context.dataType, context.portCount)(
            StreamArbiter.Arbitration.roundRobin,
            StreamArbiter.Lock.fragmentLock
          )
        )

        context.select := dut.io.chosen
        context.selectOH := dut.io.chosenOH
        context.locked := dut.locked
        context.output << dut.io.output
        for (i <- 0 until context.portCount) {
          context.inputs(i) >> dut.io.inputs(i)
        }

        val roundRobin = roundRobinVerify(
          context.selectOH,
          context.inputsValid,
          context.portCount,
          dut.maskLocked,
          context.selectUnLockCond
        )
        val withAssert = withAssertsVerify(context.output.isStall, roundRobin.masked.asBools, context.output, withLock)
        val selStable = selStableVerify(context.selectOH, context.selStableCond, withLock)
      })
  }
}

class FormalDeMuxTester extends SpinalFormalFunSuite {
  def formaldemux(selWithCtrl: Boolean = false) = {
    FormalConfig
      .withBMC(20)
      .withProve(20)
      .withCover(20)
      // .withDebug
      .doVerify(new Component {
        val portCount = 5
        val dataType = Bits(8 bits)
        val dut = FormalDut(new StreamDemux(dataType, portCount))

        val reset = ClockDomain.current.isResetActive

        assumeInitial(reset)

        val demuxSelect = anyseq(UInt(log2Up(portCount) bit))
        val demuxInput = slave(Stream(dataType))
        val demuxOutputs = Vec(master(Stream(dataType)), portCount)

        dut.io.select := demuxSelect
        demuxInput >> dut.io.input

        when(reset || past(reset)) {
          assume(demuxInput.valid === False)
        }

        val selStableCond = if (selWithCtrl) past(demuxOutputs(demuxSelect).isStall) else null
        if (selWithCtrl) {
          cover(selStableCond)
          when(selStableCond) {
            assume(stable(demuxSelect))
          }
        }

        assumeInitial(demuxSelect < portCount)
        cover(demuxInput.fire)
        demuxInput.formalAssumesSlave()
        demuxInput.formalCovers(5)

        val inputFireStableSelChanged = past(demuxInput.fire) && demuxInput.fire && changed(demuxSelect)
        cover(inputFireStableSelChanged)

        for (i <- 0 until portCount) {
          demuxOutputs(i) << dut.io.outputs(i)
          demuxOutputs(i).formalAssertsMaster()
          demuxOutputs(i).formalCovers(5)
        }

        for (i <- 0 until portCount) {
          assert(demuxOutputs(i).payload === demuxInput.payload)
          when(i =/= demuxSelect) {
            assert(demuxOutputs(i).valid === False)
          }
        }
        when(demuxSelect < portCount) {
          assert(demuxOutputs(demuxSelect) === demuxInput)
        }
      })
  }
  test("demux_sel_with_control") {
    formaldemux(true)
  }
  test("demux_sel_without_control") {
    shouldFail(formaldemux(false))
  }

  test("demux_with_selector") {
    FormalConfig
      .withProve(20)
      .withCover(20)
      .doVerify(new Component {
        val portCount = 5
        val dataType = Bits(8 bits)
        val dut = FormalDut(new StreamDemux(dataType, portCount))
        val selector = dut.io.createSelector()

        val reset = ClockDomain.current.isResetActive
        assumeInitial(reset)

        val demuxSelector = slave(Stream(UInt(log2Up(portCount) bit)))
        demuxSelector >> selector
        val demuxInput = slave(Stream(dataType))
        demuxInput >> dut.io.input
        val demuxOutputs = Vec(master(Stream(dataType)), portCount)
        for (i <- 0 until portCount) {
          demuxOutputs(i) << dut.io.outputs(i)
        }

        when(reset || past(reset)) {
          assume(demuxInput.valid === False)
          assume(demuxSelector.valid === False)
        }

        assumeInitial(demuxSelector.payload < portCount)
        demuxSelector.formalAssumesSlave()
        demuxInput.formalAssumesSlave()
        demuxInput.formalCovers(5)

        val inputFireStableSelChanged = past(demuxInput.fire) && demuxInput.fire && changed(dut.io.select)
        cover(inputFireStableSelChanged)

        for (i <- 0 until portCount) {
          demuxOutputs(i).formalAssertsMaster()
          demuxOutputs(i).formalCovers(5)
        }

        for (i <- 0 until portCount) {
          when(i =/= dut.io.select) {
            assert(demuxOutputs(i).valid === False)
          }
        }
        when(dut.io.select < portCount) {
          assert(demuxOutputs(dut.io.select) === demuxInput)
        }
      })
  }
}
