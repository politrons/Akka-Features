package alpakka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import akka.stream.scaladsl.Sink
import com.datastax.driver.core.{Cluster, SimpleStatement}

/**
  * Created by pabloperezgarcia on 09/09/2017.
  */
class CassandraConnector {


  implicit val session = Cluster.builder
    .addContactPoint("127.0.0.1").withPort(9042)
    .build.connect()

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val stmt = new SimpleStatement("SELECT * FROM akka_stream_scala_test.test").setFetchSize(20)

  val rows = CassandraSource(stmt).runWith(Sink.seq)





}
