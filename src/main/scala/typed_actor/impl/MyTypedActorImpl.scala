package typed_actor.impl

import java.util.UUID

import akka.Done
import akka.actor.{TypedActor, TypedProps}
import typed_actor.MyTypedActor
import typed_actor.model.User

import scala.concurrent.Future

/**
  * Created by pabloperezgarcia on 04/03/2017.
  *
  * One of the good things to use Typed actors is that you can code your pojo without expose that it will
  * be used as an actor, in fact in some cases, but not recommended you could used as Pojo and actor
  * in your system if you want
  */
class MyTypedActorImpl extends MyTypedActor {

  var users: Map[String, User] = Map()

  override def register(user: User): Unit = {
    users = users ++ Map(user.username -> user)
  }

  override def getUserFor(username: String): User = {
    users(username)
  }

  override def isMyUserRegister(username: String): Future[Boolean] = {
    Thread.sleep(1000)
    Future.successful(users.isDefinedAt(username))
  }

  override def addYearsLived(year: Int): Future[Done] = {
    Thread.sleep(500)
    println(s"It´s my $year birthday!!")
    addNewUserByChildrenWorker(year)
    Future.successful(Done)
  }

  private def addNewUserByChildrenWorker(year: Int) = {
    val childActor: MyTypedActor =
      TypedActor(TypedActor.context).typedActorOf(TypedProps[MyTypedActorImpl]())
    val username = UUID.randomUUID().toString
    childActor.register(User(username, year))
    println(s"My father invite my friend ${childActor.getUserFor(username).username}")
  }
}
