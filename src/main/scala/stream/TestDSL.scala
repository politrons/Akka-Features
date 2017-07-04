package stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
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
object TestDSL extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def Given(sentence: String) = Source.single(sentence.toUpperCase)

  def When(sentence: String) = Flow[Any].map(x => x.asInstanceOf[String].replace("_", " "))

  def Then(sentence: String) = Sink.foreach[String](x => println(s"######## $x"))

  import GraphDSL.Implicits._

  val runnableGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

    Given("This_is_a_hello_world") ~> When(s"I change character _") ~> Then("I expect to receive 200")

    ClosedShape
  })

  runnableGraph.run()
}
