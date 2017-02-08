package http

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
object ClientRunner extends App {

  implicit val timeout = Timeout(10 seconds)

  val system = ActorSystem("Politrons-http")

  val actorClient = system.actorOf(Props(new ActorClient()))

}
