package typed_actor

import akka.actor.{ActorSystem, TypedActor, TypedProps}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import typed_actor.impl.MyTypedActorImpl
import typed_actor.model.User

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by pabloperezgarcia on 04/03/2017.
  *
  * Typed Actor works as Actor with the different that instead to have just one entry point of message onReceive
  * as we have with actors, here we define as entry point of message all the methods defined in the interface.
  *
  * The message mechanism can be
  *
  * Fire and forget:We send the message and we dont wait for the response
  * Send and reply:We send the message and we block waiting for the answer
  * Future:Send the message get a Future and wait for the future to be finish
  */
object Runner extends App {

  implicit val system = ActorSystem("Politrons-typed-actor")
  implicit val materializer = ActorMaterializer()

  private val typedActor: MyTypedActor = TypedActor(system).typedActorOf(TypedProps[MyTypedActorImpl]())
  typedActor.register(User("politrons", 35))

  private val user = typedActor.getUserFor("politrons")

  private val result = Await.result(typedActor.isMyUserRegister("politrons"), 10 seconds)

  println(s"The user was registered? $result")

  println(s"Typed actors works for $user")

  Await.result(Source(0 to 35)
    .runForeach(year => typedActor.addYearsLived(year)), 10 seconds)

  stopAfterFinishAllMessage


  /**
    * Poison pill will stop the actor once this one finish to process all message in the mailbox
    * @return
    */
  private def stopAfterFinishAllMessage = {
    println("Stopping actor once he finish his jobs........")
    TypedActor(system).poisonPill(typedActor)
   }
}
