package partitionImpl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import partitionApi.CreatePartitionRequest

object PartitionSerializationRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[CreatePartitionRequest],
    JsonSerializer[PartitionAdded]
  )
}
