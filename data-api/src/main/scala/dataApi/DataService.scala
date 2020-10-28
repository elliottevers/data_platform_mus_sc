package dataApi

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, JsArray, JsError, JsObject, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}
import enumeratum._

trait DataService extends Service {

  def createDataset: ServiceCall[Dataset, String]

  def createDatasetStatic: ServiceCall[DatasetStaticRequest, String]

  def readDataset(guid: String): ServiceCall[NotUsed, Seq[String]]

  implicit val readsSeqString: Reads[Seq[String]] = Reads.seq(implicitly)

  override final def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    // @formatter:off
    named("dataApi")
      .withCalls(
        restCall(
          Method.POST,
          "/dataset",
          createDataset
        ),
        pathCall(
          "/dataset/:guid",
          readDataset _
        ),
        restCall(
          Method.POST,
          "/datasetStatic",
          createDatasetStatic
        )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

case class DatasetStaticRequest(maybeDatasetStatic: Option[DatasetStatic], maybeDatasetDynamicSeed: Option[DatasetDynamic], exclude: Option[String])
object DatasetStaticRequest {
  implicit val format: Format[DatasetStaticRequest] = new Format[DatasetStaticRequest] {
    override def reads(json: JsValue): JsResult[DatasetStaticRequest] = {
      (
        (json \ "datasetDynamicSeed").toOption,
        (json \ "datasetStatic").toOption
      ) match {
          // TODO: actually provide error messages
        case (Some(jsValDynamic), Some(jsValStatic)) => JsSuccess(DatasetStaticRequest(None, None, None)) // TODO
        case (None, Some(jsValStatic)) =>
          JsSuccess(DatasetStaticRequest(Some(jsValStatic.as[DatasetStatic]), None, None))
        case (Some(jsValDynamic), None) =>
          val maybeExclude = (json \ "exclude").toOption

          maybeExclude match {
            case Some(reg) =>
              JsSuccess(DatasetStaticRequest(
                None,
                Some(jsValDynamic.as[DatasetDynamic]),
                Some(reg.as[String])
              ))
            case None =>
              JsSuccess(DatasetStaticRequest(
                None,
                Some(jsValDynamic.as[DatasetDynamic]),
                None
              ))
          }
        case (None, None) => JsSuccess(DatasetStaticRequest(None, None, None)) // TODO
      }
    }

    override def writes(o: DatasetStaticRequest): JsValue = {
      o.maybeDatasetDynamicSeed match {
        case Some(datasetDynamic) =>
          JsObject(Map(
            "__tag" -> JsString("dynamic"),
            "linksPlaylist" -> JsArray(datasetDynamic.linksPlaylist.map(JsString))
          ))
      }
      o.maybeDatasetStatic match {
        case Some(datasetStatic) =>
          JsObject(Map(
            "__tag" -> JsString("dynamic"),
            "linksVideos" -> JsArray(datasetStatic.linksVideos.map(JsString))
          ))
      }

      JsObject(Map[String, JsValue](
//        "datasetDynamicSeed" -> JsObject(Map[String, JsValue]()),
//        "datasetStatic" -> JsObject(Map[String, JsValue]())
      ))
    }
  }
}


case class EmptyRequest(nothing: String)

object EmptyRequest {
  implicit val format: Format[EmptyRequest] = Json.format[EmptyRequest]
}

case class Record(page: Int)

object Record {
  implicit val format: Format[Record] = Json.format[Record]
}

case class RecordAddedServiceDescriptor(record: Record)

object RecordAddedServiceDescriptor {
  implicit val format: Format[RecordAddedServiceDescriptor] = Json.format[RecordAddedServiceDescriptor]
}

sealed trait Dataset {
  val name: String
}
case class DatasetDynamic(linksPlaylist: Seq[String], name: String) extends Dataset
case class DatasetStatic(linksVideos: Seq[String], name: String) extends Dataset

object DatasetDynamic {
  implicit val format: Format[DatasetDynamic] = Json.format[DatasetDynamic]
}

object DatasetStatic {
  implicit val format: Format[DatasetStatic] = Json.format[DatasetStatic]
}

object Dataset {
  implicit val readsSeqString: Reads[Seq[String]] = Reads.seq(implicitly)

  implicit val reads: Reads[Dataset] = new Reads[Dataset] {
    def reads(json: JsValue): JsResult[Dataset]  = {
      val datasetJson = json \ "dataset"
      val nameJson = json \ "name"
      val tag = (datasetJson \ "__tag").as[String]

      tag match {
        case "dynamic" =>
          val linksPlaylist = (datasetJson \ "linkPlaylist").as[Seq[String]]
          JsSuccess(DatasetDynamic(linksPlaylist, nameJson.as[String]))
        case "static" =>
          val linksVideos = (datasetJson \ "linksVideos").as[Seq[String]]
          JsSuccess(DatasetStatic(linksVideos, nameJson.as[String]))
        case _ =>
          JsError("must be either dynamic or static")
      }
    }
  }

  implicit val writes: Writes[Dataset] = new Writes[Dataset] {
    override def writes(o: Dataset): JsValue = {
      o match {
        case DatasetDynamic(linksPlaylist, o.name) =>
          JsObject(Map(
            "__tag" -> JsString("dynamic"),
            "linksPlaylist" -> JsArray(linksPlaylist.map(JsString)),
            "name" -> JsString(o.name)
          ))
        case DatasetStatic(linksVideos, o.name) =>
          JsObject(Map(
            "__tag" -> JsString("static"),
            "linksVideos" -> JsArray(linksVideos.map(JsString)),
            "name" -> JsString(o.name)
          ))
      }
    }
  }
}

sealed trait TypeSource extends EnumEntry

object TypeSource extends Enum[TypeSource] with PlayJsonEnum[TypeSource] {

  val values = findValues

  case object Spotify extends TypeSource
  case object YouTube extends TypeSource
  case object Book extends TypeSource
}
