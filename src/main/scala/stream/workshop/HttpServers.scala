package stream.workshop

import com.twitter.finagle._
import com.twitter.finagle.http.service.HttpResponseClassifier
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, Future}

/**
  * Created by pabloperezgarcia on 08/04/2017.
  */
object HttpServers extends App {

  def service = Service.mk[Request, Response] { r: Request => {
    val reqNumber = r.getParams("number")
    Thread.sleep(5000)
    println(s"Processing request $reqNumber")
    val rep = Response()
    rep.setContentString(
      s"""
            {"status":"OK"}
        """
    )
    Future.value(rep)
  }
  }

  Await.ready(Http.server
    .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)
    .withAdmissionControl.concurrencyLimit(maxConcurrentRequests = 10, maxWaiters = 10)
    .serve(s"localhost:1981", service))
}
