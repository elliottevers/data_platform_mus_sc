package label

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

trait LabelService extends Service {

  def createLabel: ServiceCall[LabelRecording, String]

  override final def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    // @formatter:off
    named("label")
      .withCalls(
        restCall(
          Method.POST,
          "/label",
          createLabel
        )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

object SongFormat extends Enumeration {
  type SongFormat = Value

  val ABAB = Value("ABAB")
  val AABA = Value("AABA")

  implicit val format = Json.formatEnum(this)
}

object LocationNarrative extends Enumeration {
  type LocationNarrative = Value

  val Hook = Value("Hook")
  val Exposition = Value("Exposition")

  implicit val format = Json.formatEnum(this)
}

object SongType extends Enumeration {
  type SongType = Value

  val iAm = Value("iAm")
  val iWant = Value("iWant")

  implicit val format = Json.formatEnum(this)
}

object TimeSignature extends Enumeration {
  type TimeSignature = Value

  val Common = Value("Common")
  val Waltz = Value("Waltz")
  val SixEight = Value("SixEight")

  implicit val format = Json.formatEnum(this)
}

object Tempo extends Enumeration {
  type Tempo = Value

  val Largo = Value("Largo")
  val Moderato = Value("Moderato")

  implicit val format = Json.formatEnum(this)
}

case class Number(uri: String, secondsStart: Int, secondsEnd: Int, timeSignature: TimeSignature.Value, location: LocationNarrative.Value)

case class Standalone(format: SongFormat.Value, tempo: Tempo.Value, locationHypothetical: LocationNarrative.Value)

object Recording extends Enumeration {
  type Recording = Value

  val Number = Value("Number")
  val Standalone = Value("Standalone")

  implicit val format = Json.formatEnum(this)
}

case class LabelSongbook(title: String, tempo: Tempo.Value, timeSignature: TimeSignature.Value, format: SongFormat.Value, locationPredicted: LocationNarrative.Value)

case class LabelRecording(title: String, tempo: Tempo.Value, timeSignature: TimeSignature.Value, format: SongFormat.Value, locationPredicted: LocationNarrative.Value)

// TODO: sections: Seq[Section]

object LabelRecording {
  implicit val format: Format[LabelRecording] = Json.format[LabelRecording]
}

sealed trait SectionLabel
case object A extends SectionLabel
case object B extends SectionLabel
case object C extends SectionLabel

case class Phrase(length: Int, narrativeFunction: String)

case class Section(label: SectionLabel, phrases: Seq[Phrase])
