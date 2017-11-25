package typed

import java.util.concurrent.TimeUnit

import akka.actor.Scheduler
import akka.typed._
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.Actor.Immutable
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import org.junit.Test

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by pabloperezgarcia on 25/11/2017.
  *
  * Using the new feature of akka 2.5.6 Akka typed[https://doc.akka.io/docs/akka/2.5.6/scala/typed.html]
  * we can forget about have to cast in runtime what the actor send back using ask pattern.
  *
  * Since we send in the message the actorRef of myself specifying, and in the case class of the actor that receive the message
  * we set type of message that I expect to receive back, there´s no need for more casting in the Future response type.
  *
  * Here for instance sending to the same actor system three different message with a different type
  * in the actorRef defined in the case class of the actor, my sender know which type of value it will be returned with the future.
  *
  * This a very good pattern to use CQRS for Command/events
  */
class AkkaTyped {

  import Store._

  implicit val timeout: Timeout = Timeout(Duration.create(5, TimeUnit.SECONDS))
  implicit val system: ActorSystem[Product] = ActorSystem(till, "Tesco")
  implicit val scheduler: Scheduler = system.scheduler

  @Test
  def buyProducts(): Unit = {

    val add1: Future[String] = system ? (AddProduct("coca-cola", _)) //If the type in the case class does not match it wont compile
    val add2: Future[String] = system ? (AddProduct("pepsi", _))
    val add3: Future[String] = system ? (AddProduct("cornflakes", _))
    val remove1: Future[String] = system ? (RemoveProduct("pepsi", _))
    val basket: Future[Basket] = system ? TotalBasket

    val total = Await.result(basket, Duration.create(10, TimeUnit.SECONDS))
    println("\n########## BASKET ##########")
    total.products.foreach(product => {
      println(product)
    })
  }

  object Store {

    trait Product

    final case class AddProduct(product: String, replyTo: ActorRef[String]) extends Product //In the ActorRef we specify the type

    final case class RemoveProduct(product: String, replyTo: ActorRef[String]) extends Product

    final case class TotalBasket(replyTo: ActorRef[Basket]) extends Product

    final case class Basket(products: List[String])

    var basket: List[String] = List()

    val till: Immutable[Product] = Actor.immutable[Product] {
      (_, msg) =>
        msg match {
          case add: AddProduct =>
            println(s"Adding ${add.product} in basket")
            basket = basket ++ List(add.product)
            add.replyTo ! "done"
            Actor.same
          case remove: RemoveProduct =>
            println(s"Removing ${remove.product} in basket")
            basket = basket.filter(product => product != remove.product)
            remove.replyTo ! "done"
            Actor.same
          case total: TotalBasket =>
            total.replyTo ! Basket(basket)
            Actor.same
        }
    }
  }

}
