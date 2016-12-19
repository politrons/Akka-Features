package actor

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import message._

import scala.concurrent.duration.Duration

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Master(nrOfWorkers: Int, numberOfMessages: Int, numberOfElements: Int, listener: ActorRef) extends Actor {

  var workerResult: String = ""
  var numberOfResults: Int = _
  val start: Long = System.currentTimeMillis

  /**
    * Factory router which will create a worker once it´s invoked to process a message
    */
  val workerRouter: ActorRef = context.actorOf(
    Props[Worker].withRouter(RoundRobinPool(nrOfWorkers)), name = "workerRouter")

  def receive: PartialFunction[Any, Unit] = {
    case RunWorkersMsg =>
      runWorkers()
    case ResultMsg(value) =>
      processWorkerResult(value)
  }

  private def runWorkers() = {
    for (i <- 0 until numberOfMessages){
      workerRouter ! WorkMsg(i , numberOfElements)
    }
  }

  private def processWorkerResult(value: String) = {
    numberOfResults += 1
    workerResult = workerResult.concat("\n").concat(value)
    if (numberOfResults == numberOfMessages) {
      processAllResult()
    }
  }

  private def processAllResult() = {
    // Send the result to the listener
    listener ! AllResultMsg(workerResult, duration = Duration(System.currentTimeMillis - start, "millis"))
    // Stops this actor and all its supervised children
    context.stop(self)
  }
}