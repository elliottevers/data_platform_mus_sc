package sample

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import data.{Record, RecordAddedServiceDescriptor}

object SampleSerializationRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Record],
    JsonSerializer[RecordAddedServiceDescriptor]
  )
}
