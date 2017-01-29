package stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.junit.Test

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

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
  * Flow -> Which can be used to emit just 1 element in the pipeline
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
      .to(Sink.foreach(value => println(s"Item emitted:$value in Thread:${Thread.currentThread().getName}")))
      .run()
  }

  /**
    * TakeWhile operator will emitt items while the predicate function return true.
    * You can achieve the same result with filter+take operators
    */
  @Test def takeWhile(): Unit = {
    Source(0 to 10)
      .takeWhile(n => n < 5)
      .to(Sink.foreach(value => println(s"item emitted:$value")))
      .run()
  }

  def syncVal: String => String = {
    s => s.toUpperCase
  }
}
