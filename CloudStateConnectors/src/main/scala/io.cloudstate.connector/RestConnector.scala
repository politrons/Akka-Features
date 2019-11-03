package io.cloudstate.connector

import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.eventsourced._

@EventSourcedEntity(persistenceId = "connector-actions", snapshotEvery = 20)
class RestConnector(@EntityId val entityId: String) {

  @CommandHandler
  def getRequest(requestCommand: Connector.GetEntity): Connector.Response = {
    System.out.println("Request to Connector:" + requestCommand.getUrl)
    Connector.Response.newBuilder.setResponse("hello rest connector world").build
  }
}
