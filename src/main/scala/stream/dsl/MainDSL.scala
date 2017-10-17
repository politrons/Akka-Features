package stream.dsl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Flow, Partition, Sink, Source}


class MainDSL {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  //    val in = builder.add(Flow[String].map(x => x)).in
  //    val out = builder.add(Flow[String].map(x => x)).out

  type message = (String, String)

  var builder: Builder[NotUsed] = _

  def init(b: Builder[NotUsed]) = builder = b

  def Given(sentence: String) = Source.single(new message(sentence.toUpperCase, " "))

  def When(sentence: String) = Flow[message].map(x => x._1.replace("_", x._2))

  def Then(sentence: String) = Sink.foreach[String](x => println(s"######## $x"))

  def AndThen(sentence: String) = Flow[String].map(x => x)

  lazy val \ = builder.add(Partition[String](1, s => 0))

}
