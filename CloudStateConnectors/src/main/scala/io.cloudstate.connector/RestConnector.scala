package io.cloudstate.connector

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.service.{Backoff, RetryBudget}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util._
import io.cloudstate.connector.Connector.{RestRequest, RestResponse}
import io.cloudstate.javasupport.EntityId
import io.cloudstate.javasupport.crdt._
import com.twitter.conversions.DurationOps._
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

  private val logger: Logger = Logger.getLogger(classOf[RestConnector].getName)

  private val endpoints: ORMap[String, LWWRegister[RestRequest]] = ctx.newORMap()

  private val sharedEndpoints: LWWRegisterMap[String, RestRequest] = new LWWRegisterMap(endpoints)

  private var services: Map[String, Service[Request, Response]] = Map()

  private val budget: RetryBudget = RetryBudget(
    ttl = 10.seconds,
    minRetriesPerSec = 5,
    percentCanRetry = 0.1
  )

  @CommandHandler
  def makeRequest(requestCommand: RestRequest): RestResponse = {
    Try {
      val endpoint = requestCommand.getHost + requestCommand.getUri
      logger.info("Request to Connector:" + endpoint)
      val request = http.Request(getMethod(requestCommand), requestCommand.getUri)
      request.host = requestCommand.getHost
      services.get(endpoint) match {
        case Some(service) =>
          getBodyResponse(service(request))
        case None =>
          logger.info("Adding new service in CRDT for uri:" + endpoint)
          sharedEndpoints.put(endpoint, requestCommand)
          val service: Service[Request, Response] = createFinagleService(requestCommand)
          services += endpoint -> service
          getBodyResponse(service(request))
      }
    }
    match {
      case Success(responseBody) => RestResponse.newBuilder.setResponse(responseBody).build();
      case Failure(exception) =>
        logger.info(s"Error in Rest connector. Caused by:$exception")
        RestResponse.newBuilder.setResponse(exception.getMessage).build();
    }
  }

  /**
    * Function to create Finagle service with some retry strategy configuration
    */
  private def createFinagleService: RestRequest => Service[Request, Response] = {
    requestCommand =>
      Http.client
        .withRetryBudget(budget)
        .withRetryBackoff(Backoff.const(10.seconds))
        .newService(s"${requestCommand.getHost}:${requestCommand.getPort}")
  }

  private def getBodyResponse: Future[Response] => String = {
    responseFuture => Await.result(responseFuture, Duration(10, TimeUnit.SECONDS)).getContentString()
  }

  private def getMethod: RestRequest => Method = {
    requestCommand =>
      requestCommand.getMethod match {
        case Connector.Method.GET => http.Method.Get
        case Connector.Method.POST => http.Method.Post
        case Connector.Method.PUT => http.Method.Put
        case Connector.Method.DELETE => http.Method.Delete
      }
  }


}
