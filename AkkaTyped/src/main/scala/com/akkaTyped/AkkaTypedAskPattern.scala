package com.akkaTyped

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.akkaTyped.AkkaTypedAskPattern.Actor1.Actor1Message
import com.akkaTyped.AkkaTypedAskPattern.Actor2.Actor2Message

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object AkkaTypedAskPattern extends App {

  /**
   * Using the actor system, we can just create a new Actor invoking a function that return a Behavior
   * specifying which message can use internally for the communication
   */
  ActorSystem(Actor1(), "Actor1Runner")

  object Actor1 {

    /**
     * API of the Actor1
     * -----------------
     * We define our case class here as API of our Actor, is the only way
     * we can interact with this actor from outside.
     **/
    trait Actor1API

    final case class Actor1Message(body: String) extends Actor1API

    /**
     * Internal case class to wrap the response message in communications.
     * In order to work with the actor it must have same super type, that the one defined
     * with the Behavior[Type]
     */
    final case class Actor1InternalProcess(message: String) extends Actor1API

    /**
     * This function return Behavior[Type] where type is the class that we can expect to receive from the
     * callers. Since it's strong typed, the compiler it wont allow send a message to this actor, if the message
     * is not of this type or subtype.
     */
    def apply(): Behavior[Actor1API] =

    /**
     * Using [Behavior.setup] it allow us configure our actor like if a constructor of this one.
     * In ths case having the context as argument of this callback, we're able to get the actorRef
     * of the actor we want to invoke.
     */
      Behaviors.setup { context =>

        /**
         * We define a global implicit time for all ask call timeouts. As usual with implicit,
         * if we need to specify one per ask call, we just have to pass this Timeout into
         * the function as second function param.
         */
        implicit val timeout: Timeout = 3.seconds

        /**
         * Using the context inside the actor, we can create reference to another actors just using
         * [context.spawn(Actor_object.apply(), "Actor_name")]
         */
        val actor2: ActorRef[Actor2Message] = context.spawn(Actor2(), "Actor2")
        val message = "Hello async call with ask pattern"

        /**
         * Ask pattern use the context of the actor where the DSL need to specify as first argument
         * the actorRef where we want to send the call, and as second argument, a function of
         * actorRef[ResponseType] => RequestMessage where actorRef is our actor, and the type he expect
         * as response type.
         * Having the signature of this function in mind, is way encourage to send the actorRef reference in each
         * message, but it's not mandatory to use it if your sender already has a reference of you.
         *
         * Internally it use the Future[ResponseType] so once the future is resolved with Success[ResponseType] or
         * Failure of Timeout the Try function is invoked and we produce a response, and this is where the "magic"
         * happens. Internally the actor use that [response] to be send to the inbox of the actor to be processed by
         * the [Behaviors.receiveMessage] as usual.
         * In a nutshell, whatever is the output of this partial function, it will be received in the
         * [Behaviors.receiveMessage]
         */
        context.ask[Actor2Message, Actor1Message](actor2, ref => Actor2.Actor2Message(message, ref)) {
          case Success(Actor1Message(message)) => Actor1InternalProcess(message.toUpperCase)
          case Failure(_) => Actor1InternalProcess("Error in async communications")
        }

        /**
         * As I mention before, even using [Ask pattern] the [receiveMessage] function of Actor still is the
         * one in charge to process all input message, even the one that we intercept in the ask call.
         */
        Behaviors.receiveMessage {
          // the adapted message ends up being processed like any other
          // message sent to the actor
          case Actor1InternalProcess(message) =>
            println("Actor1: async message response", message)
            Behaviors.same
        }
      }
  }

  object Actor2 {

    /**
     * API of the Actor2
     * -----------------
     * We define our case class here as API of our Actor, is the only way
     * we can interact with this actor from outside
     *
     * For this pattern we define in our message, which is the actor where we need to response [ActorRef] and
     * which message [Actor1Message] then the compiler only allow us to send to this actorRef this type of
     * message.
     */
    final case class Actor2Message(message: String, replyTo: ActorRef[Actor1Message])

    /**
     * Here Since we dont need a configuration in the constructor of the actor, we can just define
     * a [Behaviors.receive] callback, where we can expect to receive the context of the actor, and
     * the message received. Since the actor define a final message class Actor2Message1 in Behavior[Actor2Message1]
     * it's not need it a pattern matching here.
     **/
    def apply(): Behavior[Actor2Message] =
      Behaviors.receive { (_, protocolMessage) =>
        println("[Actor2] Message", protocolMessage.message)
        protocolMessage.replyTo ! Actor1.Actor1Message("Hello actor1. Nice async call using futures")
        Behaviors.same
      }
  }


}
