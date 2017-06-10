package stream.workshop

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}


/**
  * Created by pabloperezgarcia on 08/04/2017.
  */
object Client extends App {

  def client: Service[Request, Response] = Http.newService("localhost:1981")

  def request = http.Request(http.Method.Get, "/")

  0 to 20 foreach (number => {
    val future = client(request)
    future.onSuccess(res =>
      println(res)
    )
    future.onFailure(res =>
      println(res)
    )
    println(s"Request send $number")
  })

  Thread.sleep(50000)
}
