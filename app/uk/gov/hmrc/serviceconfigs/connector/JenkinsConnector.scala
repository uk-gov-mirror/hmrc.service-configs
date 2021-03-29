/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.serviceconfigs.connector

import play.api.libs.json.{Format, Json, OFormat}


import java.time.LocalDateTime

class JenkinsConnector {

}

object JenkinsConnector {

  sealed trait SeverityType

  object SeverityType {
    case object Ok extends SeverityType
    case object Warning extends SeverityType
    case object Critical extends SeverityType
  }

  case class Threshold(
                        count: Long,
                        severity: String
                      )

  case class HttpThreshold(
                            count: Long,
                            httpStatus: String,
                            severity: SeverityType
                          )

  case class LogMessageThresholds(
                                   count: Int,
                                   lessThanMode: Boolean,
                                   message: String
                                 )

  case class PercentageSplitThreshold(
                                       absoluteThreshold: Int,
                                       crossOver: Int,
                                       errorFilter: String,
                                       excludesSpikes: Int,
                                       hysteresis: Double,
                                       percentageThreshold: Double,
                                       severity: String
                                     )

  case class AlertConfig(
                          app: String,
                          handlers: Seq[String],
                          exceptionThreshold: Int,
                          threshold5xx: Seq[Threshold],
                          percentThreshold5xx: Double,
                          containerKillThreshold: Int,
                          httpStatusThresholds: Seq[Threshold],
                          totalHttpRequestThresholds: Seq[HttpThreshold],
                          logMessageThresholds: Seq[LogMessageThresholds],
                          averageCpuThreshold: Long,
                          absolutePercentageSplitThreshold: Seq[PercentageSplitThreshold]
                        )

  case class Handler(
                      name: String,
                      command: String,
                      filter: String,
                      severities: Seq[SeverityType],
                      `type`: String
                    )


  object AlertConfig {
    import SeverityType.{Ok, Warning, Critical}
    implicit val okSeverityTypeFormat: Format[Ok] = Json.reads[Ok]
    implicit val warningSeverityTypeFormat: Format[SeverityType.Warning] = Json.reads[SeverityType.Warning]
    implicit val criticalSeverityTypeFormat: Format[SeverityType.Critical] = Json.reads[SeverityType.Critical]

    implicit val severityTypeFormat: Format[SeverityType] =
      Json.format[SeverityType]

    implicit val alertConfigFormat: Format[Threshold] =
      Json.format[Threshold]

    implicit val httpThresholdFormat: Format[HttpThreshold] =
      Json.format[HttpThreshold]

    implicit val logMessageThresholdsFormat: Format[LogMessageThresholds] =
      Json.format[LogMessageThresholds]

    implicit val percentageSplitThreshold: Format[PercentageSplitThreshold] =
      Json.format[PercentageSplitThreshold]

    implicit val formats: OFormat[AlertConfig] =
      Json.using[Json.WithDefaultValues].format[AlertConfig]
  }

  object Handler {
    implicit val severityTypeFormat: Format[SeverityType] =
      Json.format[SeverityType]

    implicit val formats: OFormat[Handler] =
      Json.format[Handler]
  }
}







/////////////////////
sealed trait IndicatorType

case object ReadMeIndicatorType extends IndicatorType {
  override def toString: String = "read-me-indicator"
}

case object LeakDetectionIndicatorType extends IndicatorType {
  override def toString: String = "leak-detection-indicator"
}

case object BobbyRuleIndicatorType extends IndicatorType {
  override def toString: String = "bobby-rule-indicator"
}

case object BuildStabilityIndicatorType extends IndicatorType {
  override def toString: String = "build-stability-indicator"
}

object IndicatorType {

  private val indicatorTypes =
    Set(ReadMeIndicatorType, LeakDetectionIndicatorType, BobbyRuleIndicatorType, BuildStabilityIndicatorType)

  def apply(value: String): Option[IndicatorType] = indicatorTypes.find(_.toString == value)

  val format: Format[IndicatorType] = new Format[IndicatorType] {
    override def reads(json: JsValue): JsResult[IndicatorType] =
      json.validate[String].flatMap { str =>
        IndicatorType(str).fold[JsResult[IndicatorType]](JsError(s"Invalid Indicator: $str"))(JsSuccess(_))
      }

    override def writes(o: IndicatorType): JsValue = JsString(o.toString)
  }
}
