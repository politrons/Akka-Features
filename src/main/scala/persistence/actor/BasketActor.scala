package persistence.actor

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import persistence.Item
import persistence.commands.{AddItemCommand, RemoveItemCommand}
import persistence.events.{ItemAdded, ItemEvent, ItemRemoved}
import persistence.request.GetItemsRequest
import persistence.response.{AddItemResponse, GetItemsResponse, RemoveItemResponse}

/**
  * Created by pabloperezgarcia on 03/02/2017.
  */
class BasketActor(id: String) extends PersistentActor with ActorLogging {
  private var state: Seq[Item] = Seq.empty
  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case AddItemCommand(item) =>
      persist(ItemAdded(item)) { evt =>
        state = applyEvent(evt)
        sender() ! AddItemResponse(item)
      }
    case RemoveItemCommand(itemId) =>
      persist(ItemRemoved(itemId)) { evt =>
        state = applyEvent(evt)
        sender() ! RemoveItemResponse(itemId)
      }
    case GetItemsRequest =>
      sender() ! GetItemsResponse(state)
  }

  override def receiveRecover: Receive = {
    case evt: ItemEvent => state = applyEvent(evt)
    case RecoveryCompleted => log.info("Recovery completed!")
  }

  private def applyEvent(event: ItemEvent): Seq[Item] = event match {
    case ItemAdded(item) => item +: state
    case ItemRemoved(itemId) => state.filterNot(_.id == itemId)
  }
}

object BasketActor {
  def props(id: String): Props = Props(new BasketActor(id))
}