package actor

import akka.actor.Actor
import message.AllResultMsg

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Listener extends Actor {

  def receive: PartialFunction[Any, Unit] = {

    case AllResultMsg(allResult, duration) =>
      println("=====================================")
      println("All result message %s in %s".format(allResult, duration))
      println("=====================================")
      context.system.shutdown()
  }

}