package label

import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.Future

class LabelServiceImpl extends LabelService {
  override def createLabel: ServiceCall[LabelRecording, String] = ServiceCall { request =>
    println("logging from createLabel endpoint")
    println(request)
    Future.successful("Hi")
  }
}
