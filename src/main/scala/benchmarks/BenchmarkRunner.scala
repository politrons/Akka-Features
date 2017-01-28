package benchmarks

import akka.actor.{ActorRef, ActorSystem, Props, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
object BenchmarkRunner extends App {

  implicit val context = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(5 seconds)

  run()

  def run() {

    context.mailboxes.deadLetterMailbox

    val actorTask: ActorRef = context.actorOf(Props(new ActorTask()), name = "workerRouter")

    0 to 10 foreach (_ => actorTask ! "add")

    val future = actorTask ? "size"
    val size = Await.result(future, timeout.duration).asInstanceOf[Int]

    println(s"Total number of tasks:$size")

//    actorTask ! "delete"
  }

}