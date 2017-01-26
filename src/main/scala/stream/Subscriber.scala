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
    val subscriberSource = Source.asSubscriber[Boolean]
    val someFunctionSink = Sink.foreach(Console.println)

    val flow = subscriberSource.to(someFunctionSink)

    //create Reactive Streams Subscriber
    val subscriber: Subscriber[Boolean] = flow.run()

    //prints true
    Source.single(true).to(Sink.fromSubscriber(subscriber)).run()
  }
}
