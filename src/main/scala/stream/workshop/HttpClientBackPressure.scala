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
object HttpClientBackPressure extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def client: Service[Request, Response] = Http.newService("localhost:1981")

  def request(reqNumber: Int) = http.Request(http.Method.Get, s"/&reqNumber=$reqNumber")

  Source(0 to 10)
    .via(requestFlow)
    .to(Sink.foreach(pair => println(s"Response number ${pair._1} value ${pair._2}")))
    .run()

  def requestFlow = Flow[Int]
    .map(resNumber => {
      println(s"Request:$resNumber")
      resNumber
    })
    .flatMapMerge(10, reqNumber => Source.single(client(request(reqNumber)))
      .map(future => (reqNumber, Await.result(future))))

}
