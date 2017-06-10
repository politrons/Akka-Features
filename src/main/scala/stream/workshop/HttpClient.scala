package stream.workshop

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.Await

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

  val client: Service[Request, Response] = Http.newService("localhost:1981")
  val request = http.Request(http.Method.Get, "/")

  //  Source.tick(0 seconds, 100 millisecond, "Tick")
  Source(0 to 10)
    .via(requestFlow)
    .to(Sink.foreach(pair => println(s"Response number ${pair._1} value ${pair._2}")))
    .run()

  def requestFlow = Flow[Int]
    .map(resNumber => {
      println(s"Request $resNumber")
      request.params
      resNumber
    })
    .flatMapMerge(10, resNumber => Source.single(client(request))
      .map(future => (resNumber, Await.result(future))))

}
