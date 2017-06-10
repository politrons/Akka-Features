package stream.workshop

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.Await

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pabloperezgarcia on 08/04/2017.
  *
  * A really easy way to implement a client without almost any code
  * The Service class will receive and response a Future[Response] the type that you specify
  * Service[Req,Rep]
  */
object HttpClient extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def client: Service[Request, Response] = Http.newService("localhost:1981")

  def request = http.Request(http.Method.Get, "/")

  type pairResponse = (Int, Response)

  //  Source.tick(0 seconds, 100 millisecond, "Tick")
  Source(0 to 10)
    .via(requestFlow)
    .to(Sink.foreach(pair => println(s"Number ${pair._1} ${pair._2}")))
    .run()

  def requestFlow = Flow[Int]
    .map(reqNumber => {
      println(s"Request $reqNumber")
      reqNumber
    })
    .mapAsync[pairResponse](10) {
    resNumber =>
      scala.concurrent.Future(resNumber, Await.result(client(request)))
  }
}
