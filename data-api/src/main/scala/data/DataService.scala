package data

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object DataService {
  val TOPIC_NAME = "recordAdded"
}

trait DataService extends Service {

  def createRecord: ServiceCall[Record, String]

  def topicRecordAdded(): Topic[RecordAddedServiceDescriptor]

  override final def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    // @formatter:off
    named("data")
      .withCalls(
        restCall(
          Method.POST,
          "/data/record",
          createRecord
        )
      )
      .withTopics(
        topic(DataService.TOPIC_NAME, topicRecordAdded())
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

case class Record(page: Int)

object Record {
  implicit val format: Format[Record] = Json.format[Record]
}

case class RecordAddedServiceDescriptor(record: Record)

object RecordAddedServiceDescriptor {
  implicit val format: Format[RecordAddedServiceDescriptor] = Json.format[RecordAddedServiceDescriptor]
}
