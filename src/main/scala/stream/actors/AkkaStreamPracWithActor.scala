package stream.actors

import java.util.UUID

import akka.NotUsed
import akka.actor._
import akka.pattern.ask
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnNext}
import akka.stream.actor.{ActorPublisher, ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.Timeout
import org.reactivestreams.Publisher

import scala.concurrent.Future
import scala.io.StdIn

private case class Event(value: String) extends AnyVal

private case object Finish

private object AkkaStreamPracWithActor extends App {

  import akka.stream.scaladsl._

  implicit val system = ActorSystem("akka-stream")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  class PublishActor extends ActorPublisher[Event] {
    // publish [[Letter]] or OnComplete
    override def receive: Actor.Receive = {
      case message: String =>
        onNext(Event(message))
      case Finish =>
        onComplete()
    }
  }

  class FlowActor extends Actor {
    // subscribe and publish
    override def receive: Actor.Receive = {
      case Event(msg) => sender() ! Event(msg)
      case any => println(s"??? => $any")
    }
  }

  class SubscribeActor extends ActorSubscriber {
    override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

    // just subscribe
    override def receive: Actor.Receive = {
      case OnNext(any) => println(s"subscribed: $any")
      case OnComplete => println(s"finish process!")
    }
  }

  // publisher actor
  val publisherActor = system.actorOf(Props[PublishActor])

  // source with actor
  val source: Source[Event, NotUsed] = {
    val publisher: Publisher[Event] = ActorPublisher(publisherActor)
    Source.fromPublisher(publisher)
  }

  // flow
  val flow: Flow[Event, Event, NotUsed] = {

    import scala.concurrent.duration._
    implicit val timeout: Timeout = 1.second

    val flowActor = system.actorOf(Props[FlowActor])

    def flowWithActor(event: Event): Future[Event] = (flowActor ? event).mapTo[Event]

    Flow[Event]
      .mapAsync[Event](3)(flowWithActor)
  }

  // another flow without actor
  val accumulater: Flow[Event, String, NotUsed] =
    Flow[Event].fold("init") { (acc, rep) => s"$acc :: ${rep.value}" }

  // sink with actor
  val sink: Sink[String, NotUsed] = {
    val subscriberActor = system.actorOf(Props[SubscribeActor])
    Sink.fromSubscriber[String](ActorSubscriber[String](subscriberActor))
  }

  // simple graph
  val _graph: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(source via flow via accumulater to sink)

  // written by DSL
  val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph {
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      source ~> flow ~> accumulater ~> sink
      ClosedShape
    }

  }

  graph.run

  Thread.sleep(100L)

  1 to 10 foreach { number =>
    publisherActor ! UUID.randomUUID().toString
  }

  publisherActor ! Finish

  println("push Enter to shutdown process.")

  StdIn.readLine()

  system.terminate()

}