package actor

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import message._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pabloperezgarcia on 18/12/2016.
  *
  * As a mster Actor the responsibility of this actor, it´s just to distribute the job in the number of workers
  * that we have configure in the pool
  * The mailbox of workers are configure to use ACK, which means that if we dont receive an ack from the worker
  * we will save the message in the dead letter box and we will print a warning log
  */
class Master(nrOfWorkers: Int, numberOfMessages: Int, numberOfElements: Int, listener: ActorRef) extends Actor {

  var workerResult: String = ""
  var numberOfResults: Int = _
  val start: Long = System.currentTimeMillis

  implicit val materializer = ActorMaterializer()

  /**
    * Factory router which will create a worker once it´s invoked to process a message
    * This worker actor will use the ActorSystem mailbox configuration
    */
  val workerRouter: ActorRef = getAckActorSystem.actorOf(
    Props[Worker]
      .withDispatcher("peek-dispatcher")
      .withRouter(RoundRobinPool(nrOfWorkers)), name = "workerRouter")


  /**
    * We create an ActorSystem with an mailbox configuration to use retries system and ack
    * @return
    */
  private def getAckActorSystem = {
    ActorSystem("AckSystem", ConfigFactory.parseString(
      """
    peek-dispatcher {
      mailbox-type = "akka.contrib.mailbox.PeekMailboxType"
      max-retries = 2
    }
    """))
  }

  /**
    * Receive Partial function it´s the only function that we need to implement once we extend Actor class
    * This partial function will receive the mailbox messge and it will use chain of responsability to deliver
    * in the proper place using patter matching.
    * @return
    */
  def receive: PartialFunction[Any, Unit] = {
    case RunWorkersMsg =>
      runWorkers()
    case ResultMsg(value) =>
      processWorkerResult(value)
  }

  /**
    * Using Akka stream we will iterate over the number of message and we will send one per worker until we
    * will reach the max worker pool, the mechanism to distribute message through the nodes it will be using
    * round robin  mechanism.
    * @return
    */
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
        .map(r => r.get.toString.toUpperCase)
        .runForeach(done => {
          println(done)
          callListener
          context.stop(self)
        })

    })
  }

  private def callListener = {
    listener ! AllResultMsg(workerResult, duration = Duration(System.currentTimeMillis - start, "millis"))
  }
}