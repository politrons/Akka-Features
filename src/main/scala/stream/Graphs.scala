package stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}

/**
  * Created by pabloperezgarcia on 11/06/2017.
  */
object Graphs extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  //  def partitionFunction(i: Int): Int = if (i % 2 == 0) 0 else 1
  //
  //  val runnableGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
  //    import GraphDSL.Implicits._
  //
  //    val in = Source(1 to 10)
  //
  //    val out = Sink.foreach[Int](println)
  //
  //    val addOne = Flow[Int].map(_ + 1)
  //
  //    val partition = builder.add(Partition[Int](2, partitionFunction))
  //
  //    val merge = builder.add(Merge[Int](2))
  //
  //    in ~> merge ~> partition
  //    partition.out(0) ~> addOne ~> merge
  //    partition.out(1) ~> out
  //
  //    ClosedShape
  //  })

  def partitionFunction2(x: Any): Int = if (x.isInstanceOf[String]) 0 else 1

  val r = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val in = Source(List("a", 1, "c", "f", 2, "k", 3,"o","k"))

    val out = Sink.foreach[Any](println)

    val toUpperCase = Flow[Any].map(_.asInstanceOf[String].toUpperCase)

    val partition = builder.add(Partition[Any](2, partitionFunction2))

    val merge = builder.add(Merge[Any](2))

                  in ~> merge ~> partition
    partition.out(0) ~> toUpperCase ~> merge
    partition.out(1) ~> out

    ClosedShape
  })


  r.run()
}
