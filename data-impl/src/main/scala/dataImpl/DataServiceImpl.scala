package dataImpl

import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import akka.util.Timeout
import dataApi.{DataService, DatasetStaticRequest}
import dataImpl.youtube.Playlist

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import pureconfig._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import pureconfig.generic.auto._

class DataServiceImpl(
  persistentEntityRegistry: PersistentEntityRegistry,
  clusterSharding: ClusterSharding
)(implicit ec: ExecutionContext)
    extends DataService {

  implicit val timeout = Timeout(5.seconds)

  implicit val backend = AsyncHttpClientFutureBackend()

//  def entityRef(id: String): EntityRef[CommandDataset] = {
//    clusterSharding.entityRefFor(DatasetDynamic.typeKey, id)
//  }

  override def createDataset: ServiceCall[dataApi.Dataset, String] =
    ServiceCall { request =>
      println("logging from createDataset method")

      val guid = java.util.UUID.randomUUID()

      val (typeKey, command) = request match {
        case dataApi.DatasetStatic(linksVideos, name) =>
          (DatasetStatic.typeKey, AddDatasetStatic(linksVideos, name))
        case dataApi.DatasetDynamic(linkPlaylist, name) =>
          (DatasetDynamic.typeKey, AddDatasetDynamic(linkPlaylist, name))
      }

//    clusterSharding.entityRefFor(typeKey, guid.toString).ask[Nothing](reply => command)

      Future.successful("dataset created")
    }

  case class ServiceConf(keyApiYoutube: String)

  override def readDataset(guid: String): ServiceCall[NotUsed, Seq[String]] = ServiceCall {
    request =>
      val entityRef = clusterSharding.entityRefFor(
        DatasetStatic.typeKey,
        guid
      )
      entityRef.ask[Seq[String]](
        actorRef => QueryStateStaticDataset("", actorRef)
      )
  }

  override def createDatasetStatic: ServiceCall[DatasetStaticRequest, String] =
    ServiceCall { request =>
      val c = ConfigSource.default.load[ServiceConf]

      var messageResponse = ""

      for {
        conf <- c
        datasetDynamic <- request.maybeDatasetDynamicSeed.toRight()
      } yield {

        val reg = """list=([^&]*)""".r

        val maybeLinksVideosToSave: Either[String, List[String]] = datasetDynamic.linksPlaylist.foldLeft[Either[String, List[String]]](Right(List()))(
          (acc, linkPlaylist) => {
            val maybeAllItems = Playlist.retrieveAllItems(
              reg.findAllIn(linkPlaylist).subgroups.toArray.head,
              conf.keyApiYoutube,
              request.exclude.map(_.r)
            )

            acc match {
              case Left(e) => Left[String, List[String]](e)
              case Right(listLinkVideosFetched) =>
                maybeAllItems match {
                  case Right(allItemsPlaylist) =>
                    Right[String, List[String]](
                      listLinkVideosFetched.concat(allItemsPlaylist)
                    )
                  case Left(e) =>
                    Left[String, List[String]](
                      "at least one failed request to YouTube API"
                    )
                }
            }
          }
        )

        maybeLinksVideosToSave match {
          case Right(linksVideosToSave) =>
            val guid = java.util.UUID.randomUUID()
            val entityRef =
              clusterSharding.entityRefFor(DatasetStatic.typeKey, guid.toString)
              entityRef.ask[String](
                actorRef =>
                  AddDatasetStatic(linksVideosToSave, datasetDynamic.name)
              )
            messageResponse = "Request forwarded to entity actor"
          case Left(e) =>
            messageResponse = e
        }
      }

      Future.successful(messageResponse)
    }
}
