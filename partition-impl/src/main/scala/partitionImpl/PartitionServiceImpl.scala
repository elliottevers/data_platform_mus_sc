package partitionImpl

import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import dataImpl.{DatasetStatic, QueryStateStaticDataset}
import partitionApi.{CreatePartitionRequest, CreatePartitionResponse, PartitionService}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class PartitionServiceImpl(
  persistentEntityRegistry: PersistentEntityRegistry,
  clusterSharding: ClusterSharding
)(implicit ec: ExecutionContext)
    extends PartitionService {
  implicit val timeout = Timeout(5.seconds)
  override def createPartition
    : ServiceCall[CreatePartitionRequest, CreatePartitionResponse] =
    ServiceCall { request =>
      val entityRef = clusterSharding.entityRefFor(
        DatasetStatic.typeKey,
        request.idDatasetStatic
      )
      val res = Await.result(
        entityRef
          .ask[Seq[String]](actorRef => QueryStateStaticDataset("", actorRef)),
        10.seconds
      )

      val r = scala.util.Random
      r.setSeed(request.seedRng)

      val bucketsWithState = request.splits.foldLeft(
        (Map[Range, String](), 0.toInt)
      )((acc: (Map[Range, String], Int), next: (String, Int)) => {
        (
          acc._1 updated (acc._2 until (acc._2 + next._2), next._1.toString),
          acc._2 + next._2
        )
      })

      val buckets: Map[Range, String] = bucketsWithState._1

      val partitionNew: Map[String, String] = res
        .map(idVideo => {
          val intRandom = r.nextInt(100)
          val bucketThisRandomInt: Seq[String] = buckets
            .filter(rangeToString => {
              rangeToString._1.contains(intRandom)
            })
            .toSeq
            .map(_._2)

          (idVideo, bucketThisRandomInt.head)
        })
        .toMap

      val guid = java.util.UUID.randomUUID()

      val entityRefPartition = clusterSharding.entityRefFor(
        Partition.typeKey,
        guid.toString
      )

      entityRefPartition.ask[Map[String, String]](
        actorRef =>
          AddPartition(
            partitionNew,
            actorRef
        )
      ).map(partition => CreatePartitionResponse(partition, guid.toString))
    }

  override def readPartition(guid: String): ServiceCall[NotUsed, Map[String, String]] = ServiceCall { request =>
    val entityRefPartition = clusterSharding.entityRefFor(
      Partition.typeKey,
      guid
    )

    entityRefPartition.ask[Map[String, String]](
      actorRef =>
        QueryStatePartition(
          "",
          actorRef
        )
    )
  }

  override def topicPartitionAdded(): Topic[String] = TopicProducer.taggedStreamWithOffset(EventPartition.Tag.allTags.toSeq) { (tag, offset) =>
    persistentEntityRegistry.eventStream(tag, offset).mapAsync(1) { event =>
      event.event match {
        case _ =>
          println("logging that something else happened from PartitionServiceImpl")
          Future.successful(("this message is coming from a topic", offset))
      }
    }
  }
}
