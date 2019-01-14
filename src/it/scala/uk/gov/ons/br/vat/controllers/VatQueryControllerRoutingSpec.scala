package uk.gov.ons.br.vat.controllers

import com.google.inject.{AbstractModule, TypeLiteral}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestData
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, _}
import play.mvc.Http.HttpVerbs.GET
import uk.gov.ons.br.models.Address
import uk.gov.ons.br.repository.QueryRepository
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.vat.controllers.VatQueryControllerRoutingSpec.SampleVatUnit
import uk.gov.ons.br.vat.models.{Vat, VatRef}

import scala.concurrent.Future

/*
 * We are relying on the Play Router to perform request parameter validation for us (in accordance with regex
 * constraints specified in the routes file).
 * This spec tests that the Router is configured correctly for Vat requests.
 *
 * NOTE: MockFactory needs to be the right-most trait to avoid 'assertion failed: Null expectation context -
 * missing withExpectations?' error.
 */
class VatQueryControllerRoutingSpec extends UnitSpec with GuiceOneAppPerTest with MockFactory {
  /*
   * Stub the repository layer so that valid requests find a unit.
   */
  override def newAppForTest(testData: TestData): Application = {
    val queryResult = Right(Some(SampleVatUnit))
    val queryRepository = stub[QueryRepository[VatRef, Vat]]
    (queryRepository.queryByUnitReference _).when(*).returns(Future.successful(queryResult))

    val fakeModule = new AbstractModule {
      override def configure(): Unit = {
        bind(new TypeLiteral[QueryRepository[VatRef, Vat]]() {}).toInstance(queryRepository)
        ()
      }
    }

    new GuiceApplicationBuilder().overrides(fakeModule).build()
  }

  private trait Fixture {
    val ValidLengthVatRef = "142536478376"
  }

  "A request to query a VAT admin unit by VAT reference" - {
    "is rejected when" - {
      "the target VAT reference comprises fewer than 12 characters" in new Fixture {
        val result = route(app, FakeRequest(GET, s"/v1/vat/${ValidLengthVatRef.drop(1)}"))

        status(result.value) shouldBe BAD_REQUEST
      }

      "the target VAT reference comprises more than 12 characters" in new Fixture {
        val result = route(app, FakeRequest(GET, s"/v1/vat/$ValidLengthVatRef" + "X"))

        status(result.value) shouldBe BAD_REQUEST
      }

      "the target VAT reference contains non numeric characters" in new Fixture {
        val result = route(app, FakeRequest(GET, s"/v1/vat/${ValidLengthVatRef.drop(1)}" + "A"))

        status(result.value) shouldBe BAD_REQUEST
      }

      "the target VAT reference contains non alphanumeric characters" in new Fixture {
        val result = route(app, FakeRequest(GET, s"/v1/vat/${ValidLengthVatRef.drop(1)}" + "~"))

        status(result.value) shouldBe BAD_REQUEST
      }
    }

    "is accepted when" - {
      "the target VAT reference comprises 12 numeric characters" in new Fixture {
        val result = route(app, FakeRequest(GET, s"/v1/vat/$ValidLengthVatRef"))

        status(result.value) shouldBe OK
      }
    }
  }
}

private object VatQueryControllerRoutingSpec {
  val SampleVatUnit = Vat(
    vatref = VatRef("065H7Z31732"),
    name = "some-name",
    address = Address(
      line1 = "some-line1",
      line2 = None,
      line3 = None,
      line4 = None,
      line5 = None,
      postcode = "some-postcode"
    ),
    tradingStyle = None,
    legalStatus = "some-legalStatus",
    sic = "some-SIC",
    recordType = "some-RecordType",
    turnover = None,
    lifespan = None,
    links = None)
}