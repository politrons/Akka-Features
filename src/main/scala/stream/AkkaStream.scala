package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

/**
  * Created by pabloperezgarcia on 25/01/2017.
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
