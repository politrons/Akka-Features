package com.akkaTyped

import akka.actor.typed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.akkaTyped.AkkaTypedFromOutsideActor.TypedActor.{TypedActorMessage, TypedActorMessage1}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object AkkaTypedFromOutsideActor extends App {

  object TypedActor {

    /**
     * API of the TypedActor
     * ---------------------
     * We define our case class here as API of our Actor, is the only way
     * we can interact with this actor from outside
     */
    trait ActorTypedMessage

    final case class TypedActorMessage(body: String, replyTo: ActorRef[UntypedActorMessage]) extends ActorTypedMessage

    final case class TypedActorMessage1(body: String, replyTo: ActorRef[String]) extends ActorTypedMessage


    /**
     * Here Since we dont need a configuration in the constructor of the actor, we can just define
     * a [Behaviors.receive] callback, where we can expect to receive the context of the actor, and
     * the message received. Since the actor define a final message class Actor1Message in Behavior[Actor1Message]
     * it's not need it a pattern matching here.
     **/
    def apply(): Behavior[ActorTypedMessage] =
      Behaviors.setup { _ =>
        Behaviors.receiveMessage {
          case TypedActorMessage(body, replyTo) =>
            println("[Typed Actor] Message:", body)
            Future {
              "Hello stranger!, don't be shy and join us into the Actor gang"
            }.map(msg => replyTo ! UntypedActorMessage(msg))
            Behaviors.same
          case TypedActorMessage1(body, replyTo) =>
            println("[Typed Actor] Message:", body)
            replyTo ! "Hello stranger!, don't be shy and join us into the Actor gang"
            Behaviors.same
        }
      }
  }

  final case class UntypedActorMessage(body: String)

  /**
   * We need to create an ActorSystem/ActorRef of the typed actor we're going to call.
   */
  val typedActor: ActorSystem[TypedActor.TypedActorMessage] = ActorSystem(TypedActor(), "TypedActor")

  val typedActor1: ActorSystem[TypedActor.TypedActorMessage1] = ActorSystem(TypedActor(), "TypedActor")

  /**
   * Since the call it will use the same context as the actor, we need to provide to the call the implicits for:
   * * Timeout: Of the communication
   * * ExecutionContext: To deal with the threads
   */
  implicit val timeout: Timeout = 3.seconds
  implicit val ec: ExecutionContextExecutor = typedActor.executionContext
  implicit val scheduler: typed.Scheduler = typedActor.scheduler

  /**
   * Now having imported [akka.actor.typed.scaladsl.AskPattern._] we can use ask from an typed actor ActorSystem.
   * We just need to pass a function where we specify the actorRef of the invoker, and which message type we expect as
   * response. Internally it will create an actor typed and it will passed to this function.
   * This it will return a Future[ReturnType] where the ReturnType it will be inference by the definition type that you must
   * provide in the future signature Future[UntypedActorMessage].
   */
  val future: Future[UntypedActorMessage] = typedActor.ask(ref => TypedActorMessage("Hello Typed Actor from outside Actor system", ref))

  future.onComplete {
    case Success(value) => println(value)
    case Failure(exception) => println(exception.getMessage)
  }

  val future1: Future[String] = typedActor1.ask(ref => TypedActorMessage1("Hello Typed Actor from outside Actor system", ref))

  future1.onComplete {
    case Success(value) => println(value)
    case Failure(exception) => println(exception.getMessage)
  }

}
