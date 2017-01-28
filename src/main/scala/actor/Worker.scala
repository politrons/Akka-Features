package actor

import akka.actor.{Actor, ActorSystem}
import akka.contrib.mailbox.PeekMailboxExtension
import message.{ResultMsg, WorkMsg}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Worker extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case WorkMsg(start, nrOfElements) =>
      //sender Return asynchronously the message back to the master
      sender ! ResultMsg(returnResultMsg(start, nrOfElements)) // perform the wor
      PeekMailboxExtension.ack()//This return the ack to the sender
  }

  private def returnResultMsg(taskNumber: Int, nrOfElements: Int): Future[String] = {
    implicit val ec: ExecutionContext = ActorSystem().dispatcher
    Future {
       s"Worker $taskNumber in thread ${Thread.currentThread().getName} finish job ${nrOfElements - taskNumber}"
    }
  }


}