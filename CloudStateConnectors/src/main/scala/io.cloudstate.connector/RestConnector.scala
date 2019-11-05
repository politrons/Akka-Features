package io.cloudstate.connector

import java.util.concurrent.TimeUnit

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Duration, Future}
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.crdt._

import scala.util.{Failure, Success, Try}

@CrdtEntity
class RestConnector(ctx: CrdtCreationContext, @EntityId val entityId: String) {

  private val endpoints: ORMap[String, LWWRegister[Connector.RestRequest]] = ctx.newORMap()

  private val sharedEndpoints: LWWRegisterMap[String, Connector.RestRequest] = new LWWRegisterMap(endpoints)

  @CommandHandler
  def getRequest(requestCommand: Connector.RestRequest): Connector.Response = {
    Try {
      val api = requestCommand.getHost + requestCommand.getEndpoint
      System.out.println("Request to Connector:" + api)
      val service: Service[Request, Response] = Http.newService(s"${requestCommand.getHost}:${requestCommand.getPort}")
      val request = http.Request(getMethod(requestCommand), requestCommand.getEndpoint)
      request.host = requestCommand.getHost

      Option(sharedEndpoints.get(api)) match {
        case Some(_) =>
         getBodyResponse(service(request))
        case None =>
          System.out.println("Adding new service in CRDT for uri:" + api)
          sharedEndpoints.put(api, requestCommand)
          getBodyResponse(service(request))
      }
    }
    match {
      case Success(responseBody) => Connector.Response.newBuilder.setResponse(responseBody).build();
      case Failure(exception) =>
        System.out.println(s"Error in Rest connector. Caused by:$exception")
        Connector.Response.newBuilder.setResponse(exception.getMessage).build();
    }
  }

  private def getBodyResponse(responseFuture: Future[Response]): String = {
    val response = Await.result(responseFuture, Duration(10, TimeUnit.SECONDS))
    response.getContentString()
  }

  private def getMethod(requestCommand: Connector.RestRequest): Method = {
    requestCommand.getMethod match {
      case "Get" => http.Method.Get
      case "Post" => http.Method.Post
      case "Put" => http.Method.Put
      case "Delete" => http.Method.Delete
    }
  }

}
