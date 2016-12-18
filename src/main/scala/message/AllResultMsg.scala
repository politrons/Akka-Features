package message


import scala.concurrent.duration.Duration

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
case class AllResultMsg(allResponseMsg: String, duration: Duration) extends Message
