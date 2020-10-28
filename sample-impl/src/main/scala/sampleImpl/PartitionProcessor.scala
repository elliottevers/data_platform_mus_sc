package sampleImpl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import partitionImpl.{EventPartition, PartitionAdded}
import slick.dbio.DBIO

class PartitionProcessor(readSide: SlickReadSide,
                         partitionRepository: PartitionRepository)
    extends ReadSideProcessor[EventPartition] {

  private def processPartitionCreated(eventElement: EventStreamElement[EventPartition]): DBIO[Done] = {
    partitionRepository.save
  }

  override def buildHandler()
    : ReadSideProcessor.ReadSideHandler[EventPartition] = {
    readSide
      .builder[EventPartition]("PartitionProcessor")
      .setGlobalPrepare(partitionRepository.createTable)
      .setEventHandler[PartitionAdded](processPartitionCreated)
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[EventPartition]] = EventPartition.Tag.allTags
}
