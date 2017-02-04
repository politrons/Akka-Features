package stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.junit.Test
import org.reactivestreams.Subscriber

import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * Created by pabloperezgarcia on 28/01/2017.
  *
  * Back pressure refers to pressure opposed to the desired flow of items in a pipe.
  * It is often caused by long process that normally makes the pipe start having more input items that can process out.
  */
class BackPressure {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  @Test
  def runBuffer() = {
    runSubscriber(buffer())
  }

  @Test
  def runBufferDropHead() {
    runSubscriber(bufferDropHead())
  }

  @Test
  def runBufferDropTail() {
    runSubscriber(bufferDropTail())
  }

  def runSubscriber(function: () => Subscriber[Int]) {
    Source(1 to 10)
      .to(Sink.fromSubscriber(function.apply()))
      .run()
  }

  /**
    * With back-pressure mechanism if the buffer is full will make the publisher stop emitting elements until
    * the buffer limit goes down.
    * Shall print:
    * 0 to 100
    */
  def buffer(): () => Subscriber[Int] = {
    () =>
      val source = Source.asSubscriber[Int]
        .buffer(1, OverflowStrategy.backpressure)
        .map(value => value)
      val sink = Sink.foreach(Console.println)
      source to sink run()
  }

  /**
    * If the buffer is full delete the oldest element in the buffer
    * Shall print:
    * 40 to 100
    */
  def bufferDropHead(): () => Subscriber[Int] = {
    () =>
      val source = Source.asSubscriber[Int]
        .buffer(1, OverflowStrategy.dropHead)
        .map(value => value)
      val sink = Sink.foreach(Console.println)
      source to sink run()
  }

  /**
    * If the buffer is full delete the youngest element in the buffer
    * Shall print:
    * 40 to 100
    */
  def bufferDropTail(): () => Subscriber[Int] = {
    () =>
      val source = Source.asSubscriber[Int]
        .buffer(1, OverflowStrategy.dropTail)
        .map(value => value)
      val sink = Sink.foreach(Console.println)
      source to sink run()
  }

  /**
    * GroupedWithin allow you group emission of items by a specific number of items or by a window time
    */
  @Test def groupedWithin(): Unit = {
    Await.ready(Source(0 to 10)
      .map(_.toString)
      .groupedWithin(3, 5 millisecond)
      .runForeach(list => println(s"List:$list")), 5 seconds)
  }


}

