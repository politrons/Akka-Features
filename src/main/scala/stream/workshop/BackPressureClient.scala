package stream.workshop

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

/**
  * Created by pabloperezgarcia on 08/04/2017.
  */
object BackPressureClient extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def client: Service[Request, Response] = Http.newService("localhost:1981")

  def request = http.Request(http.Method.Get, "/")

  Source(0 to 20)
    .via(flow)
    .to(Sink.foreach(pair => println(s"#################### Response message:${pair._1} status:${pair._2.statusCode}")))
    .run()

  def flow = Flow[Int]
    .map(message => {
      println(s"Sending message $message")
      message
    })
    .flatMapMerge(4, resNumber => Source.single(client(request))
      .map(future => processResponse(resNumber, future)))

  private def processResponse(resNumber: Int, future: Future[Response]) = (resNumber, Await.result(future))

}
