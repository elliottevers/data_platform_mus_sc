package partitionImpl

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, JsObject, JsResult, JsString, JsSuccess, JsValue, Json}

// COMMANDS
case class GenericReply(message: String)

sealed trait CommandPartition extends ReplyType[GenericReply]

final case class AddPartition(partition: Map[String, String], replyTo: ActorRef[Map[String, String]]) extends CommandPartition

object AddPartition {
  implicit val format: Format[Map[String, String]] = new Format[Map[String, String]] {
    override def reads(json: JsValue): JsResult[Map[String, String]] = {
      JsSuccess(json.as[Map[String, String]])
    }

    override def writes(o: Map[String, String]): JsValue = {
      val converted: Map[String, JsValue] = o.map { case (k,v) => (k, JsString(v))}
      JsObject(converted)
    }
  }
}

case class QueryStatePartition(anything: String, replyTo: ActorRef[Map[String, String]]) extends CommandPartition
object QueryStatePartition {

}


// EVENTS
sealed trait EventPartition extends AggregateEvent[EventPartition] {
  override def aggregateTag: AggregateEventTagger[EventPartition] = EventPartition.Tag
}

object EventPartition {
  val Tag: AggregateEventShards[EventPartition] = AggregateEventTag.sharded[EventPartition](numShards = 3)
}

final case class PartitionAdded(partition: Map[String, String]) extends EventPartition

object PartitionAdded {
  implicit val format: Format[PartitionAdded] = Json.format
}


case class StatePartition(partition: Map[String, String]) {
  def applyCommand(cmd: CommandPartition): Effect[EventPartition, StatePartition] = {
    cmd match {
      case AddPartition(p, replyTo) =>
        Effect
          .persist[PartitionAdded, StatePartition](PartitionAdded(p))
          .thenReply(replyTo)(partition => partition.partition)
      case QueryStatePartition(_, replyTo) =>
        Effect
          .reply(replyTo)(partition)
    }
  }

  def applyEvent(evt: EventPartition): StatePartition =
    evt match {
      case PartitionAdded(p) => this.copy(partition = p)
    }
}


final class Partition extends PersistentEntity {

  override type Command = CommandPartition
  override type Event = EventPartition
  override type State = StatePartition

  override def initialState: StatePartition = Partition.empty

  override def behavior: Behavior = {
    case _ => Actions()
  }
}

object Partition {

  val empty: StatePartition = StatePartition(Map[String, String]())

  val typeKey: EntityTypeKey[CommandPartition] = EntityTypeKey[CommandPartition]("Partition")

  def apply(entityContext: EntityContext[CommandPartition]): EventSourcedBehavior[CommandPartition, EventPartition, StatePartition] = {
    EventSourcedBehavior.apply(
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      Partition.empty,
      (partition, cmd) => partition.applyCommand(cmd),
      (partition, evt) => partition.applyEvent(evt)
    )
  }

  // TODO: ... this is probably necessary
  //  implicit val format: Format[DatasetDynamic] = Json.format
}