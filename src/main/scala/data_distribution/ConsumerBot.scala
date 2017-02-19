package data_distribution

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, ORSetKey}

/**
  * Created by pabloperezgarcia on 19/02/2017.
  *
  * In this example the consumer it will subscribe to a specific ORSetKey and it will receive all changes happens
  * in the ORSetKey
  *
  * http://doc.akka.io/docs/akka/2.4.16/scala/distributed-data.html
  */
class ConsumerBot extends Actor with ActorLogging {

  val replicator: ActorRef = DistributedData(context.system).replicator

  implicit val node = Cluster(context.system)

  val DataKey: ORSetKey[String] = ORSetKey[String]("uniqueKey")

  replicator ! Subscribe(DataKey, self)

  def receive = {

    case _: UpdateResponse[_] => //ignore

    case c@Changed(DataKey) =>
      val data = c.get(DataKey)
      log.info("Consumer elements: {}", data.elements)

  }

}