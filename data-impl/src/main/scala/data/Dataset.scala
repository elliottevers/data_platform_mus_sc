package data

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag, AggregateEventTagger, AkkaTaggerAdapter}
import data.Dataset.RecordAdded
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

object Dataset {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class AddRecord(record: Record) extends Command

  implicit val addRecordFormat: Format[AddRecord] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 3)
  }

  final case class RecordAdded(record: Record) extends Event

  implicit val recordAddedFormat: Format[RecordAdded] = Json.format

  val empty: Dataset = Dataset(records = List())

  // TODO: what is the purpose of this?
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Dataset")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, Dataset] = {
    // TODO: enforced replies
//    EventSourcedBehavior.withEnforcedReplies(
//      persistenceId = persistenceId,
//      emptyState = Dataset.empty,
//      commandHandler = (dataset, cmd) => dataset.applyCommand(cmd),
//      eventHandler = (dataset, evt) => dataset.applyEvent()
//    )
    EventSourcedBehavior.apply(
      persistenceId = persistenceId,
      emptyState = Dataset.empty,
      commandHandler = (dataset, cmd) => dataset.applyCommand(cmd),
      eventHandler = (dataset, evt) => dataset.applyEvent(evt)
    )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 10, keepNSnapshots = 5))

  /**
   * The aggregate get snapshoted every configured number of events. This
   * means the state gets stored to the database, so that when the entity gets
   * loaded, you don't need to replay all the events, just the ones since the
   * snapshot. Hence, a JSON format needs to be declared so that it can be
   * serialized and deserialized when storing to and from the database.
   */
  implicit val datasetFormat: Format[Dataset] = Json.format
}

// State
final case class Dataset(records: Seq[Record]) {

  def applyCommand(cmd: Dataset.Command): ReplyEffect[Dataset.Event, Dataset] = {
    cmd match {
      // TODO: validations and replies
      case Dataset.AddRecord(record) =>
        Effect
          .persist(RecordAdded(record))
          .thenNoReply()
    }
  }

  def applyEvent(evt: Dataset.Event): Dataset =
    evt match {
      case RecordAdded(record) => appendRecord(record)
    }

  private def appendRecord(record: Record): Dataset = {
    copy(records = records :+ record)
  }
}
