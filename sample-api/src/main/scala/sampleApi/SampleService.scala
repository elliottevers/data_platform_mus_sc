package sampleApi

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json, Reads}

trait SampleService extends Service {

  def sampleRecord(guidPartition: String, environment: String): ServiceCall[NotUsed, Sample]

  override final def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    // @formatter:off
    named("sampleApi")
      .withCalls(
        pathCall(
          "/sample/:guidPartition?environment",
          sampleRecord(_, _)
        ),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

case class Sample(link: String)
object Sample {
  implicit val format: Format[Sample] = Json.format[Sample]
  implicit val readsListSample: Reads[Seq[Sample]] = Reads.seq(implicitly)
}