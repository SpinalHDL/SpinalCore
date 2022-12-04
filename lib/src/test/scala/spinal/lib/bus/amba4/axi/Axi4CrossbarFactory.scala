package spinal.lib.bus.amba4.axi

import spinal.core._
import spinal.lib._

import spinal.tester.SpinalTesterCocotbBase

object Axi4CrossbarTester{
  class Axi4CrossbarTester extends Component {
    val axiMasterConfig = Axi4Config(addressWidth = 13,dataWidth = 32,idWidth = 4)
    val axiSlaveConfig  = Axi4Config(addressWidth = 13,dataWidth = 32,idWidth = 6)

    val axiMasters = Vec(slave(Axi4(axiMasterConfig)),3)
    val axiSlaves  = Vec(master(Axi4(axiSlaveConfig)),4)

    val crossbarFactory = Axi4CrossbarFactory()
      .addSlaves(
        axiSlaves(0) -> (0x000,0x400),
        axiSlaves(1) -> (0x400,0x400),
        axiSlaves(2) -> (0x800,0x400),
        axiSlaves(3) -> (0xC00,0x400)
      )
      .addConnections(
        axiMasters(0) -> List(axiSlaves(1),axiSlaves(2),axiSlaves(3)),
        axiMasters(1) -> List(axiSlaves(0),axiSlaves(2),axiSlaves(3)),
        axiMasters(2) -> List(axiSlaves(0),axiSlaves(1),axiSlaves(3))
      )

    val readCrossbar = crossbarFactory.build()
  }
}

class Axi4CrossbarTesterCocotbBoot extends SpinalTesterCocotbBase {
  override def getName: String = "Axi4CrossbarTester"
  override def pythonTestLocation: String = "tester/src/test/python/spinal/Axi4CrossbarTester"
  override def createToplevel: Component = new Axi4CrossbarTester.Axi4CrossbarTester
}

object Axi4CrossbarTester2{
  class Axi4CrossbarTester2 extends Component {
    val axiMasterConfig = Axi4Config(addressWidth = 15,dataWidth = 32,idWidth = 4)
    val axiSlaveConfig  = Axi4Config(addressWidth = 15,dataWidth = 32,idWidth = 8)

    val axiMasters = Vec(slave(Axi4(axiMasterConfig)),2)
    val axiSlaves  = Vec(master(Axi4(axiSlaveConfig)),2)

    val axiReadOnlyMasters = Vec(slave(Axi4ReadOnly(axiMasterConfig)),2)
    val axiReadOnlySlaves  = Vec(master(Axi4ReadOnly(axiSlaveConfig)),2)

    val axiWriteOnlyMasters = Vec(slave(Axi4WriteOnly(axiMasterConfig)),2)
    val axiWriteOnlySlaves  = Vec(master(Axi4WriteOnly(axiSlaveConfig)),2)

    val axiSharedMasters = Vec(slave(Axi4Shared(axiMasterConfig)),2)
    val axiSharedSlaves  = Vec(master(Axi4Shared(axiSlaveConfig)),2)

    val crossbarFactory = Axi4CrossbarFactory()
      .addSlaves(
        axiSlaves(0)          -> (0x0000,0x800),
        axiSlaves(1)          -> (0x0800,0x800),
        axiSharedSlaves(0)    -> (0x1000,0x800),
        axiSharedSlaves(1)    -> (0x1800,0x800),
        axiReadOnlySlaves(0)  -> (0x2000,0x800),
        axiReadOnlySlaves(1)  -> (0x2800,0x800),
        axiWriteOnlySlaves(0) -> (0x3000,0x800),
        axiWriteOnlySlaves(1) -> (0x3800,0x800)
      )
      .addConnections(
        axiMasters(0) -> (axiSlaves ++ axiReadOnlySlaves ++ axiWriteOnlySlaves ++ axiSharedSlaves),
        axiMasters(1) -> (axiSlaves ++ axiReadOnlySlaves ++ axiWriteOnlySlaves ++ axiSharedSlaves),
        axiSharedMasters(0) -> (axiSlaves ++ axiReadOnlySlaves ++ axiWriteOnlySlaves ++ axiSharedSlaves),
        axiSharedMasters(1) -> (axiSlaves ++ axiReadOnlySlaves ++ axiWriteOnlySlaves ++ axiSharedSlaves),
        axiReadOnlyMasters(0) -> (axiSlaves ++ axiReadOnlySlaves  ++ axiSharedSlaves),
        axiReadOnlyMasters(1) -> (axiSlaves ++ axiReadOnlySlaves  ++ axiSharedSlaves),
        axiWriteOnlyMasters(0) -> (axiSlaves ++ axiWriteOnlySlaves ++ axiSharedSlaves),
        axiWriteOnlyMasters(1) -> (axiSlaves ++ axiWriteOnlySlaves ++ axiSharedSlaves)
      )

    val crossbar = crossbarFactory.build()
  }
}

class Axi4CrossbarTester2CocotbBoot extends SpinalTesterCocotbBase {
  override def getName: String = "Axi4CrossbarTester2"
  override def pythonTestLocation: String = "tester/src/test/python/spinal/Axi4CrossbarTester2"
  override def createToplevel: Component = new Axi4CrossbarTester2.Axi4CrossbarTester2
  override def backendConfig(config: SpinalConfig): SpinalConfig = super.backendConfig(config)
}
