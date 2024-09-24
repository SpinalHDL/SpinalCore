package spinal.lib.memory.sdram.dfi

import spinal.core._
import spinal.lib._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.memory.sdram.dfi.foundation._
import spinal.lib.memory.sdram.dfi.interface._

case class BmbBridge(bmbp: BmbParameter, tpa: TaskParameterAggregate) extends Component {
  import tpa._
  val io = new Bundle {
    val bmb = slave(Bmb(bmbp))
    val taskPort = master(TaskPort(tpp, tpa))
  }

  val bmbAdapter = BmbAdapter(bmbp, tpa)
  bmbAdapter.io.input <> io.bmb
  bmbAdapter.io.output.writeData <> io.taskPort.writeData
  bmbAdapter.io.output.rsp <> io.taskPort.rsp

  val maketask = MakeTask(tpp, tpa)
  maketask.io.cmd << bmbAdapter.io.output.cmd
  maketask.io.writeDataToken <> bmbAdapter.io.output.writeDataToken
  maketask.io.output <> io.taskPort.tasks
  maketask.io.bmbHalt <> bmbAdapter.io.bmbHalt
}
