package meetup.airports

import akka.actor.{Actor, DeadLetter}
import meetup.planes.{Iberia, Panam}

/**
  * Actor that create a child actor JFK and configure three possible receive message.
  */
class Barajas extends Actor {

  val flyTime = 2000

  override def receive: Receive = {

    case "pong" =>
      println("pong")
      Thread.sleep(flyTime)
      sender() ! "ping"

    case "future_message" =>
      println("Logic in process")
      Thread.sleep(flyTime)
      sender() ! "done"

    case panam: Panam =>
      Thread.sleep(flyTime)
      println(s"Actor ${context.self.path} Panam plane landing in Barajas at:${System.currentTimeMillis() - panam.departureTime}")

    case iberia: Iberia =>
      Thread.sleep(flyTime)
      println(s"Actor ${context.self.path} Iberia plane landing in Barajas at:${System.currentTimeMillis() - iberia.departureTime}")

    /**
      * Case for dead letters to in case of that happens receive the message from subscription
      */
    case DeadLetter(msg, from, to) => println(s"####### Dead letter:msg $msg from $from to $to")
  }

}
