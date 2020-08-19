package sample

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import data.Record

trait SampleService extends Service {

  def sampleRecord: ServiceCall[String, Record]

  override final def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    // @formatter:off
    named("sample")
      .withCalls(
        restCall(
          Method.POST,
          "/sample",
          sampleRecord
        )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
