package persistence

import akka.actor.{ActorSystem, PoisonPill}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import persistence.commands.AddItemCommand
import persistence.response.AddItemResponse
import persistence.actor.BasketActor
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
class Runner extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
  val basketId = "sc-000001"
  val shoppingCartActor = system.actorOf(BasketActor.props(basketId))

  private val item = new Item(basketId)
  shoppingCartActor ! AddItemCommand(item)
  expectMsg(AddItemResponse(item))

  shoppingCartActor ! PoisonPill
}
