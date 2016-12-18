package message


import scala.concurrent.duration.Duration

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
case class PiApproximationMsg(pi: Double, duration: Duration) extends Message
