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

import com.google.common.io.BaseEncoding
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.serviceconfigs.config.JenkinsConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class JenkinsConnector @Inject() (config: JenkinsConfig, http: HttpClient) {

//  def getZip(baseUrl: String)(implicit ec: ExecutionContext): Future[Option[???]] = {
//
//    // Stops Server Side Request Forgery
//    assert(baseUrl.startsWith(config.jenkinsHost))
//
//    val authorizationHeader =
//      s"Basic ${BaseEncoding.base64().encode(s"${config.username}:${config.token}".getBytes("UTF-8")}
//
//
//    implicit val hc: HeaderCarrier = HeaderCarrier()
//    val url                        = url"${baseUrl} ???
//
//    http
//      .GET[Option[???]](
//        url = url,
//        headers = Seq("Authorization" -> authorizationHeader)
//      )

}

object JenkinsConnector {

//  sealed trait SeverityType
//
//  object SeverityType {
//    case object Ok extends SeverityType
//    case object Warning extends SeverityType
//    case object Critical extends SeverityType
//  }

//  case class Threshold(
//                        count: Long,
//                        severity: String
//                      )
//
//  case class HttpThreshold(
//                            count: Long,
//                            httpStatus: String,
//                            severity: SeverityType
//                          )
//
//  case class LogMessageThresholds(
//                                   count: Int,
//                                   lessThanMode: Boolean,
//                                   message: String
//                                 )
//
//  case class PercentageSplitThreshold(
//                                       absoluteThreshold: Int,
//                                       crossOver: Int,
//                                       errorFilter: String,
//                                       excludesSpikes: Int,
//                                       hysteresis: Double,
//                                       percentageThreshold: Double,
//                                       severity: String
//                                     )

  case class AlertConfig(
                          app: String,
                          handlers: Seq[String],
                          exceptionThreshold: Int,
                          threshold5xx: Seq[String],
                          percentThreshold5xx: Double,
                          containerKillThreshold: Int,
                          httpStatusThresholds: Seq[String],
                          totalHttpRequestThresholds: Seq[String],
                          logMessageThresholds: Seq[String],
                          averageCpuThreshold: Long,
                          absolutePercentageSplitThreshold: Seq[String]
                        )

  object AlertConfig {
    implicit val formats: OFormat[AlertConfig] =
      Json.format[AlertConfig]
  }

  case class Handler(
                      name: String,
                      command: String,
                      filter: String,
                      severities: Seq[String],
                      `type`: String
                    )

  object Handler {
    implicit val formats: OFormat[Handler] =
      Json.format[Handler]
  }

}
