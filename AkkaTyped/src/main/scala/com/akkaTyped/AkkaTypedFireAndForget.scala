package com.akkaTyped

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.akkaTyped.AkkaTypedFireAndForget.Actor1.Actor1Message
import com.akkaTyped.AkkaTypedRequestResponse.Actor2.Actor2Message1

import scala.util.Random

object AkkaTypedFireAndForget extends App {

  /**
   * Using the actor system, we can just create a new Actor invoking a function that return a Behavior
   * specifying which message can use internally for the communication.
   * This factory it will return an ActorRef[Type] where type embrace the Strong type system where we can only
   * send that type message to the actor.
   */
  private val actor1Ref: ActorRef[Actor1.Actor1Message] = ActorSystem(Actor1(), "Actor1Runner")

  actor1Ref ! Actor1Message("Hello fire and forget!")

  object Actor1 {

    /**
     * API of the Actor1
     * -----------------
     * We define our case class here as API of our Actor, is the only way
     * we can interact with this actor from outside
     */
    final case class Actor1Message(body: String)

    /**
     * Here Since we dont need a configuration in the constructor of the actor, we can just define
     * a [Behaviors.receive] callback, where we can expect to receive the context of the actor, and
     * the message received. Since the actor define a final message class Actor1Message in Behavior[Actor1Message]
     * it's not need it a pattern matching here.
     **/
    def apply(): Behavior[Actor1Message] =
      Behaviors.receive { (context, message) =>
        println("[Actor1] Message:", message.body)
        Behaviors.same
      }
  }


}
