package stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Attributes, Materializer}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("Main")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val mainRoute = new MainRoute(new DataProcessor)

  Http().bindAndHandle(mainRoute.route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
}


class MainRoute(dataProcessor: DataProcessor) {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def route(): Route = post {
    entity(asSourceOf[DataChunk]) { source =>
      val result: Future[String] = dataProcessor.runDataSource(source)
      complete(result)
    }
  }
}

class DataProcessor(implicit system: ActorSystem, materializer: Materializer) {


  def runDataSource(source: Source[DataChunk, NotUsed]): Future[String] = {
    // This is needed for the last `map` method execution
    implicit val ec: ExecutionContext = system.dispatcher

    val initialCount = 0
    val countElement = (currentCount: Int, _: DataChunk) => currentCount + 1

    source
      .log("received").withAttributes(
      Attributes.logLevels(onElement = Logging.InfoLevel))
      // count the number of elements (chunks)
      .runFold(initialCount)(countElement)
      // map the materialized value
      .map { count => s"You sent $count chunks" }
  }
}

case class DataChunk(id: Int, data: String)

object DataChunk {
  implicit val dataChunkJsonFormat: RootJsonFormat[DataChunk] = jsonFormat2(DataChunk.apply)
}
