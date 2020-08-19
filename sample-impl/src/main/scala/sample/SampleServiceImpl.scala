package sample

import akka.Done
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.Future
import akka.stream.scaladsl.Flow
import data.{DataService, Record, RecordAddedServiceDescriptor}

class SampleServiceImpl(dataService: DataService) extends SampleService {
  override def sampleRecord: ServiceCall[String, Record] = ServiceCall { request =>
    println("logging from sample service")
    println(request)
    Future.successful(Record(1))
  }

  dataService.topicRecordAdded().subscribe.atLeastOnce(
    Flow[RecordAddedServiceDescriptor].mapAsync(1) {
      case RecordAddedServiceDescriptor(r) =>
        println(s"logging $r from sample service")
        Future.successful(Done)
      case other =>
        println(s"logging $other from sample service")
        Future.successful(Done)
    }
  )
}
