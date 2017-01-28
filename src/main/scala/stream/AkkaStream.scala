package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

/**
  * Created by pabloperezgarcia on 25/01/2017.
  *
  * Akka stream provide all the operators and features that other reactive libraries provide as ReactiveX or Flow
  * Having those feature we can not only consume the message in a much better way but also and really important
  * have back-pressure mechanism which will prevent OutOfMemory problems in our system
  *
  * In Akka Stream we have three elements.
  *
  * Source -> Which we use the create a source with an element to emit in the pipeline
  * Flow -> Which can be used to emit just 1 element in the pipeline
  * Sink -> which it will be the subscriber to the Source/Flow to consume the items emited.
  */
object AkkaStream extends App {

  implicit val context = ActorSystem()
  implicit val materializer = ActorMaterializer()

  run()

  def run(): Unit = {
    val sink = Sink.foreach(println)

    val source = Source(List("Akka", "Kafka", "Afakka", "Kakfta"))
      .map(syncVal)
      .filter(_.contains("AKKA"))

    source to sink run()

  }

  def syncVal: String => String = {
    s => s.toUpperCase
  }
}
