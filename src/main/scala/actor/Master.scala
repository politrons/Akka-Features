package actor

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import message.{CalculateMsg, PiApproximationMsg, ResultMsg, WorkMsg}

import scala.concurrent.duration.Duration

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef) extends Actor {

  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  /**
    * Factory router which will create workers once it´s invoked
    */
  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinPool(nrOfWorkers)), name = "workerRouter")

  def receive = {
    case CalculateMsg =>
      for (i ← 0 until nrOfMessages) workerRouter ! WorkMsg(i * nrOfElements, nrOfElements)
    case ResultMsg(value) ⇒
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) {
        // Send the result to the listener
        listener ! PiApproximationMsg(pi, duration = Duration(System.currentTimeMillis - start, "millis"))
        // Stops this actor and all its supervised children
        context.stop(self)
      }
  }

}