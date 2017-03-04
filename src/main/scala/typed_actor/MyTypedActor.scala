package typed_actor

import akka.Done
import typed_actor.model.User

import scala.concurrent.Future

/**
  * Created by pabloperezgarcia on 04/03/2017.
  */
trait MyTypedActor {
  def register(user: User): Unit

  def getUserFor(username: String): User

  def isMyUserRegister(username:String): Future[Boolean]

  def addYearsLived(year:Int):Future[Done]
}
