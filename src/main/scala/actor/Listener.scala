package actor

import akka.actor.Actor
import message.PiApproximationMsg

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
class Listener extends Actor {
  def receive = {

    case PiApproximationMsg(pi, duration) ⇒
      println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s"
        .format(pi, duration))
      context.system.shutdown()
  }
}