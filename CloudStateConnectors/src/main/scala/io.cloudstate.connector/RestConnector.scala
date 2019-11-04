package io.cloudstate.connector

import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.crdt._

@CrdtEntity
class RestConnector(ctx: CrdtCreationContext, @EntityId val entityId: String) {

  private val endpoints: ORMap[String, LWWRegister[Connector.RestRequest]] = ctx.newORMap()

  private val sharedEndpoints: LWWRegisterMap[String, Connector.RestRequest] = new LWWRegisterMap(endpoints)

  @CommandHandler
  def getRequest(requestCommand: Connector.RestRequest): Connector.Response = {
    System.out.println("Request to Connector:" + requestCommand.getUrl)
    Option(sharedEndpoints.get(requestCommand.getUrl)) match {
      case Some(request) =>
        Connector.Response.newBuilder.setResponse(s"this request ${request.getUrl} was catched").build
      case None =>
        System.out.println("Adding new service in CRDT for uri:" + requestCommand.getUrl)
        sharedEndpoints.put(requestCommand.getUrl, requestCommand)
        Connector.Response.newBuilder.setResponse("new Request here").build
    }
  }
}
