package sampleImpl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import dataApi.{Record, RecordAddedServiceDescriptor}
import partitionImpl.PartitionAdded

object SampleSerializationRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Record],
    JsonSerializer[RecordAddedServiceDescriptor],
    JsonSerializer[PartitionAdded]
  )
}
