package dataImpl.youtube

import sttp.client3.basicRequest
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.matching.Regex


object Playlist {
  implicit val backend = AsyncHttpClientFutureBackend()

  def retrieveAllItems(playlistId: String, keyApi: String, regexExclude: Option[Regex]): Either[String, List[String]] = {
    val maxResults = 50

    var token = ""

    var items = List[String]()

    var anotherPage = true

    while (anotherPage) {
      var responseNext = Await.result(basicRequest
        .get(
          uri"https://www.googleapis.com/youtube/v3/playlistItems?key=$keyApi&playlistId=$playlistId&part=snippet&maxResults=$maxResults&pageToken=$token"
        ).send(backend),
        10.seconds
      )

      responseNext.body match {
        case Right(v) =>
          val parsed = Json.parse(v)
          parsed.validate[ResponsePlaylistItems] match {
            case JsError(es) =>
              items = items :++ parsed.as[ResponsePlaylistItemsLastPage].items.map(playlistItem => playlistItem.snippet.resourceId.videoId)
              anotherPage = false
            case JsSuccess(value, path) =>
              val responsePlaylistItems = value
              regexExclude match {
                case Some(r) =>
                  items = items :++ responsePlaylistItems.items
                    .filter(playlistItem => r.findFirstMatchIn(playlistItem.snippet.title).isEmpty)
                    .map(playlistItem => playlistItem.snippet.resourceId.videoId)
                case None =>
                  items = items :++ responsePlaylistItems.items.map(playlistItem => playlistItem.snippet.resourceId.videoId)
              }
              token = responsePlaylistItems.nextPageToken
          }
        case Left(e) => return Left(e)
      }
    }

    Right(items)
  }
}

case class PageInfo(totalResults: Int, resultsPerPage: Int)
object PageInfo {
  implicit val format = Json.format[PageInfo]
}

case class ResourceId(videoId: String)
object ResourceId {
  implicit val format: Format[ResourceId] = Json.format[ResourceId]
}

case class Snippet(title: String, resourceId: ResourceId)
object Snippet {
  implicit val format: Format[Snippet] = Json.format[Snippet]
}

case class PlaylistItem(kind: String,
                        etag: String,
                        id: String,
                        snippet: Snippet)

object PlaylistItem {
  implicit val format = Json.format[PlaylistItem]
}

case class ResponsePlaylistItems(items: List[PlaylistItem], pageInfo: PageInfo, nextPageToken: String)

object ResponsePlaylistItems {
  implicit val format: Format[ResponsePlaylistItems] = Json.format[ResponsePlaylistItems]
}


case class ResponsePlaylistItemsLastPage(items: List[PlaylistItem], pageInfo: PageInfo)

object ResponsePlaylistItemsLastPage {
  implicit val format: Format[ResponsePlaylistItemsLastPage] = Json.format[ResponsePlaylistItemsLastPage]
}
