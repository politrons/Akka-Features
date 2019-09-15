package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext

object HttpAkkaStream extends App {

  implicit val system: ActorSystem = ActorSystem("AkkaStreamHttp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  private val port = 8080

  Http().bindAndHandle(getRoutes, "localhost", port)

  println(s"Akka Stream Server running in port $port")

  // ADT & JSON FORMAT
  //####################

  /**
    * We create the request data for the POST request,
    * and also the Json format from Spray library, that we use as implicit conversion to transform a json entry to Data type.
    */

  case class UserDataType(id: Int, name: String, age: Int, sex: String)

  implicit val requestDataJsonFormat: RootJsonFormat[UserDataType] = jsonFormat4(UserDataType.apply)

  var users = Map[Int, UserDataType]()

  /**
    * Function where we define all routes of our service.
    * We are able to use [asSourceOf] function which thanks to implicit [EntityStreamingSupport]
    * which expect a Data type to be able to marshall to Json we are able to transform into Akka [Source]
    * We are able to Marshall from Json to Data type [UserDataType] thanks to implicit conversion RootJsonFormat[UserDataType]
    *
    * Once we have the Source from our request we can use [runFold] operator to be able to receive the whole json request chunk in
    * each [UserDataType] entry, to process the whole request as a stream of data.
    *
    * @return Route type that it will used by [Http().bindAndHandle] to route request to the proper handler.
    */
  private def getRoutes: Route = {

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    post {
      entity(asSourceOf[UserDataType]) { source =>
        complete {
          source
            .runFold(users)((users, user) => {
              users == users ++ Map(user.id -> user)
              println(s"New user ${user.name} added")
              users
            }).map(users => s"total number of users in system ${users.size}")
        }
      }
    }
  }
}

