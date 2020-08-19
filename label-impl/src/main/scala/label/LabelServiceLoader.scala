package label

import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.{LagomDevModeComponents, LagomDevModeServiceLocatorComponents}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._

abstract class LabelApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with CassandraPersistenceComponents
  with LagomKafkaComponents {

  lazy val labelService = serviceClient.implement[LabelService]
  override lazy val lagomServer = serverFor[LabelService](wire[LabelServiceImpl])
  override lazy val jsonSerializerRegistry = LabelSerializationRegistry
}

class LabelServiceLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new LabelApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new LabelApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[LabelService])
}
