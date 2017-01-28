package benchmarks

import java.util.UUID

import akka.actor.Actor
import akka.agent.Agent
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by pabloperezgarcia on 27/01/2017.
  */
class ActorTask extends Actor {

  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(5 seconds)


  val agent: Agent[Map[String, String]] = Agent(Map[String, String]())

  override def receive: Receive = {
    case "add" =>
      addTask(UUID.randomUUID().toString)
    case "delete" =>
      deleteTasks()
    case "size" =>
      sender ! Await.result(agent.future(), timeout.duration).size
  }

  def addTask(task: String) {
    agent.alter(Await.result(agent.future(), timeout.duration) ++ Map(task -> "test"))
      .onComplete(_ => println(s"Number of tasks ${Await.result(agent.future(), timeout.duration).size}"))
  }

  def deleteTasks(): Unit = {
    val map = agent.get()
    val newMap = map.toStream
      .map(entry => map - entry._1)
      .scan(Map())((map1, map2) => map1 ++ map2).last
    agent.send(newMap)
  }

}

