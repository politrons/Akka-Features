package actor_system

import actor.{Listener, Master}
import akka.actor.{ActorSystem, Props}
import message.RunWorkersMsg

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
object Runner extends App {

  run(numberOfWorkers = 10, numberOfElements = 10, numberOfMessages = 50)

  // actors and messages ...

  def run(numberOfWorkers: Int, numberOfElements: Int, numberOfMessages: Int) {
    // Create an Akka system
    val system = ActorSystem("Politron-Chief")

    system.mailboxes.deadLetterMailbox

    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
    val master = system.actorOf(Props(
      new Master(numberOfWorkers, numberOfMessages, numberOfElements, listener)), name = "master")

    // start the tasks
    master ! RunWorkersMsg

  }
}