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

package uk.gov.hmrc.serviceconfigs.model

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Json, OFormat, Writes, __}



case class AlertEnvironmentHandler(
                                  serviceName: String,
                                  production: Boolean
                                  )

object AlertEnvironmentHandler {
  implicit val mongoFormats: Format[AlertEnvironmentHandler] =
    ((__ \ "serviceName").format[String]
      ~ (__ \ "production").format[Boolean])(AlertEnvironmentHandler.apply, unlift(AlertEnvironmentHandler.unapply))
}

case class LastJobNumber(jobNumber: Int)

object LastJobNumber {
  val formats: Format[LastJobNumber] =
    Json.format[LastJobNumber]
}


//serviceName: "",
//handlerNames: ["", "", ""],
//production: ["",""],
//integration: ["", ""]
//}
/*

  Scheduled Job > Run every 2 hours --> Jenkins --> Zipped File of (AC)[Alert Configuration] with a linked Build Number

  AC(ID: BuildNumber) [JSON] ::>
    - case class AlertConfig
    - case class Handler

  Each File ::
    - Map Json Document to [AlertConfig || Handler] >> dataDocument
    - processDataDocument(dataDocument)

  processDataDocument ::
    - if Alert Config > project >


*/


/*

{
  "handlers": {
    "C2NI": {
      "command": "/etc/sensu/handlers/hmrc_pagerduty_multiteam_env_apiv2.rb --team C2NI -e aws_production",
      "filter": "occurrences",
      "severities": ["ok", "warning", "critical"],
      "type": "pipe"
    },
    "LISA": {
      "command": "/etc/sensu/handlers/hmrc_pagerduty_multiteam_env_apiv2.rb --team LISA -e aws_production",
      "filter": "occurrences",
      "severities": ["ok", "warning", "critical"],
      "type": "pipe"
    },

*/

// Service Name && handlerNames :: [] && production :: {} && integration :: {} ...

//{
//  lastJobNumber: [BuildJobNumber]
//}
//
//Collection >> [] :: structure {}
//{


//{
//"app": "accessibility-statement-frontend.public.mdtp",
//"handlers": ["platform-ui"],
//"exception-threshold":10,
//"5xx-threshold":{"count":2147483647,"severity":"critical"},
//"5xx-percent-threshold":1.0,
//"containerKillThreshold" : 1,
//"httpStatusThresholds" : [
//    {"count":5,"httpStatus":500,"severity":"critical"},
//    {"count":5,"httpStatus":502,"severity":"critical"},
//    {"count":5,"httpStatus":503,"severity":"critical"},
//    {"count":5,"httpStatus":504,"severity":"critical"}
//],
//"total-http-request-threshold": 2147483647,
//"log-message-thresholds" : [],
//"average-cpu-threshold" : 2147483647,
//"absolute-percentage-split-threshold" : []
//}
//
//___"aws_production.json"___
//
//"platform-ui": {
//"command": "/etc/sensu/handlers/hmrc_pagerduty_multiteam_env_apiv2.rb --team platform-ui -e aws_production",
//"filter": "occurrences",
//"severities": ["ok", "warning", "critical"],
//"type": "pipe"
//},
//
//___"aws_integration.json"___
//"platform-ui": {
//"command": "/etc/sensu/handlers/noop.rb",
//"filter": "occurrences",
//"severities": ["ok", "warning", "critical"],
//"type": "pipe"
//},
//
//"platsec": {
//"command": "/etc/sensu/handlers/hmrc_pagerduty_multiteam_env_apiv2.rb --team platsec -e aws_integration",
//"filter": "occurrences",
//"severities": ["ok", "warning", "critical"],
//"type": "pipe"
//},
//
//withExceptionThreshold(10)
//.withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_500, 10))
//.withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_501, 10))
//.withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_502, 10))
//.withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_503, 10))
//.withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_504, 10))
//.withContainerKillThreshold(1)
//.withLogMessageThreshold("There was a problem during parsing notification", 1)
//.withLogMessageThreshold("No email has been sent for DMSDOC Notification", 1)
//.withHandlers("cds-exports")
//
//
//
//object DDCWLiveServices extends AlertConfig{
//
//  override def environmentConfig: Seq[EnvironmentAlertBuilder] = Seq(
//    EnvironmentAlertBuilder("fhdds").inExternalTest().inProduction(),
//    EnvironmentAlertBuilder("help-to-save").inExternalTest().inProduction(),
//    EnvironmentAlertBuilder("cbcr"),
//    EnvironmentAlertBuilder("ddcwliveservices"),
//    EnvironmentAlertBuilder("ddcwliveservices-agents-and-individuals"),
//    EnvironmentAlertBuilder("cato"),
//    EnvironmentAlertBuilder("ddcw-gmp"),
//    EnvironmentAlertBuilder("ei-ddcw")
//  )
//
//  override def alertConfig: Seq[AlertConfigBuilder] = {
//
//    val ei = teamAlerts(Seq(
//      "ei",
//      "ei-frontend"
//    )).withExceptionThreshold(15)
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_499, 1))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_500, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_501, 1)) // DO NOT CHANGE  DDCNL-870
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_502, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_503, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_504, 5))
//      .withHttp5xxThreshold(20)
//      .withHttp5xxPercentThreshold(150)
//      .withContainerKillThreshold(1)
//      .withHandlers("ei-ddcw")
//
//    val gmpBulk = teamAlerts(Seq(
//      "gmp-bulk"
//    )).withExceptionThreshold(15)
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_499, 1))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_500, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_501, 1)) // DO NOT CHANGE
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_502, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_503, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_504, 5))
//      .withHttp5xxThreshold(20)
//      .withHttp5xxPercentThreshold(150)
//      .withContainerKillThreshold(1)
//      .withHandlers("ddcw-gmp")
//
//    val gmp = teamAlerts(Seq(
//      "gmp"
//    )).withExceptionThreshold(15)
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_499, 2))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_500, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_501, 1)) // DO NOT CHANGE
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_502, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_503, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_504, 15))
//      .withHttp5xxThreshold(60)
//      .withHttp5xxPercentThreshold(10)
//      .withContainerKillThreshold(1)
//      .withHandlers("ddcw-gmp")
//
//    val gmpFrontend = teamAlerts(Seq(
//      "gmp-frontend"
//    )).withExceptionThreshold(25)
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_499, 2))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_500, 15))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_501, 1)) // DO NOT CHANGE
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_502, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_503, 5))
//      .withHttpStatusThreshold(HttpStatusThreshold(HTTP_STATUS_504, 5))
//      .withHttp5xxThreshold(60)
//      .withHttp5xxPercentThreshold(10)
//      .withContainerKillThreshold(1)
//      .withHandlers("ddcw-gmp")
