package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
  * Created by pabloperezgarcia on 25/01/2017.
  */
object Fow extends App {

  implicit val context = ActorSystem()
  implicit val materializer = ActorMaterializer()

  run()

  def run(): Unit = {
//    val fetchStrings: Flow[String, String] =
//      Flow.("A")
//        .map(value => value.toUpperCase)

  }

}
