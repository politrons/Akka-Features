package actor

import akka.actor.Actor
import message.{ResultMsg, WorkMsg}

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Worker extends Actor {

  def receive = {
    case WorkMsg(start, nrOfElements) =>
      //sender Return asynchronously the message back to the master
      sender ! ResultMsg(calculatePiFor(start, nrOfElements)) // perform the work
  }

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i ← start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

}