package meetup.airports

import akka.actor.{Actor, ActorRef}
import meetup.planes.Iberia


class JFK(ref:ActorRef) extends Actor {

  val flyTime = 2000

  override def receive: Receive = {

    case "ping" =>
      println("ping")
      Thread.sleep(flyTime)

      ref ! "pong"

    case iberia: Iberia =>
      Thread.sleep(flyTime)
      println(s"Iberia plane landing in JFK at:${System.currentTimeMillis() - iberia.departureTime}")
      sender() ! iberia.copy(departureTime = System.currentTimeMillis())

  }


}
