package dataImpl

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag, AggregateEventTagger, AkkaTaggerAdapter, PersistentEntity}
import play.api.libs.json.{Format, Json}


// COMMANDS
case class GenericReply(message: String)

sealed trait CommandDataset extends ReplyType[GenericReply]

final case class AddDatasetDynamic(linksPlaylist: Seq[String], name: String) extends CommandDataset

object AddDatasetDynamic {
  implicit val format: Format[AddDatasetDynamic] = Json.format
}

final case class AddDatasetStatic(linksVideos: Seq[String], name: String) extends CommandDataset

object AddDatasetStatic {
  implicit val format: Format[AddDatasetStatic] = Json.format
}

case class QueryStateStaticDataset(anything: String, replyTo: ActorRef[Seq[String]]) extends CommandDataset
object QueryStateStaticDataset {

}

case class QueryStateDynamicDataset(anything: String, replyTo: ActorRef[String]) extends CommandDataset
object QueryStateDynamicDataset {

}


// EVENTS
sealed trait EventDataset extends AggregateEvent[EventDataset] {
  override def aggregateTag: AggregateEventTagger[EventDataset] = Event.Tag
}

object Event {
  val Tag: AggregateEventShards[EventDataset] = AggregateEventTag.sharded[EventDataset](numShards = 3)
}

final case class DatasetDynamicAdded(linksPlaylist: Seq[String], name: String) extends EventDataset

object DatasetDynamicAdded {
  implicit val format: Format[DatasetDynamicAdded] = Json.format
}

final case class DatasetStaticAdded(linksVideos: Seq[String], name: String) extends EventDataset

object DatasetStaticAdded {
  implicit val format: Format[DatasetStaticAdded] = Json.format
}


sealed trait StateDataset

case class StateDynamicDataset(linksPlaylist: Seq[String], name: String) extends StateDataset {
    def applyCommand(cmd: CommandDataset): Effect[EventDataset, StateDynamicDataset] = {
      cmd match {
        case AddDatasetDynamic(linksNew, name) =>
          Effect
            .persist(DatasetDynamicAdded(linksNew, name))
            .thenNoReply()
        case QueryStateDynamicDataset(_, replyTo) =>
          Effect.reply(replyTo)(linksPlaylist.toString)
      }
    }

    def applyEvent(evt: EventDataset): StateDynamicDataset =
      evt match {
        case DatasetDynamicAdded(ls, n) => this.copy(linksPlaylist = ls, name = n)
      }
}

case class StateStaticDataset(linksVideos: Seq[String], name: String) extends StateDataset {
  def applyCommand(cmd: CommandDataset): Effect[EventDataset, StateStaticDataset] = {
    cmd match {
      case AddDatasetStatic(ls, n) =>
        Effect
          .persist(DatasetStaticAdded(ls, n))
          .thenNoReply()
      case QueryStateStaticDataset(_, replyTo) =>
        Effect.reply(replyTo)(linksVideos)
    }
  }

  def applyEvent(evt: EventDataset): StateStaticDataset =
    evt match {
      case DatasetStaticAdded(ls, n) => this.copy(linksVideos = ls, name = n)
    }
}


final class DatasetDynamic extends PersistentEntity {

  override type Command = CommandDataset
  override type Event = EventDataset
  override type State = StateDynamicDataset

  override def initialState: StateDynamicDataset = DatasetDynamic.empty

  override def behavior: Behavior = {
    case _ => Actions()
//      Actions()
//        .onCommand[CommandDataset, GenericReply] {
//          case (AddDatasetDynamic(link), ctx, state) =>
//            ctx.thenPersist(DatasetDynamicAdded(link)) { evt =>
//              ctx.reply(GenericReply("added"))
//            }
//        }.onEvent {
//          case (DatasetDynamicAdded(link), state) =>
//            StateDynamicDataset(link)
//        }.onReadOnlyCommand[CommandDataset, GenericReply] {
//          case (QueryState(_, replyTo), ctx, state) =>
//            ctx.reply(GenericReply(state.linkPlaylist))
//        }
  }
}

object DatasetDynamic {

  val empty: StateDynamicDataset = StateDynamicDataset(List(), "Unnamed")

  val typeKey: EntityTypeKey[CommandDataset] = EntityTypeKey[CommandDataset]("DatasetDynamic")

  def apply(entityContext: EntityContext[CommandDataset]): EventSourcedBehavior[CommandDataset, EventDataset, StateDynamicDataset] = {
    EventSourcedBehavior.apply(
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      DatasetDynamic.empty,
      (datasetDynamic, cmd) => datasetDynamic.applyCommand(cmd),
      (datasetDynamic, evt) => datasetDynamic.applyEvent(evt)
    )
  }

  // TODO: ... this is probably necessary
//  implicit val format: Format[DatasetDynamic] = Json.format
}


final class DatasetStatic extends PersistentEntity {

  override type Command = CommandDataset
  override type Event = EventDataset
  override type State = StateStaticDataset

  override def initialState: StateStaticDataset = DatasetStatic.empty

  override def behavior: Behavior = {
    case _ => Actions()
//      Actions()
//        .onCommand[CommandDataset, GenericReply] {
//        case (AddDatasetStatic(link), ctx, state) =>
//          ctx.thenPersist(DatasetStaticAdded(link)) { evt =>
//            ctx.reply(GenericReply("added"))
//          }
//      }.onEvent {
//        case (DatasetStaticAdded(linksVideos), state) =>
//          StateStaticDataset(linksVideos)
//      }.onReadOnlyCommand[CommandDataset, GenericReply] {
//        case (QueryState(_, replyTo), ctx, state) =>
//          ctx.reply(GenericReply(s"${state.linksVideos.length} videos added"))
//      }
  }
}

object DatasetStatic {

  val empty: StateStaticDataset = StateStaticDataset(List(), "Unnamed")

  val typeKey: EntityTypeKey[CommandDataset] = EntityTypeKey[CommandDataset]("DatasetStatic")

  def apply(entityContext: EntityContext[CommandDataset]): EventSourcedBehavior[CommandDataset, EventDataset, StateStaticDataset] = {
    EventSourcedBehavior.apply(
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      DatasetStatic.empty,
      (DatasetStatic, cmd) => DatasetStatic.applyCommand(cmd),
      (DatasetStatic, evt) => DatasetStatic.applyEvent(evt)
    )
  }

  // TODO: ... this is probably necessary
  //  implicit val format: Format[DatasetDynamic] = Json.format
}
