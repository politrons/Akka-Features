package actor_system.actor

import actor_system.message.AllResultMsg
import akka.actor.Actor

/**
  * Created by pabloperezgarcia on 18/12/2016.
  *
  * The responsibility of this actor it´s print the result once all workers has finish to process tasks,
  */
class Listener extends Actor {

  def receive: PartialFunction[Any, Unit] = {

    case AllResultMsg(allResult, duration) =>
      println("=====================================")
      println("All result message %s in %s".format(allResult, duration))
      println("=====================================")
      context.system.stop(self)
  }

}