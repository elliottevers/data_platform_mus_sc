package data

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.{LagomDevModeComponents, LagomDevModeServiceLocatorComponents}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._

abstract class DataApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with CassandraPersistenceComponents
  with LagomKafkaComponents {

  lazy val dataService = serviceClient.implement[DataService]
  override lazy val lagomServer = serverFor[DataService](wire[DataServiceImpl])
  override lazy val jsonSerializerRegistry = DataSerializationRegistry
  clusterSharding.init(
    Entity(Dataset.typeKey) { entityContext =>
      Dataset.apply(entityContext)
    }
  )
}

class DataServiceLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new DataApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new DataApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[DataService])
}
