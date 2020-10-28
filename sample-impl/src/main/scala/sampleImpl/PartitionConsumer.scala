package sampleImpl

import akka.Done
import akka.stream.scaladsl.Flow
import partitionApi.PartitionService

class PartitionConsumer(partitionService: PartitionService) {
  partitionService.topicPartitionAdded().subscribe.atLeastOnce(
    Flow[String].map { elem =>
      println(elem); println("printing from consumer"); elem
    }.map(any => Done)
  )
}
