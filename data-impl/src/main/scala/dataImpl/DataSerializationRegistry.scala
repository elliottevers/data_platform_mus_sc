package dataImpl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import dataApi.EmptyRequest

object DataSerializationRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[DatasetDynamicAdded],
    JsonSerializer[AddDatasetDynamic],
    JsonSerializer[EmptyRequest],
  )
}
