package http

/**
  * Created by pabloperezgarcia on 06/02/2017.
  */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn

object WebServer extends App{

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  initializeService

  private def initializeService = {
    val bindingFuture = Http().bindAndHandle(initializeRoutes, "localhost", 8080)
    bindingFuture.onComplete(_ => println(s"Server online at http://localhost:8080/\nPress RETURN to stop..."))
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }

  private def initializeRoutes = {
    path("hello") {
      get {
        complete(getHelloResponse)
      }
    }
  }

  private def getHelloResponse = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>")
  }

}
