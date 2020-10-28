package partitionApi

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, JsNumber, JsObject, JsResult, JsString, JsSuccess, JsValue, Json, Reads}

trait PartitionService extends Service {

  val TOPIC_NAME = "PartitionAddedTest"

  implicit val readsSeqString: Reads[Seq[String]] = Reads.seq(implicitly)

  def topicPartitionAdded(): Topic[String]

  def createPartition: ServiceCall[CreatePartitionRequest, CreatePartitionResponse]

  def readPartition(guid: String): ServiceCall[NotUsed, Map[String, String]]

  override final def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    // @formatter:off
    named("partitionApi")
      .withCalls(
        restCall(
          Method.POST,
          "/partition",
          createPartition
        ),
        pathCall(
          "/partition/:guid",
          readPartition _
        )
      )
      .withTopics(
        topic(TOPIC_NAME, topicPartitionAdded())
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

case class CreatePartitionResponse(partitions: Map[String, String], guid: String)
object CreatePartitionResponse {
  implicit val format: Format[CreatePartitionResponse] = Json.format[CreatePartitionResponse]
}

case class ReadPartitionRequest(guid: String)
object ReadPartitionRequest {
  implicit val format: Format[ReadPartitionRequest] = Json.format[ReadPartitionRequest]
}

case class CreatePartitionRequest(idDatasetStatic: String,
                                  splits: Map[String, Int],
                                  seedRng: Int)
object CreatePartitionRequest {
  implicit val format: Format[CreatePartitionRequest] = new Format[CreatePartitionRequest] {
    override def reads(json: JsValue): JsResult[CreatePartitionRequest] = {
      JsSuccess(
        CreatePartitionRequest(
          (json \ "idDatasetStatic").as[String],
          (json \ "splits").as[Map[String, Int]],
          (json \ "seedRng").as[Int]
        )
      )
    }

    override def writes(o: CreatePartitionRequest): JsValue = {

      val ss: Map[String, JsValue] = o.splits.map { pair =>
        (pair._1, JsNumber(pair._2))
      }

      JsObject(
        Map[String, JsValue](
          "idDatasetStatic" -> JsString(o.idDatasetStatic),
          "splits" -> JsObject(ss)
        )
      )
    }
  }

}
