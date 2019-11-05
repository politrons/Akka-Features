package io.cloudstate.connector

import java.util.concurrent.TimeUnit

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Duration, Future}
import io.cloudstate.connector.Connector.{RestRequest, RestResponse}
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.crdt._

import scala.util.{Failure, Success, Try}

/**
  * CRDT ( Conflict free replicate data type) entity that we use as Rest connector.
  * Using this type of entity we can replicate the state between others replicas in the cluster.
  *
  * We use this entity as a StatefulService that can be deployed in the same cluster as the +
  * main program, and this program can invoke the connector though the DSL created in the
  * proto file using gRPC and by localhost/port which is pretty much the same as if is in the
  * same JVM. With the great advantage that we can deploy independently this connector without affect
  * the other service.
  */
@CrdtEntity
class RestConnector(ctx: CrdtCreationContext, @EntityId val entityId: String) {

  private val endpoints: ORMap[String, LWWRegister[RestRequest]] = ctx.newORMap()

  private val sharedEndpoints: LWWRegisterMap[String, RestRequest] = new LWWRegisterMap(endpoints)

  @CommandHandler
  def makeRequest(requestCommand: RestRequest): RestResponse = {
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
      case Success(responseBody) => RestResponse.newBuilder.setResponse(responseBody).build();
      case Failure(exception) =>
        System.out.println(s"Error in Rest connector. Caused by:$exception")
        RestResponse.newBuilder.setResponse(exception.getMessage).build();
    }
  }

  private def getBodyResponse(responseFuture: Future[Response]): String = {
    val response = Await.result(responseFuture, Duration(10, TimeUnit.SECONDS))
    response.getContentString()
  }

  private def getMethod(requestCommand: RestRequest): Method = {
    requestCommand.getMethod match {
      case Connector.Method.GET => http.Method.Get
      case Connector.Method.POST => http.Method.Post
      case Connector.Method.PUT => http.Method.Put
      case Connector.Method.DELETE => http.Method.Delete
    }
  }

}
