package actor

import akka.actor.Actor
import message.{ResultMsg, WorkMsg}

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Worker extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case WorkMsg(start, nrOfElements) =>
      //sender Return asynchronously the message back to the master
      sender ! ResultMsg(returnResultMsg(start, nrOfElements)) // perform the work
  }

  def returnResultMsg(start: Int, nrOfElements: Int): String = {
    s"Worker $start in thread ${Thread.currentThread().getName} finish job ${nrOfElements-start}"
  }

}