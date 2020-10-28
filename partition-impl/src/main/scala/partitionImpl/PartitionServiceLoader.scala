package partitionImpl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.{LagomDevModeComponents, LagomDevModeServiceLocatorComponents}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._
import dataImpl.DatasetStatic
import partitionApi.PartitionService

abstract class PartitionApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with CassandraPersistenceComponents
  with LagomKafkaComponents {

  lazy val labelService = serviceClient.implement[PartitionService]
  override lazy val lagomServer = serverFor[PartitionService](wire[PartitionServiceImpl])
  override lazy val jsonSerializerRegistry = PartitionSerializationRegistry

  clusterSharding.init(
    Entity(Partition.typeKey) { entityContext =>
      Partition.apply(entityContext)
    }
  )

  clusterSharding.init(
    Entity(DatasetStatic.typeKey) { entityContext =>
      DatasetStatic.apply(entityContext)
    }
  )
}

class PartitionServiceLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new PartitionApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new PartitionApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[PartitionService])
}
