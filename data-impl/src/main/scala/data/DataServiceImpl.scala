package data

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import data.Dataset.{AddRecord, RecordAdded}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future

class DataServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, clusterSharding: ClusterSharding) extends DataService {

  implicit val timeout = Timeout(5.seconds)

  override def createRecord: ServiceCall[Record, String] = ServiceCall { request =>
    println("logging from data service")
    clusterSharding.entityRefFor(Dataset.typeKey, "7ea45dd3-3976-4570-acd9-bf928592b5df").ask[Nothing](reply => AddRecord(Record(2)))
    println(request)
    Future.successful("createdRecord succeeded")
  }

  override def topicRecordAdded(): Topic[RecordAddedServiceDescriptor] = TopicProducer.taggedStreamWithOffset(Dataset.Event.Tag.allTags.toSeq) { (tag, offset) =>
    persistentEntityRegistry.eventStream(tag, offset).mapAsync(1) { event =>
      event.event match {
        case RecordAdded(r) =>
          println("logging that Record added from DataServiceImpl") // Success!
          Future.successful((RecordAddedServiceDescriptor(r), offset))
        case other =>
          println("logging that something else happened from DataServiceImpl")
          Future.successful((RecordAddedServiceDescriptor(Record(9001)), offset))
      }
    }
  }
}
