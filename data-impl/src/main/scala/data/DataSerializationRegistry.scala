package data

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import data.Dataset.{AddRecord, RecordAdded}

object DataSerializationRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[RecordAdded],
    JsonSerializer[AddRecord]
  )
}
