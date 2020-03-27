package com.akkaTyped

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.akkaTyped.AkkaTypedRequestResponse.Actor2.Actor2Message1

import scala.util.Random

object AkkaTypedRequestResponse extends App {

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

    final case class Actor1Message1(body: String, from: ActorRef[Actor2Message1]) extends Actor1API

    final case class Actor1Message2(body: String, from: ActorRef[Actor2Message1]) extends Actor1API

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
         * Using the context inside the actor, we can create reference to another actors just using
         * [context.spawn(Actor_object.apply(), "Actor_name")]
         */
        val actor2: ActorRef[Actor2Message1] = context.spawn(Actor2(), "Actor2")
        actor2 ! Actor2Message1("Hello from actor 1", context.self)

        /**
         * Here we define the behavior of the actor, which message we expect in our API to receive.
         * All the message that extend the trait defined [Actor1API] must be set inside the [receiveMessage]
         * just like the [receive] function of Untyped Akka works.
         */
        Behaviors.receiveMessage {
          case Actor1Message1(body, replyTo) =>
            println("[Actor1] Message1:", body)
            replyTo ! Actor2Message1("Hello actor2", context.self)
            Behaviors.same
          case Actor1Message2(body, replyTo) =>
            println("[Actor1] Message2:", body)
            replyTo ! Actor2Message1("Hello actor2", context.self)
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
     * which message [Actor1.Actor1API] then the compiler only allow us to send to this actorRef this type of
     * message.
     */
    final case class Actor2Message1(body: String, replyTo: ActorRef[Actor1.Actor1API])

    /**
     * Here Since we dont need a configuration in the constructor of the actor, we can just define
     * a [Behaviors.receive] callback, where we can expect to receive the context of the actor, and
     * the message received. Since the actor define a final message class Actor2Message1 in Behavior[Actor2Message1]
     * it's not need it a pattern matching here.
     **/
    def apply(): Behavior[Actor2Message1] =
      Behaviors.receive { (context, message) =>
        println("[Actor2] Message:", message.body)
        if (new Random().nextBoolean()) {
          message.replyTo ! Actor1.Actor1Message1("Hello actor1 how you doing?", context.self)
        } else {
          message.replyTo ! Actor1.Actor1Message2("Hi again actor1 how you doing?", context.self)
        }
        Thread.sleep(1000)//Let's give a rest the actors
        Behaviors.same
      }
  }


}
