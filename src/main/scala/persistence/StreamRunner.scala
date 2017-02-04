package persistence

import akka.actor.{ActorSystem, Props}
import persistence.actor.BasketActor
import persistence.commands.AddItemCommand

/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
object StreamRunner extends App {

  //  val customConf = ConfigFactory.parseString("""
  //     akka.persistence {
  //        journal.plugin = "inmemory-journal"
  //        snapshot-store.plugin = "inmemory-snapshot-store"
  //      }
  //      """)
  //
  //  val system = ActorSystem("Politrons-stream",ConfigFactory.load(customConf) )

  val system = ActorSystem("MySystem")

//  system.mailboxes.deadLetterMailbox

  // ConfigFactory.load sandwiches customConfig between default reference
  // config and default overrides, and then resolves it.
  //  val system = ActorSystem("MySystem", ConfigFactory.load(customConf))

  val basketId = "sc-000001"

  val basketActor = system.actorOf(Props(new BasketActor(basketId)))

  private val item = new Item()

  basketActor ! AddItemCommand("cola")
  basketActor ! AddItemCommand("pepsi")


  //  basketActor ! "works"

  //  AddItemResponse(item)

//  basketActor ! PoisonPill
}
