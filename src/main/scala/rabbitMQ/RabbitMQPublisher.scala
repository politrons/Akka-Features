package rabbitMQ

import akka.actor.{ActorRef, ActorSelection, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.newmotion.akka.rabbitmq._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by pabloperezgarcia on 23/04/2017.
  */
object RabbitMQPublisher extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val rabbitURL: String = "amqp://guest:guest@localhost:5672"
  val factory = new ConnectionFactory()
  factory.setUri(rabbitURL)
  val exchange = "custom.myOwnExchange.com"

  openConnection
  publishEvents(500)

  private def openConnection = {
    val connection = system.actorOf(ConnectionActor.props(factory), "rabbitmq")
    connection ! CreateChannel(ChannelActor.props(setupQueue), Some("publisher"))
  }

  private def setupQueue(channel: Channel, self: ActorRef) {
    val queue = channel.queueDeclare().getQueue
    channel.queueBind(queue, exchange, "")
  }

  private def publishEvents(numberOfEvents: Int = 1) = {
    val publisher = system.actorSelection("/user/rabbitmq/publisher")
    Await.ready(Source(0 to numberOfEvents)
      .flatMapConcat(evt => Source.single(publishEvent(publisher, s"This is a test message $evt")))
      .runForeach(event => println(s"Event published:$event")), 5 seconds)
  }

  private def publishEvent(publisher: ActorSelection, event: String) = {
    publisher ! ChannelMessage(_.basicPublish(exchange, "", null, toBytes(event)),
      dropIfNoChannel = false)
  }

  def toBytes(message: String) = message.getBytes("UTF-8")

}
