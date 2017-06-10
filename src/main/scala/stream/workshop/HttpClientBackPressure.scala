package stream.workshop

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

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

  def request = http.Request(http.Method.Get, s"/")

  Source(0 to 10)
    .via(requestFlow)
    .to(Sink.foreach(pair => println(s"Response number ${pair._1} value ${pair._2}")))
    .run()

  def requestFlow = Flow[Int]
//    .groupedWithin(5, 20 seconds)
    .map(resNumber => {
      println(s"Request $resNumber")
      resNumber
    })
    .flatMapMerge(10, resNumber => Source.single(client(request))
      .map(future => processResponse(resNumber, future)))

  private def processResponse(resNumber: Int, future: Future[Response]) = {
    val response = Await.result(future)
    println(s"$response processed, waiting for merge.....")
    (resNumber, response)
  }


//  /**
//    * GroupedWithin allow you group emission of items by a specific number of items or by a window time
//    */
//  @Test def groupedWithin(): Unit = {
//    Await.ready(Source(0 to 10)
//      .map(_.toString)
//      .groupedWithin(3, 5 millisecond)
//      .runForeach(list => println(s"List:$list")), 5 seconds)
//  }

}
