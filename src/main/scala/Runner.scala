import actor.{Listener, Master}
import akka.actor.{ActorSystem, Props}
import message.RunWorkersMsg

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
object Runner extends App {

  run(numberOfWorkers = 4, numberOfElements = 10000, numberOfMessages = 10000)

  // actors and messages ...

  def run(numberOfWorkers: Int, numberOfElements: Int, numberOfMessages: Int) {
    // Create an Akka system
    val system = ActorSystem("PiSystem")

    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
    val master = system.actorOf(Props(
      new Master(numberOfWorkers, numberOfMessages, numberOfElements, listener)), name = "master")

    // start the tasks
    master ! RunWorkersMsg

  }
}