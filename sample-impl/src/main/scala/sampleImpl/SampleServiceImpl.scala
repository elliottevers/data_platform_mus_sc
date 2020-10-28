package sampleImpl

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import play.api.libs.json.{Format, Json}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.basicRequest
import dataApi.DataService
import partitionImpl.{Partition, QueryStatePartition}
import sampleApi.{Sample, SampleService}
import slick.jdbc.JdbcBackend.Database
import sttp.client3._

class SampleServiceImpl(dataService: DataService,
                        clusterSharding: ClusterSharding,
                        db: Database,
                        sampleRepository: PartitionRepository,
)(implicit materializer: Materializer)
    extends SampleService {

  implicit val backend = AsyncHttpClientFutureBackend()

  override def sampleRecord(guidPartition: String,
                            environment: String): ServiceCall[NotUsed, Sample] =
    ServiceCall { request =>
      var response = Await.result(
        basicRequest
          .get(uri"https://qrng.anu.edu.au/API/jsonI.php?length=1&type=uint16")
          .send(backend),
        10.seconds
      )

      response.body match {
        case Right(v) =>
          val parsed = Json.parse(v)
          val mostRandomNumberEver = parsed.as[ResponseQRNG].data.head
          val entityRefPartition = clusterSharding
            .entityRefFor(Partition.typeKey, guidPartition)

          val partition = Await.result(
            entityRefPartition.ask[Map[String, String]](
              actorRef => QueryStatePartition("", actorRef)
            )(5.seconds),
            5.seconds
          )
          val partitionFiltered = partition.filter(kv => {
            kv._2 == environment
          })

          Future.successful(
            Sample(
              s"https://www.youtube.com/watch?v=${partitionFiltered.toList(mostRandomNumberEver % partitionFiltered.size)._1}"
            )
          )

        case Left(e) => Future.successful(Sample("an error occured"))
      }
    }
}

case class ResponseQRNG(data: Seq[Int])
object ResponseQRNG {
  implicit val format: Format[ResponseQRNG] = Json.format[ResponseQRNG]
}
