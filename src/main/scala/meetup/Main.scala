package meetup

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.{RoundRobinGroup, RoundRobinPool, Router}
import com.typesafe.config.ConfigFactory
import meetup.airports.{Barajas, JFK}
import meetup.planes.{Iberia, Panam}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App {

  //  val system = ActorSystem.create("Actor_system")

  val system = ActorSystem.create("actor-system", ConfigFactory.parseString(
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
      |    parallelism-max = 4
      |  }
      |  # Throughput defines the maximum number of messages to be
      |  # processed per actor before the thread jumps to the next actor.
      |  # Set to 1 for as fair as possible.
      |  throughput = 10
      |}
    """.stripMargin))


  private val barajasRef: ActorRef =
    system.actorOf(Props[Barajas]
      .withDispatcher("my-dispatcher")
      .withRouter(RoundRobinPool(2)),"barajas_actor")

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
