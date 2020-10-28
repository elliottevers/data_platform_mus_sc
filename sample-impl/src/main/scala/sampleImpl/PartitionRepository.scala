package sampleImpl

import akka.Done
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global

class PartitionRepository {
  class PartitionTable(tag: Tag) extends Table[(String, String, String)](tag, "tblPartition") {
    def linkVideoYouTube = column[String]("link_video_youtube")
    def guidPartition = column[String]("guid_partition")
    def namePartition = column[String]("name_partition")
    def * = (linkVideoYouTube, guidPartition, namePartition)
  }

  val partitions = TableQuery[PartitionTable]

  def selectPartitions() = partitions.result

  def save = {
    partitions.insertOrUpdate(("test", "this", "shit")).map(_ => Done)
  }

  def createTable = {
    partitions.schema.createIfNotExists
  }
}
