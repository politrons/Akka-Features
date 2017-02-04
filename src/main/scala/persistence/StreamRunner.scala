package persistence

import akka.actor.{ActorSystem, Props, _}
import akka.pattern.ask
import akka.util.Timeout
import persistence.actor.BasketActor
import persistence.commands.{AddItemCommand, RemoveItemCommand}
import persistence.request.GetItemsRequest

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
object StreamRunner extends App {

  implicit val timeout = Timeout(10 seconds)

  val system = ActorSystem("Politrons-persistence")

  val basketActor = system.actorOf(Props(new BasketActor("akka-persistence-1")))

  basketActor ! AddItemCommand("cola")
  basketActor ! AddItemCommand("pepsi")
  basketActor ! AddItemCommand("doritos")
  basketActor ! RemoveItemCommand("pepsi")
  basketActor ! AddItemCommand("budweiser")
  basketActor ! RemoveItemCommand("cola")

  val basket = Await.result(basketActor ? GetItemsRequest, timeout.duration)

  println(s"Basket:$basket")

  basketActor ! PoisonPill
}
