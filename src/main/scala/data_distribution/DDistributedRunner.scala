package data_distribution

import akka.actor.{ActorSystem, Props}

import scala.language.postfixOps


/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
object DDistributedRunner extends App {

  val system = ActorSystem("Politrons-cluster")

  system.actorOf(Props(new ProducerBot()))

  system.actorOf(Props(new ConsumerBot("Consumer 1")))
  system.actorOf(Props(new ConsumerBot("Consumer 2")))
  system.actorOf(Props(new ConsumerBot("Consumer 3")))
  system.actorOf(Props(new ConsumerBot("Consumer 4")))
  system.actorOf(Props(new ConsumerBot("Consumer 5")))
  system.actorOf(Props(new ConsumerBot("Consumer 6")))
  system.actorOf(Props(new ConsumerBot("Consumer 7")))
  system.actorOf(Props(new ConsumerBot("Consumer 8")))
  system.actorOf(Props(new ConsumerBot("Consumer 9")))
  system.actorOf(Props(new ConsumerBot("Consumer 10")))
  

}
