package sample

import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.devmode.{LagomDevModeComponents, LagomDevModeServiceLocatorComponents}
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._
import data.DataService

abstract class SampleApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with LagomKafkaClientComponents
{

  lazy val sampleService = serviceClient.implement[SampleService]
  override lazy val lagomServer = serverFor[SampleService](wire[SampleServiceImpl])
  lazy val dataService = serviceClient.implement[DataService]
}

class SampleServiceLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new SampleApplication(context) with LagomDevModeServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new SampleApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[SampleService])
}
