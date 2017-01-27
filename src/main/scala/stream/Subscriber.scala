package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.reactivestreams.Subscriber

/**
  * Created by pabloperezgarcia on 26/01/2017.
  */
object Subscriber extends App {

  implicit val context = ActorSystem()
  implicit val materializer = ActorMaterializer()

  runSubscriber()

  def runSubscriber() {
    Source.single("Akka subscription!")
      .to(Sink.fromSubscriber(createSubscriber()))
      .run()
  }

  /**
    * create Reactive Streams Subscriber:
    * We create the source as subscriber and sink to get the lazy subscriber
    * Once that we create the Source with value and we set the subscriber with "to" we start
    * using the pipeline
    * @return
    */
  private def createSubscriber():Subscriber[String] = {
    val source = Source.asSubscriber[String]
      .map(value => value.toUpperCase)
      .filter(value => !value.isEmpty)
    val sink = Sink.foreach(Console.println)
    source to sink run()
  }
}
