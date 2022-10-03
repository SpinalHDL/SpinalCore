package spinal.lib.bus.amba3.ahblite.sim

import scala.language.implicitConversions

/** Possible values of HBURST
  *
  * Written to be compliant with:
  * http://eecs.umich.edu/courses/eecs373/readings/ARM_IHI0033A_AMBA_AHB-Lite_SPEC.pdf
  */
object Hburst {

  /** Single burst */
  val SINGLE = 0

  /** Incrementing burst of undefined length */
  val INCR = 1

  /** 4-beat wrapping burst */
  var WRAP4 = 2

  /** 4-beat incrementing burst */
  val INCR4 = 3

  /** 8-beat wrapping burst */
  val WRAP8 = 4

  /** 8-beat incrementing burst */
  val INCR8 = 5

  /** 16-beat incrementing burst */
  val WRAP16 = 6

  /** 16-beat incrementing burst */
  val INCR16 = 7

  /** Number of beats defined in HBURST, if any
    *
    * @param hburst
    *   HBURST
    * @return
    *   number of beats defined, if any
    */
  def beats(hburst: Int): Option[Int] = hburst match {
    case 0     => Some(1)
    case 2 | 3 => Some(4)
    case 4 | 5 => Some(8)
    case 6 | 7 => Some(16)
    case _     => None
  }

  /** Is the burst whapping?
    *
    * @param hburst
    *   HBURST
    * @return
    *   true if it is wrapping, else false
    */
  def isWrapping(hburst: Int): Boolean = hburst match {
    case 2 | 4 | 6 => true
    case _         => false
  }

  /** Single Hburst */
  def single(): Hburst = burst(SINGLE)

  /** Builds an INCR Hburst
    *
    * @see
    *   burst
    *
    * @param beats
    *   number of beats
    * @return
    *   Hburst
    */
  def incr(beats: Int): Hburst = new Hburst(INCR, Some(beats))

  /** 4-beat wrapping Hburst */
  def wrap4(): Hburst = burst(WRAP4)

  /** 4-beat incrementing Hburst */
  def incr4(): Hburst = burst(INCR4)

  /** 8-beat wrapping Hburst */
  def wrap8(): Hburst = burst(WRAP8)

  /** 8-beat incrementing Hburst */
  def incr8(): Hburst = burst(INCR8)

  /** 16-beat incrementing Hburst */
  def wrap16(): Hburst = burst(WRAP16)

  /** 16-beat incrementing Hburst */
  def incr16(): Hburst = burst(INCR16)

  /** Builds a Hburst
    *
    * @see
    *   incr
    *
    * @param hburst
    *   HBURST, must not be INCR
    * @return
    */
  def burst(hburst: Int): Hburst = new Hburst(hburst, None)

  /** Hburst can be implicitly converted to an integer: HBURST */
  implicit def hburst2int(hburst: Hburst): Int = hburst.hburst
}

/** Represents a burst
  *
  * @param hburst
  *   HBURST
  * @param n
  *   number of beats, given that HBURST is INCR
  */
class Hburst(_hburst: Int, n: Option[Int]) {
  require(n.isDefined == (hburst == Hburst.INCR))
  n.foreach(beats => require(beats > 0))

  /** Value of HBURST */
  def hburst: Int = _hburst

  /** Number of beats
    *
    * @return
    *   number of beats defined, if any
    */
  def beats: Int = n.getOrElse(Hburst.beats(hburst).get)

  /** Is the burst whapping?
    *
    * @return
    *   true if it is wrapping, else false
    */
  def isWrapping: Boolean = Hburst.isWrapping(hburst)
}
