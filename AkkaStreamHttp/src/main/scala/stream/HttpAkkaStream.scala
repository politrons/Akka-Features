package stream

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.{ByteString, Timeout}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * POST: To Test just run the Server and run the client.sh which it will run some post request.
  * GET: To Test just run the server and make a request with some query params (http://localhost:8080/requestStreamGet?key=hello&value=world)
  */
object HttpAkkaStream extends App {

  implicit val system: ActorSystem = ActorSystem("AkkaStreamHttp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  private val port = 8080

  val routes = getRoutes

  Http().bindAndHandle(routes._1 ~ routes._2 ~ routes._3, "localhost", port)

  println(s"Akka Stream Server running in port $port")

  // ADT & JSON FORMAT
  //####################

  /**
    * We create the request data for the POST request,
    */
  case class UserDataType(id: Int, name: String, age: Int, sex: String)

  /**
    * Json format from Spray library, that we use as implicit conversion to transform a json entry to Data type.
    * We have to use the operator [jsonFormatX] where [X] is the number of arguments of your Data type.
    */
  implicit val requestDataJsonFormat: RootJsonFormat[UserDataType] = jsonFormat4(UserDataType.apply)

  var users = Map[Int, UserDataType]()

  //  ROUTES
  //#############
  /**
    * Function where we define all routes of our service.
    *
    * We define routes per http method, where inside we can deifne multiple [paths], and then we return a tuple of routes.
    *
    * @return (Route,Route) type that it will used by [Http().bindAndHandle] to route request to the proper handler.
    */
  private def getRoutes: (Route, Route, Route) = {

    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    /**
      * We are able to use [asSourceOf] function which thanks to implicit [EntityStreamingSupport]
      * which expect a json array to chunk into array of Data type [UserDataType] into Akka [Source]
      *
      * We are able to Marshall from Json to Data type [UserDataType] thanks to implicit conversion RootJsonFormat[UserDataType]
      *
      * Once we have the Source from our request we can use [runFold] operator to be able to receive  each [UserDataType] entry,
      * to process the whole request as a stream of data.
      */
    val postRoutes = path("requestStream") {
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

    /**
      *
      * In order to get query params we use [parameters] where you use ['] followed by the query name and the type you want to set.
      * This generate function where we receive the two query params variables.
      *
      * Once we receive the request we can define in the complete the output.
      *
      * We create the Source using [fromFuture] which allow us make the computation of the request in another Thread.
      * the arguments we receive form the request, and we use [throttle] to provide back-pressure.
      *
      * Using [throttle] operator Sends elements downstream with speed limited to `elements/per`. In other words, this stage set the maximum rate
      * for emitting messages.
      * Backpressures when downstream backpressures or the incoming rate is higher than the speed limit.
      *
      * Once we have out monad Stream we can make transformation as usual using [map] or composition of another [Source] using [flatMapConcat]
      *
      * Also we can filter or takeWhile passing predicate functions to follow down to the stream.
      */
    val getRoutes = path("requestStreamGet") {
      get {
        parameters('key.as[String], 'value.as[String]) { (key, value) =>
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
            Source.fromFuture(Future(s"Request received: Key:$key - value:$value"))
              .throttle(elements = 1000, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
              .flatMapConcat(value => Source.fromFuture(Future(s"$value Concat in another future")))
              .map(value => value.toUpperCase)
              .flatMapConcat(value => Source.single(value + "!!!"))
              .filter(value => value.length > 5)
              .takeWhile(value => value.length < 100)
              .map(s => ByteString(s + "\n"))))
        }
      }
    }

    /**
      * Using Source of Akka Stream we can communicate and pass the event emitted in the stream to another actor
      * using [ask] operator, which expect to receive an actorRef to call it. Since Akka communication here is untyped, we must
      * specify in the ask operator, the return type of the communication.
      *
      * Here we will first send as Message type, and we will receive an String, and then we will send that String, and we will
      * receive the Message type.
      *
      * Since Akka use Future in Ask patter, the communication is completely async.
      */
    val akkaRoutes = path("requestStreamActor") {
      get {
        complete {
          Source.fromFuture(Future(s"Request received redirected to Actor:"))
            .throttle(elements = 1000, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
            .map(message => Message(message))
            .ask[String](customActor)
            .ask[Message](customActor)
            .map(message => ByteString(message.value + "\n"))
        }
      }
    }

    (postRoutes, getRoutes, akkaRoutes)
  }

  // AKKA ACTOR & MESSAGE
  //#####################

  lazy val customActor = system.actorOf(Props[CustomActor], name = "customAkka")

  case class Message(value: String)

  class CustomActor extends Actor {
    override def receive: Receive = {
      case Message(value) => sender() ! s"$value Message process and response from Actor"
      case value: String => sender() ! Message(value + "!!!")
    }
  }

}