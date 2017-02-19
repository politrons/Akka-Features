package data_distribution


import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey}

import scala.concurrent.duration._

/**
  * Created by pabloperezgarcia on 19/02/2017.
  *
  * With Akka cluster distributed data we can use the Replicator Actor, which we can subscribe to a ORSetKey-actor.
  * This ORSetKey it´s a combination of object and key, the object it will be the data to modify/increase/reduce in the cluster
  * For instance it could be a Set of events to be distributed through of cluster.
  *
  * In this example the producer it will subscribe to a specific ORSetKey and it will modify this one using scheduler.
  *
  * All nodes subscribed with the same ORSetKey will recveive the notification of change in "element @ Changed(DataKey)"
  *
  * Official doc
  * http://doc.akka.io/docs/akka/2.4.16/scala/distributed-data.html
  */

class ProducerBot extends Actor with ActorLogging {

  val replicator: ActorRef = DistributedData(context.system).replicator

  implicit val node = Cluster(context.system)

  import context.dispatcher

  val tickTask: Cancellable = context.system.scheduler.schedule(5.seconds, 5.seconds, self, "Tick")

  val DataKey: ORSetKey[String] = ORSetKey[String]("uniqueKey")

  replicator ! Subscribe(DataKey, self)

  /**
    * In Akka cluster distributed data we can write the changes in different ways:
    *
    * WriteLocal the value will immediately only be written to the local replica, and later disseminated with gossip
    * WriteTo(n) the value will immediately be written to at least n replicas, including the local replica
    * WriteMajority the value will immediately be written to a majority of replicas, i.e. at least N/2 + 1 replicas, where N is the number of nodes in the cluster (or cluster role group)
    * WriteAll the value will immediately be written to all nodes in the cluster (or all nodes in the cluster role group)
    */
  def receive = {
    case "Tick" =>
      val randomElement = ThreadLocalRandom.current().nextInt(97, 123).toChar.toString
      log.info("Producer adding: {}", randomElement)
      replicator ! Update(DataKey, ORSet.empty[String], WriteLocal)(_ + randomElement)

    case _: UpdateResponse[_] => //Ignore

    //This case will get all changes in the ORSetKey
    case element @Changed(DataKey) =>
      val data = element.get(DataKey)
      log.info("Producer elements: {}", data.elements)
  }

  override def postStop(): Unit = tickTask.cancel()

}