package stream.workshop

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}


/**
  * Created by pabloperezgarcia on 08/04/2017.
  */
object Client extends App {

  def client: Service[Request, Response] = Http.newService("localhost:1981")

  def request = http.Request(http.Method.Get, "/")

  0 to 20 foreach (message => {
    val future = client(request).map(res => (message, res))
    future.onSuccess(res =>
      println(s"#################### Response message:${res._1} status:${res._2.statusCode}")
    )
    future.onFailure(res =>
      println(res)
    )
    println(s"Sending message $message")
  })

  Thread.sleep(50000)
}
