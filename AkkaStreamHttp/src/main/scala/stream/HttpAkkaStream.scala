package stream

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Attributes}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("Main")
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")

  def routes: Route = {

    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
    // This is needed for the last `map` method execution
    implicit val ec: ExecutionContext = system.dispatcher

    val initialCount = 0
    val countElement: (Int, DataChunk) => Int = (currentCount: Int, _: DataChunk) => currentCount + 1

    post {
      entity(asSourceOf[DataChunk]) { source =>
        val program: Future[String] = {
          source
            .log("received").withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
            .runFold(initialCount)(countElement)
            // map the materialized value
            .map { count => s"You sent $count chunks" }
        }
        complete(program)
      }
    }
  }
}


case class DataChunk(id: Int, data: String)

object DataChunk {
  implicit val dataChunkJsonFormat: RootJsonFormat[DataChunk] = jsonFormat2(DataChunk.apply)
}
