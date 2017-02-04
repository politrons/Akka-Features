package persistence.actor

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, RecoveryCompleted}
import persistence.commands.{AddItemCommand, RemoveItemCommand}
import persistence.events.{ItemAdded, ItemEvent, ItemRemoved}
import persistence.request.GetItemsRequest
import persistence.response.{AddItemResponse, GetItemsResponse, RemoveItemResponse}

/**
  * Created by pabloperezgarcia on 03/02/2017.
  *
  * This class extend the behave of the Akka actors with PersistentActor, forcing us to implement:
  *
  * persistenceId:  The id that we will use to identify the journal
  *
  * receiveCommand: The typical actor mailbox where we receive the commands.
  * Here is where we will make use the function persist, where we persist the new events created
  * through the information provided by the command.
  *
  * receiveRecover: Function which receive the events from the journey to rehydrate the state of the actor.
  * Once that all events has been rehydrate invoke the message RecoveryCompleted
  */
class BasketActor(id: String) extends PersistentActor with ActorLogging {

  private var state: Seq[String] = Seq.empty

  /**
    * Here we set the identify of the journal
    *
    * @return
    */
  override def persistenceId: String = id

  /**
    * As "receive" function in actor model, here we receive all commands to get the item and create the event
    * to persist.
    * Once that we persist the event we change the state of the actor
    *
    * @return
    */
  override def receiveCommand: Receive = {
    case AddItemCommand(item) =>
      persist(ItemAdded(item)) { evt =>
        log.info(s"Item added:$item")
        applyFunction(() => sender() ! AddItemResponse(item), evt)
      }
    case RemoveItemCommand(item) =>
      persist(ItemRemoved(item)) { evt =>
        log.info(s"Remove item:$item")
        applyFunction(() => sender() ! RemoveItemResponse(item), evt)
      }
    case GetItemsRequest =>
      log.info("Get items:")
      sender() ! GetItemsResponse(state)
  }

  /**
    * Before to persist any new item in the journey akka persistence rehydrate the state of your actor
    * in this case "state" Seq
    *
    * @return
    */
  override def receiveRecover: Receive = {
    case evt: ItemEvent =>
      log.info(s"Recovering event:$evt")
      state = applyEvent(evt)
    case RecoveryCompleted => log.info("Recovery completed!")
  }

  def applyFunction(function: () => Unit, evt: ItemEvent): Unit = {
    state = applyEvent(evt)
    function.apply()
  }

  private def applyEvent(event: ItemEvent): Seq[String] = event match {
    case ItemAdded(item) => item +: state
    case ItemRemoved(item) => state.filter(_ != item)
  }

}
