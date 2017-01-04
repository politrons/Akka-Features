package message

import scala.concurrent.Future

/**
  * Created by pabloperezgarcia on 18/12/2016.
  */
case class ResultMsg(value: Future[String]) extends Message
