package com.akkaTyped

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, Extension, ExtensionId}

object ExtensionPattern extends App{

  /**
   * Creating a class of type [Extension] you create  candidate class to be used as Extension pattern
   * It will be used by another companion object that it will be used as singleton in all the ActorSystem context
   */
  class SingletonDependency(actorSystem: ActorSystem[_]) extends Extension {

    println(s"initializing instance with actor system: $actorSystem")

    def run(): Unit = println("hello world")
  }

  /**
   * This companion object must be created extending ExtensionId[Class] where the generic type,
   * is the class that it will create the instance juast once.
   * Implementing [createExtension] it will be used internally when an [apply] of this object is invoked
   * to create the instance [SingletonDependency]
   */
  object SingletonDependency extends ExtensionId[SingletonDependency] {

    override def createExtension(system: ActorSystem[_]): SingletonDependency = new SingletonDependency(system)
  }

  /**
   * Actor example where we invoke the creation of the [SingletonDependency] multiple times, but as we can see
   * only the constructor is invoked once, and only one print [initializing instance with actor system] happens
   */
  object MyActor {

    def apply(): Behavior[Any] = {
      Behaviors.setup[Any] { context =>
        SingletonDependency(context.system).run()
        SingletonDependency(context.system).run()
        SingletonDependency(context.system).run()
        SingletonDependency(context.system).run()
        SingletonDependency(context.system).run()
        Behaviors.same
      }
    }
  }

  ActorSystem(MyActor(), "My_actor")
}