package stream.dsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}


class MainDSL {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def Given(sentence: String) = Source.single(sentence.toUpperCase)

  def When(sentence: String) = Flow[Any].map(x => x.asInstanceOf[String].replace("_", " "))

  def Then(sentence: String) = Sink.foreach[String](x => println(s"######## $x"))


}
