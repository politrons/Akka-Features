package stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.junit.Test

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

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
  * Flow -> Which can be used to emit just 1 element in the pipeline, normally used by a Source with "via"
  * Sink -> which it will be the subscriber to the Source/Flow to consume the items emitted.
  */
class AkkaStream {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  @Test def sourceToSink(): Unit = {
    val sink = Sink.foreach(println)

    val source = Source(List("Akka", "Kafka", "Afakka", "Kakfta"))
      .alsoTo(Sink.foreach(s => println(s"Input:$s")))
      .map(syncVal)
      .filter(_.contains("AKKA"))

    source to sink run()
  }

  /**
    * Flow is normally the glue between Source and sink, in Flow we define some operators where our items will pass over
    * the pipeline. Making in this way, we can define some independent flows to be reused and avoid break DRY
    * It´s even possible zip multiple flows in the same pipeline.
    */
  @Test def flow(): Unit = {
    val doubleFlow = Flow[Int]
      .map(_ * 2)
      .filter(value => value < 10)
    Await.ready(Source(0 to 10)
      .via(doubleFlow)
      .runWith(Sink.foreach(value => println(value))), 5 seconds)
  }

  /**
    * Using mapAsync operator, we pass a function which return a Future, the number of parallel run futures will
    * be determine by the argument passed to the operator.
    */
  @Test def mapAsync(): Unit = {
    Source(0 to 10)
      .mapAsync(2) { value =>
        implicit val ec: ExecutionContext = ActorSystem().dispatcher
        Future {
          Thread.sleep(500)
          println(s"Process in Thread:${Thread.currentThread().getName}")
          value
        }
      }
      .runWith(Sink.foreach(value => println(s"Item emitted:$value in Thread:${Thread.currentThread().getName}")))
  }

  /**
    * FlatMapConcat works like flatMap in Rx, it create a new Line of execution(Source) and once it finish
    * It flat all the result to be emitted in the pipeline.
    */
  @Test def flatMap(): Unit = {
    Await.ready(Source(0 to 10)
      .flatMapConcat(value => Source.single(value)
        .map(value => value * 10))
      .runForeach(item => println(s"Item:$item")), 5 seconds)
  }

  var sources: Seq[Source[Char, NotUsed]] = Seq(Source("hello"), Source("scala"), Source("world"))

  @Test def zipWith(): Unit = {


  }


  /**
    * Tick operator is like interval in Rx it will repeat the emittion of item with an initial delay
    * and an internal delay
    */
  @Test def tick(): Unit = {
    Source.tick(0 seconds, 1 seconds, "Tick")
      .map(value => value.toUpperCase)
      .to(Sink.foreach(value => println(s"item emitted:$value")))
      .run()
  }

  /**
    * Create a `Source` that will continually emit the given element.
    *
    * Delay operator will delay the emittion of the item in the pipeline the time specify in the operator
    */
  @Test def repeat(): Unit = {
    Source.repeat("Repeat")
      .delay(500 millisecond)
      .map(value => value.toUpperCase)
      .to(Sink.foreach(value => println(s"item emitted:$value")))
      .run()
  }

  /**
    * TakeWhile operator will emitt items while the predicate function return true.
    * You can achieve the same result with filter+take operators
    */
  @Test def takeWhile(): Unit = {
    Await.result(Source(0 to 10)
      .takeWhile(n => n < 5)
      .runForeach(value => println(s"Item emitted:$value"))
      , 5 seconds)
  }

  /**
    * DropWhile operator will drop items while the predicate function return true.
    * You can achieve the same result with filter+drop operators
    */
  @Test def dropWhile(): Unit = {
    Await.result(Source(0 to 10)
      .dropWhile(n => n < 5)
      .runForeach(value => println(s"Item emitted:$value"))
      , 5 seconds)
  }

  /**
    * Group the emission of items in groups define in the operator, in this case since start in 0
    * and we group per 5 items we should end up having 3 collections.
    */
  @Test def group(): Unit = {
    Await.result(Source(0 to 10)
      .grouped(5)
      .runForeach(value => println(s"Item emitted:$value"))
      , 5 seconds)
  }

  /**
    * When receive an item create a collection with the item emitted and wait to collect the number of
    * items specify in the operator.
    * In this case since we specify 5 we should print:
    * (0, 1, 2, 3, 4)
    * (1, 2, 3, 4, 5)
    * (2, 3, 4, 5, 6)
    * (3, 4, 5, 6, 7)
    * (4, 5, 6, 7, 8)
    * (5, 6, 7, 8, 9)
    * (6, 7, 8, 9, 10)
    */
  @Test def sliding(): Unit = {
    Await.result(Source(0 to 10)
      .sliding(5)
      .runForeach(value => println(s"Item emitted:$value"))
      , 5 seconds)
  }

  /**
    * Scan operator get the item emitted and it´s added into a collection to be passed in every emission.
    */
  @Test def scan(): Unit = {
    Await.ready(Source(0 to 10)
      .scan(List[Int]())((list, item) => list.::(item))
      .runForeach(list => println(s"List:$list")), 5 seconds)
  }

  val start = System.nanoTime()

  /**
    * Intersperse operator allow you the possibility to attach items in the emission of items in the pipeline
    * Here for instance we add an item at the beginning of the emission, per item emitted and once we finish
    */
  @Test def intersperse(): Unit = {
    Await.ready(Source(0 to 10)
      .map(_.toString)
      .intersperse("Start", separatorFunction.apply(), "End")
      .runForeach(list => println(s"List:$list")), 5 seconds)
  }

  private def separatorFunction = {
    () => "******************+"
  }

  def syncVal: String => String = {
    s => s.toUpperCase
  }
}
