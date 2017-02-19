package http

/**
  * Created by pabloperezgarcia on 06/02/2017.
  */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.parsing.json.JSON

object WebServer extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val localhost = "localhost"
  val port = 8080
  var order = Map[String, String]()

  initializeService

  /**
    * Akka http provide the bindAndHandler which allow you to bind using a dsl some endpoints in a specific address/port
    *
    */
  private def initializeService = {
    val serverFuture = Http().bindAndHandle(routes, localhost, port)
    serverFuture.onComplete(_ => println(s"Server online at http://$localhost:$port/\nPress RETURN to stop..."))
    StdIn.readLine() // let it run until user presses return
    shutdownService(serverFuture)
  }

  /**
    * Trigger unbinding from the port and shutdown when done
    *
    * @param bindingFuture
    */
  private def shutdownService(bindingFuture: Future[ServerBinding]) = {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ ⇒ system.terminate())
  }

  /**
    * Akka http provide a dsl to create the endpoints for the rest service
    * The ~ character pass the request form one route to the next one in the chain.
    */
  private def routes = {
    path("version") {
      get {
        complete(getVersionResponse)
      }
    } ~
      path("order") {
        get {
          entity(as[String]) { id =>
            complete(getOrderResponse(id))
          }
        } ~
          post {
            entity(as[String]) { order =>
              val jsonOrder = JSON.parseFull(order)
              onComplete(saveOrder(getOrderBody(jsonOrder))) { _ =>
                complete(getPostResponse)
              }
            }
          }
      }
  }

  private def getOrderBody(jsonOrder: Option[Any]) = {
    jsonOrder.get.asInstanceOf[Map[String, String]]
  }

  def saveOrder(order: Map[String, String]): Future[Map[String, String]] = {
    this.order = this.order ++ getBodyMap(order)
    Future.apply(order)
  }

  private def getBodyMap(order: Map[String, String]) = {
    Map[String, String](order("id") -> order("product"))
  }

  private def getVersionResponse = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, "Akka-http version 1.0")
  }

  private def getOrderResponse(id: String) = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Get order ${order(id)}")
  }

  private def getPostResponse = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Order stored")
  }
}
