package meetup

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import meetup.airports.Barajas
import meetup.planes.{Iberia, Panam}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Main extends App {

  //  val system = ActorSystem.create("Actor_system")

  val system = ActorSystem.create("my-actor-system", ConfigFactory.parseString(
    """
      |my-dispatcher {
      |  # Dispatcher is the name of the event-based dispatcher
      |  type = Dispatcher
      |  # What kind of ExecutionService to use
      |  executor = "fork-join-executor"
      |  # Configuration for the fork join pool
      |  fork-join-executor {
      |    # Min number of threads to cap factor-based parallelism number to
      |    parallelism-min = 2
      |    # Parallelism (threads) ... ceil(available processors * factor)
      |    parallelism-factor = 2.0
      |    # Max number of threads to cap factor-based parallelism number to
      |    parallelism-max = 10
      |  }
      |  # Throughput defines the maximum number of messages to be
      |  # processed per actor before the thread jumps to the next actor.
      |  # Set to 1 for as fair as possible.
      |  throughput = 100
      |}
      |my-mailbox {
      |  mailbox-type = "akka.dispatch.NonBlockingBoundedMailbox"
      |  mailbox-capacity = 2
      |}
    """.stripMargin))


  private val barajasRef: ActorRef =
    system.actorOf(Props[Barajas]
      .withDispatcher("my-dispatcher")
      .withMailbox("my-mailbox")
      .withRouter(RoundRobinPool(4)), "barajas_actor")

  //Dead letter subscriber
  val barajasDeadLetter = system.actorOf(Props[Barajas],"deadletter-barajas")
  system.eventStream.subscribe(barajasDeadLetter, classOf[DeadLetter])

  //Ask pattern
  implicit val timeout = Timeout(10 seconds)
  private val future: Future[Any] = barajasRef ? "future_message"
  val eventualString = future.mapTo[String]
  println(Await.result(eventualString, 10 seconds))

  barajasRef ! "open"

  Future {
    0 to 10 foreach (_ => {
      barajasRef ! Iberia(100, System.currentTimeMillis())
    })
  }

  Future {
    0 to 10 foreach (_ => {
      barajasRef ! Panam(100, System.currentTimeMillis())
    })
  }

  //  system.actorOf(Props.create(classOf[JFK],barajasRef),"jfk") ! "ping"

  println("Baraja airport open")

}
