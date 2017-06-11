package stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}

/**
  * Created by pabloperezgarcia on 11/06/2017.
  *
  * Using Graph is a really cool way to create DSLs and use it for implement architectures with compensations
  * such as distributed sagas.
  *
  * You just need to use the GraphDSL.Builder to use it to glue your partialFunctions, where you will introduce
  * your business logic. And then depending on the numeric output of that logic, we will create new flows of execution
  * of your code.
  *
  * All the operators created, Source, Flow and Sink can be plugged together in this DSL using ~> <~ in any direction.
  */
object Graphs extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

    def isNumericFunction(x: Any): Int = {
      x match {
        case s: String if s.isEmpty => 0
        case _: String => 1
        case _ => 2
      }
    }

  def checkWordFunction(x: String): Int ={
    x match {
      case s:String if s.contains("-") =>1
      case s:String if s.contains("*") =>2
      case _ => 0
    }
  }


  val runnableGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val list = Source(List("hello", 1, "*akka*", "-graph-", "*world*", 2, "",  4, "!"))

    val printWarning = Sink.foreach[Any](x => println(s"Dont use empty man!"))

    val printWord = Sink.foreach[Any](x => println(s"######## $x"))

    val printNumber = Sink.foreach[Any](println)

    val toUpperCase = Flow[Any].map(_.asInstanceOf[String].toUpperCase)

    val toLowerCase = Flow[Any].map(_.asInstanceOf[String].toLowerCase)

    val removeCharacter = Flow[String].map(_.replace("*", "|"))

    val isNumeric = builder.add(Partition[Any](3, isNumericFunction))

    val contains = builder.add(Partition[String](3, checkWordFunction))

    list ~> isNumeric
            isNumeric.out(0) ~> printWarning
            isNumeric.out(1) ~> toUpperCase ~> contains
            isNumeric.out(2) ~> printNumber
                                               contains.out(0) ~> printWord
                   printWord <~ toLowerCase <~ contains.out(1)
                                               contains.out(2) ~> removeCharacter ~> printWord

    ClosedShape
  })


  runnableGraph.run()
}
