package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.twitter.finagle.http
import com.twitter.util.{Await, Future}

/**
  * Created by pabloperezgarcia on 12/08/2017.
  */
object AkkaAsync extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def request = http.Request(http.Method.Get, "/")

  Source(0 to 10)
    .via(flow)
    .to(Sink.foreach(pair => println(s"Response in Thread: ${Thread.currentThread().getName}:${pair._1}")))
    .run()

  def flow = Flow[Int]
    .flatMapMerge(5, resNumber => {
      Source.single(processExecution())
        .map(future => processResponse(resNumber, future))
    })

  private def processResponse(resNumber: Int, future: Future[String]) = (resNumber, Await.result(future))

  private def processExecution(): Future[String] = {
    val future = Future[String] {
      System.out.println("Execution in " + Thread.currentThread.getName)
      try {
        Thread.sleep(1000)
        "done"
      } catch {
        case e: InterruptedException =>
          e.printStackTrace()
          "wrong"
      }
    }

    future
  }


}
