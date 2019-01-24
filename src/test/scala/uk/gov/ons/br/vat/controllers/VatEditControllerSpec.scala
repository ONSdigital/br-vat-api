package uk.gov.ons.br.vat.controllers


import akka.util.ByteString
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.JsString
import play.api.libs.streams.Accumulator
import play.api.mvc.Results.{BadRequest, ImATeapot}
import play.api.mvc._
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import uk.gov.ons.br.actions.EditAction
import uk.gov.ons.br.actions.EditAction.UserIdHeaderName
import uk.gov.ons.br.models.patch.{Patch, TestOperation}
import uk.gov.ons.br.vat.controllers.VatEditControllerSpec._
import uk.gov.ons.br.vat.models.VatRef
import uk.gov.ons.br.vat.test.SampleVat.SampleVatRef
import uk.gov.ons.br.services.PatchService
import uk.gov.ons.br.services.PatchService.{PatchApplied, PatchDescriptor, PatchStatus}
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class VatEditControllerSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerTest {

  private trait Fixture extends StubControllerComponentsFactory {
    implicit val executionContext = ExecutionContext.Implicits.global
    implicit val materializer = app.materializer

    val requestHeader = stub[RequestHeader]
    val bodyParser = mock[BodyParser[Patch]]
    val patchService = mock[PatchService[VatRef]]
    val patchHandler = mockFunction[PatchStatus, Result]
    /*
     * While Actions are the recommended element of reuse in Play, it does not seem particularly easy to mock/stub
     * their interactions.  We resort to using a real instance here.
     */
    val editAction = new EditAction(bodyParser)
    val underTest = new VatEditController(stubControllerComponents(), editAction, patchService, patchHandler)
  }

  "An Edit Controller" - {
    "delegates to the editAction, which returns BadRequest when the X-User-Id header is not defined" in new Fixture {
      val request = fakePatchRequest(SamplePatchOperations)

      whenReady(underTest.applyPatch(SampleVatRef)(request)) { result =>
        result shouldBe BadRequest
      }
    }

    "delegates to the editAction, and then the service and handler to process the request when the X-User-Id header is defined" in new Fixture {
      (bodyParser.apply _).expects(requestHeader).returning(Accumulator.done(Right(SamplePatchOperations)))
      (requestHeader.withBody[Patch] _).when(SamplePatchOperations).returns(fakePatchRequest(SamplePatchOperations).withHeaders(SomeEditedByHeaders))
      val patch = PatchDescriptor(editedBy = UserId, operations = SamplePatchOperations)
      (patchService.applyPatchTo _).expects(SampleVatRef, patch).returning(Future.successful(SomePatchStatus))
      patchHandler.expects(SomePatchStatus).returning(SomeHttpStatus)

      val patchAction = underTest.applyPatch(SampleVatRef)

      whenReady(patchAction.apply(requestHeader).run(ByteString("some-request-body"))) { result =>
        result shouldBe SomeHttpStatus
      }
    }
  }
}

private object VatEditControllerSpec {
  val SamplePatchOperations = Seq(TestOperation(path = "/foo", value = JsString("bar")))
  val UserId = "doej"
  val SomeEditedByHeaders = Headers(UserIdHeaderName -> UserId)
  val SomePatchStatus = PatchApplied
  val SomeHttpStatus = ImATeapot

  def fakePatchRequest(patch: Patch): Request[Patch] =
    FakeRequest().withBody(patch)
}
