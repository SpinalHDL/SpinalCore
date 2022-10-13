package spinal.lib.sim.protocolSim.master

/** Extend it to build a transfer with no returned value */
trait ReadTransfer extends Transfer {

  /** The type of the result of the transfer */
  type Result = BigInt

  private var state: Option[Result] = None

  /** Run this function to declare the transfer as completed with the result */
  protected final def done(result: Result): Unit = state = Some(result)

  final def isDone: Boolean = state.isDefined

  /** Get the result of the transfer.
    *
    * Throws an exception if the transfer has not completed.
    *
    * @return
    *   the result of the transfer
    */
  final def result: Result = state.get

  /** Waits until transfer completes to get its result
    *
    * @return
    *   the result of the transfer
    */
  final def awaitResult(): Result = { await(); result }

  /** Blocking function to run one transfer and return its result
    *
    * @return
    *   the result of the transfer
    */
  final def runResult(): Result = { run(); result }
}