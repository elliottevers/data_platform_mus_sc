package sampleImpl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.broker.kafka.{LagomKafkaClientComponents, LagomKafkaComponents}
import com.lightbend.lagom.scaladsl.devmode.{LagomDevModeComponents, LagomDevModeServiceLocatorComponents}
import com.lightbend.lagom.scaladsl.persistence.cassandra.WriteSideCassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.ReadSideJdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.ReadSideSlickPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._
import dataApi.DataService
import partitionApi.PartitionService
import partitionImpl.Partition
import play.api.db.HikariCPComponents
import sampleApi.SampleService

abstract class SampleApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with WriteSideCassandraPersistenceComponents
  with LagomKafkaClientComponents
  with LagomKafkaComponents
  with ReadSideJdbcPersistenceComponents
  with HikariCPComponents
  with ReadSideSlickPersistenceComponents
{

  wire[PartitionConsumer]
  readSide.register(wire[PartitionProcessor])

  // NB: equivalent
//  readSide.register[EventPartition](new PartitionProcessor(slickReadSide, new PartitionRepository))
  lazy val partitionRepository = wire[PartitionRepository]

  lazy val sampleService = serviceClient.implement[SampleService]
  override lazy val lagomServer = serverFor[SampleService](wire[SampleServiceImpl])
  lazy val dataService = serviceClient.implement[DataService]
  lazy val partitionService = serviceClient.implement[PartitionService]


  override lazy val jsonSerializerRegistry = SampleSerializationRegistry

  clusterSharding.init(
    Entity(Partition.typeKey) { entityContext =>
      Partition.apply(entityContext)
    }
  )
}

class SampleServiceLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new SampleApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new SampleApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[SampleService])
}
