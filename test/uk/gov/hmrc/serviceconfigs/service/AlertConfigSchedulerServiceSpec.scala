package uk.gov.hmrc.serviceconfigs.service

import akka.actor.ActorSystem
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.serviceconfigs.connector.JenkinsConnector
import uk.gov.hmrc.serviceconfigs.model.AlertEnvironmentHandler
import uk.gov.hmrc.serviceconfigs.config.JenkinsConfig
import akka.stream.Materializer
import play.api.libs.ws.ahc.AhcWSClient
import play.libs.ws.WSClient
import uk.gov.hmrc.serviceconfigs.persistence.{AlertEnvironmentHandlerRepository, AlertJobNumberRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import java.io.FileInputStream
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AlertConfigSchedulerServiceSpec extends AnyWordSpec with Matchers
  with ScalaFutures with MockitoSugar {



  private val mockAlertJobNumberRepository: AlertJobNumberRepository = mock[AlertJobNumberRepository]
  private val mockAlertEnvironmentHandlerRepository: AlertEnvironmentHandlerRepository = mock[AlertEnvironmentHandlerRepository]
  private val mockJenkinsConnector: JenkinsConnector = mock[JenkinsConnector]


  "AlertConfigSchedulerService.updateConfigs" should {
    "update configs when run for the first time" in {

      val fis = new FileInputStream("./test/resources/happy-output.zip")



      val alertConfigSchedulerService: AlertConfigSchedulerService = new AlertConfigSchedulerService(mockAlertEnvironmentHandlerRepository, mockAlertJobNumberRepository, mockJenkinsConnector)

      when(mockJenkinsConnector.getLatestJob()).thenReturn(Future.successful(Some(1)))
      when(mockAlertJobNumberRepository.findOne()).thenReturn(Future.successful(None))
      when(mockJenkinsConnector.getSensuZip()).thenReturn(Future.successful(fis))
      when(mockAlertEnvironmentHandlerRepository.insert(any[Seq[AlertEnvironmentHandler]])).thenReturn(Future.successful())
      when(mockAlertJobNumberRepository.update(eqTo(1))).thenReturn(Future.successful())

      Await.result( alertConfigSchedulerService.updateConfigs(), Duration.Inf)
    }


//    "Return Indicator with NoReadme result when no readme found" in {
//      when(mockGithubConnector.findReadMe("foo")).thenReturn(Future.successful(None))
//
//      val result = rater.rate("foo")
//
//      result.futureValue mustBe Indicator(ReadMeIndicatorType, Seq(Result(NoReadme, "No Readme defined", None)))
//    }

  }

  "AlertConfigSchedulerService.processZip" should {
    "iterate through files in zip with an input stream and produce a valid SensuConfig" in {

      val fis = new FileInputStream("./test/resources/happy-output.zip")
      val file = AlertConfigSchedulerService.processZip(fis)

      file.alertConfigs.exists(_.app == "accessibility-statement-frontend.public.mdtp") shouldBe true
      file.alertConfigs.exists(_.app == "add-taxes-frontend.public.mdtp") shouldBe true

      file.productionHandler.get("yta") shouldBe Some(Handler("/etc/sensu/handlers/hmrc_pagerduty_multiteam_env_apiv2.rb --team yta -e aws_production"))
      file.productionHandler.get("platform-ui") shouldBe Some(Handler("/etc/sensu/handlers/hmrc_pagerduty_multiteam_env_apiv2.rb --team platform-ui -e aws_production"))
    }

  }

  "AlertConfigSchedulerService.processSensuConfig" should {
    "produce an AlertEnvironmentHandler for a service that has alert config enabled" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp",
        Seq("TEST"))),
        Map("TEST" -> Handler("test/command")))

      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", true))

    }

    "produce an AlertEnvironmentHandler for a service that has Alert Config Disabled" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp",
        Seq("TEST"))),
        Map("TEST" -> Handler("test/noop.rb")))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", false))

    }

    "produce an AlertEnvironmentHandler when a service has No Matching Handler Found in Production" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp", Seq("TEST"))))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", false))

    }

    "produce an AlertEnvironmentHandler for a service that has No Handler Name Defined in Config" in {

      val sensuConfig = SensuConfig(Seq(AlertConfig("test.public.mdtp",
        Seq())),
        Map("TEST" -> Handler("test/command")))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List(AlertEnvironmentHandler("test", false))

    }

    "produce an empty list when there is No Existing Alert Config" in {

      val sensuConfig = SensuConfig(Seq(), Map("TEST" -> Handler("test/command")))
      AlertConfigSchedulerService.processSensuConfig(sensuConfig) shouldBe List.empty

    }


  }





}

// Test the zip
// Write test cases:
// Can we read the file, read the file find alert config(enabled (true), disabled(noop, false), no matching handler name)
//


//s"./src/test/resources/$filename"
//
//
//@RunWith(classOf[JUnitRunner])
//class ZipIntegrationTest extends Specification with TestData {
//
//  "ZipIntegrationTest" should {
//    "Compress a File, and them uncompress it" in {
//      val file1 = getResource("txt/lorem10.txt")
//      val file2 = getResource("img/github2.jpg")
//      val files = file1 :: file2 :: EmptyZip
//      println(files)
//      val zip = files.zipAs("./src/test/resources/stuff.zip")
//      val file = zip.unzipAs("./src/test/resources/stuff")
//      file.exists must_== true
//    }
//
//  }
//
//}