package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.junit.Test

import scala.collection.immutable._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by pabloperezgarcia on 25/01/2017.
  *
  * Akka stream provide all the operators and features that other reactive libraries provide as ReactiveX or Flow
  * Having those feature we can not only consume the message in a much better way but also and really important
  * have back-pressure mechanism which will prevent OutOfMemory problems in our system
  *
  * In Akka Stream we have three elements:
  * [Source]~>[Flow]~>[Sink]
  *
  * Source: Origin of data, it could be multiple inputs
  * Flow: This part of the stream is where we transform the data that we introduce in our pipeline
  * Sink: This is where we receive all data once that has been processed in order to be printed,
  * passed to another source and so once with "via"
  */
class AkkaStream {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  @Test def sourceToFlowToSink(): Unit = {

    val source = Source(List("Akka", "Kafka", "Afakka", "Kakfta"))

    val flow = Flow[String]
      .map(word => word.toUpperCase)
      .filter(word => word.contains("AKKA"))

    val sink = Sink.foreach(println)

    source via flow to sink run()

    //Just to wait for the end of the execution
    Thread.sleep(1000)
  }

  /**
    * Flow is normally the glue between Source and sink, in Flow we define some operators where our items will pass over
    * the pipeline. Making in this way, we can define some independent flows to be reused and avoid break DRY
    * It´s even possible zip multiple flows in the same pipeline.
    */
  @Test def flow(): Unit = {
    val doubleFlow = Flow[Int]
      .map(_ * 2)
      .filter(_ < 10)
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
    * FlatMapConcat works like flatMap in Rx but sequentially since concat one emition to the next,
    * it create a new Line of execution(Source) and once it finish
    * It flat all the result to be emitted in the pipeline.
    */
  @Test def flatMapConcat(): Unit = {
    Await.ready(Source(0 to 10)
      .flatMapConcat(value => Source.single(value)
        .map(value => value * 10))
      .runForeach(item => println(s"Item:$item")), 5 seconds)
  }

  /**
    * FlatMapMerge works like flatMap in Rx all source run in parallel.
    * Once that finish all source flatMap merge all results.
    * it create a new Line of execution(Source) and once it finish
    * It flat all the result to be emitted in the pipeline.
    */
  @Test def flatMapMerge(): Unit = {
    Await.result(Source(0 to 10)
      .via(requestFlow)
      .runForeach(res => println(s"Item emitted:$res")), 5 seconds)

    def requestFlow = Flow[Int]
      .flatMapMerge(10, resNumber => Source.single(resNumber)
        .map(res => res * 100))
  }

  /**
    * Zip operator allow you to merge together into a Vector multiple Sources
    */
  @Test def zipWith(): Unit = {
    val sources: Seq[Source[Char, _]] = Seq(Source("h"), Source("e"), Source("l"), Source("l"), Source("o"))
    Await.ready(Source.zipN[Char](sources)
      .map(vector => vector.scan(new String)((b, b1) => mergeCharacters(b, b1)).last).async
      .runForeach(word => println(word)), 5 seconds)
  }

  private def mergeCharacters(b: Any, b1: Any) = {
    b.asInstanceOf[String].concat(b1.asInstanceOf[Char].toString)
  }

  /**
    * Adding async operator after another operator mark that all previous operator of the pipeline
    * must be executed in another thread, then the sink(subscriber) will consume the events in the main thread.
    */
  @Test def async(): Unit = {
    val runWith = Source(0 to 10)
      .map(value => {
        println(s"Execution 1 ${Thread.currentThread().getName}")
        value * 100
      })
      .map(value => {
        println(s"Execution 2 ${Thread.currentThread().getName}")
        value * 100
      }).async
      .runWith(Sink.foreach(value => println(s"Item emitted:$value in Thread:${Thread.currentThread().getName}")))
    runWith

    Thread.sleep(10000)
  }

  /**
    * The way to get the value of the source onces start emitting (run) it´s made by lazy operator, which inside
    * we need to pass a function with the Source that we want to create
    */
  @Test def defer(): Unit = {
    var range = 0 to 10
    val graphs = Source.lazily(() => Source(range))
      .to(Sink.foreach(println))
    range = 10 to 20
    graphs.run()
    Thread.sleep(2000)
  }

  /**
    * Tick operator is like interval in Rx it will repeat the emittion of item with an initial delay
    * and an internal delay
    */
  @Test def tick(): Unit = {
    val runnableGraph = Source.tick(0 seconds, 1 seconds, "Tick")
      .map(value => value.toUpperCase)
      .to(Sink.foreach(value => println(s"item emitted:$value")))
    runnableGraph.run()
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

  /**
    * Operator that allow us to redirect the flow to another sink but still continue with the flow of the stream
    */
  @Test def alsoTo(): Unit = {
    val sink = Sink.foreach(println)

    val flow = Flow[String]
      .alsoTo(Sink.foreach(s => println(s"Input:$s")))
      .map(s => s.toUpperCase)
      .filter(_.contains("AKKA"))

    val source = Source(List("Akka", "Kafka", "Afakka", "Kakfta"))

    source via flow to sink run()

    //Just to wait for the end of the execution
    Thread.sleep(1000)
  }

  /**
    * One of the most important things about akka stream is how easy is create a DSL for user than can be reuse in their
    * applications, we just need to provide some lego pieces and they just need to put it together
    */
  @Test def generateDSL(): Unit = {

    val year = 2017
    transactions via filterTransactionBefore(year) to report(s"Transaction before $year") run()

    //Just to wait for the end of the execution
    Thread.sleep(5000)
  }

  type Transaction = (Int, String)

  val transactions = Source(List((2014, "Coca-cola"),
    (2012, "Levis"),
    (2016, "Burrito"),
    (2008, "M&M"),
    (2005, "Playstation 2"),
    (2017, "Playstation 4")))

  def filterTransactionBefore(year: Int) = Flow[Transaction]
    .filter(transaction => transaction._1 < year)

  def report(name: String) = Sink.foreach[Transaction](transaction => println(s"Report $name ${transaction._2}"))

  private def separatorFunction = {
    () => "******************+"
  }

  /**
    * Using mapAsync operator, we pass a function which return a Future, the number of parallel run futures will
    * be determine by the argument passed to the operator.
    */
  @Test def readAsync(): Unit = {
    Source(0 to 10)//-->Your files
      .mapAsync(5) { value => //-> It will run in parallel 5 reads
        implicit val ec: ExecutionContext = ActorSystem().dispatcher
        Future {
          //Here read your file
          Thread.sleep(500)
          println(s"Process in Thread:${Thread.currentThread().getName}")
          value
        }
      }
      .runWith(Sink.foreach(value => println(s"Item emitted:$value in Thread:${Thread.currentThread().getName}")))
  }


}
