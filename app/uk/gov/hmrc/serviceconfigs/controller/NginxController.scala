/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.serviceconfigs.controller

import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.serviceconfigs.model.FrontendRoutes
import uk.gov.hmrc.serviceconfigs.persistence.FrontendRouteRepo

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
@Api("Nginx Routes")
class NginxController @Inject()(db: FrontendRouteRepo,
                                mcc: MessagesControllerComponents)
    extends BackendController(mcc) {

  implicit val formats: OFormat[FrontendRoutes] = Json.format[FrontendRoutes]

  @ApiOperation(
    value = "Retrieves nginx route config for the given service",
    notes =
      """The nginx rules are extracted from the mdtp-frontend-routes repo"""
  )
  def searchByServiceName(
    @ApiParam(value = "The service name to query") serviceName: String
  ) = Action.async { implicit request =>
    db.findByService(serviceName)
      .map(FrontendRoutes.fromMongo)
      .map(routes => Json.toJson(routes))
      .map(Ok(_))
  }

  @ApiOperation(
    value = "Retrieves nginx route config for the given environment",
    notes =
      """The nginx rules are extracted from the mdtp-frontend-routes repo"""
  )
  def searchByEnvironment(
                           @ApiParam(value = "The environment to query") environment: String
                         ) = Action.async { implicit request =>
    db.findAllRoutes()
      .map(_.filter(_.environment == environment))
      .map(FrontendRoutes.fromMongo)
      .map(routes => Json.toJson(routes))
      .map(Ok(_))
  }

  @ApiOperation(
    value =
      "Retrieves nginx route config after doing a search for the given frontEnd path",
    notes =
      """The nginx rules are extracted from the mdtp-frontend-routes repo"""
  )
  def searchByFrontendPath(
    @ApiParam(value = "The front end path to search by") frontendPath: String
  ) = Action.async { implicit request =>
    db.searchByFrontendPath(frontendPath)
      .map(FrontendRoutes.fromMongo)
      .map(routes => Json.toJson(routes))
      .map(Ok(_))
  }

}
