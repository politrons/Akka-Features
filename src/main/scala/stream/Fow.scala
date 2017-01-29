package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.junit.Test

/**
  * Created by pabloperezgarcia on 25/01/2017.
  */
class Fow {

  implicit val context = ActorSystem()
  implicit val materializer = ActorMaterializer()

  /**
    * Flow are just pipeline where has 1 input item, and emitt 1 output item
    * We just need to specify the type of item that we will received, and then
    * we can use all operators that Akka stream api provide
    */
  @Test def mainFlow(): Unit = {
    val increase = Flow[Int]
      .map(value => value * 10)
    val filterFlow = Flow[Int]
      .filter(value => value > 50)
      .take(2)
    Source(0 to 10)
      .via(increase)
      .via(filterFlow)
      .to(Sink.foreach(value => println(s"Item emitted:$value")))
      .run()
  }

}
