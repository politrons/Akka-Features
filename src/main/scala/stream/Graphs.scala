package stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}

/**
  * Created by pabloperezgarcia on 11/06/2017.
  */
object Graphs extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def partitionFunction(x: Any): Int = if (x.isInstanceOf[String]) 0 else 1

  def wordContainsCharacter(x: String): Int = if (x.contains("*")) 0 else 1


  val runnableGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val in = Source(List("hello", 1, "*akka*", "graph", "*world*", 2, 3, 4, "!"))

    val log = Sink.foreach[Any](x => println(s"######## $x"))

    val out = Sink.foreach[Any](println)

    val toUpperCase = Flow[Any].map(_.asInstanceOf[String].toUpperCase)

    val removeCharacter = Flow[String].map(_.replace("*", ""))

    val isNumeric = builder.add(Partition[Any](2, partitionFunction))

    val contains = builder.add(Partition[String](2, wordContainsCharacter))

    in ~> isNumeric
          isNumeric.out(0) ~> toUpperCase ~> contains
          isNumeric.out(1) ~> out
                                             contains.out(0) ~> removeCharacter ~> log
                                             contains.out(1) ~> log

    ClosedShape
  })


  runnableGraph.run()
}
