package actor

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import message._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Master(nrOfWorkers: Int, numberOfMessages: Int, numberOfElements: Int, listener: ActorRef) extends Actor {

  var workerResult: String = ""
  var numberOfResults: Int = _
  val start: Long = System.currentTimeMillis

  implicit val materializer = ActorMaterializer()

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
    Source(0 to numberOfMessages)
      .runForeach(number => workerRouter ! WorkMsg(number, numberOfElements))
  }

  private def processWorkerResult(future: Future[String]) = {
    implicit val ec: ExecutionContext = ActorSystem().dispatcher
    future.onComplete(value => {
      numberOfResults += 1
      workerResult = workerResult.concat("\n").concat(value.get)
      if (numberOfResults == numberOfMessages) {
        processAllResult()
      }
    })
  }

  private def processAllResult() = {
    getFlow.run()
  }

  private def getFlow = {
    val source = Source.single(workerResult)
    val sink: Sink[Any, NotUsed] = getSink
    source to sink
  }

  private def getSink = {
    Sink.onComplete(result => {
      Source.single(result)
        .filter(r => r.isSuccess)
        .runForeach(done => {
          callListener
          context.stop(self)
        })

    })
  }

  private def callListener = {
    listener ! AllResultMsg(workerResult, duration = Duration(System.currentTimeMillis - start, "millis"))
  }
}