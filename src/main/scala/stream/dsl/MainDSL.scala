package stream.dsl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Flow, Partition, Sink, Source}


class MainDSL {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  var builder: Builder[NotUsed] = _

  def init(b: Builder[NotUsed]) = builder = b

  def Given(sentence: String) = Source.single(sentence.toUpperCase)

  def When(sentence: String) = Flow[String].map(x => x.replace("_", " "))

  def Then(sentence: String) = Sink.foreach[String](x => println(s"######## $x"))

  def AndThen(sentence: String) = Flow[String].map(x => x)

  lazy val \ = builder.add(Partition[String](1, s => 0))

}
