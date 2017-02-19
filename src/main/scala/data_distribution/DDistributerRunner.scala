package data_distribution

import akka.actor.{ActorSystem, Props}

import scala.language.postfixOps


/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
object DDistributerRunner extends App {

  val system = ActorSystem("Politrons-cluster")

  system.actorOf(Props(new ProducerBot()))

  system.actorOf(Props(new ConsumerBot()))

}
