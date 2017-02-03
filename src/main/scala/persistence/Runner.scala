package persistence

import akka.actor.{ActorSystem, PoisonPill}
import persistence.actor.BasketActor
import persistence.commands.AddItemCommand

/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
object Runner extends App{
  val basketId = "sc-000001"

  val system = ActorSystem("Politron-Chief")

  system.mailboxes.deadLetterMailbox

  val basketActor = system.actorOf(BasketActor.props(basketId))

  private val item = new Item(basketId)
  basketActor ! AddItemCommand(item)
//  AddItemResponse(item)

  basketActor ! PoisonPill
}
