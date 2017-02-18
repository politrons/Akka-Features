package http

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

/**
  * Akka http it´s just a simple implementation of Http client using Actor system
  *
  */
class ActorClient extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  /**
    * preStart method it´s invoked when the actor it´s completely initialized
    *
    * http class it´s used to invoke singleRequest which allow all types of request GET, POST, PUT, DELETE
    * After we make the request we pipe the request to the actorSystem which provide a Future[HttpResponse]
    *
    */
  override def preStart(): Unit = {
    getVersion
    postOrder.onComplete(_ => getOrder)
  }

  private def getVersion = {
    http.singleRequest(HttpRequest(uri = "http://localhost:8080/version"))
      .pipeTo(self)
  }

  private def postOrder = {
    val data = ByteString("abc")
    http.singleRequest(HttpRequest(HttpMethods.POST, uri = "http://localhost:8080/order", entity = data))
      .pipeTo(self)
  }

  private def getOrder = {
    http.singleRequest(HttpRequest(uri = "http://localhost:8080/order"))
      .pipeTo(self)
  }

  def receive: PartialFunction[Any, Unit] = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      getBody(entity)
    case HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
  }

  private def getBody(entity: ResponseEntity) = {
    entity.dataBytes
      .map(value => value.decodeString("UTF-8"))
      .runForeach(value => log.info(value))
  }
}

