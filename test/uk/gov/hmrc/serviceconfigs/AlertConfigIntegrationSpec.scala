package uk.gov.hmrc.serviceconfigs

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.ws.WSClient
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.serviceconfigs.model.AlertEnvironmentHandler
import uk.gov.hmrc.serviceconfigs.persistence.AlertEnvironmentHandlerRepository
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class AlertConfigIntegrationSpec
  extends AnyWordSpec
    with DefaultPlayMongoRepositorySupport[AlertEnvironmentHandler]
    with GuiceOneServerPerSuite
    //with WireMockEndpoints
    with Matchers
    with ScalaFutures
    with Eventually {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(1000, Millis)))

  protected val repository: AlertEnvironmentHandlerRepository = app.injector.instanceOf[AlertEnvironmentHandlerRepository]
  private[this] lazy val ws                                   = app.injector.instanceOf[WSClient]


  override def fakeApplication: Application =
    new GuiceApplicationBuilder()
      .disable(classOf[com.kenshoo.play.metrics.PlayModule])
      .configure(
        Map(
          "mongodb.uri"                                       -> mongoUri,
          "metrics.jvm"                                       -> false
        )
      )
      .build()

  "Alert Config Controller" should {
    "return 200 when it starts correctly and receives GET /ping/ping" in {
      val response = ws.url(s"http://localhost:$port/ping/ping").get.futureValue

      response.status shouldBe 200
    }
  }
}
