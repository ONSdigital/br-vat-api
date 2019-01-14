package uk.gov.ons.br.vat.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import uk.gov.ons.br.actions.{DefaultQueryActionMaker, QueryActionMaker}
import uk.gov.ons.br.repository.{QueryRepository, QueryResult}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.vat.models.{Vat, VatRef}
import uk.gov.ons.br.vat.test.SampleVat.{SampleVatRef, SampleVatUnit}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, reflectiveCalls}

class VatQueryControllerSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerTest {

  private trait Fixture extends StubControllerComponentsFactory {
    val queryResultHandler = mockFunction[QueryResult[Vat], Result]
  }

  private trait MockActionFixture extends Fixture {
    /*
     * Unfortunately we cannot simply mock QueryActionMaker.  Attempting to do so results in an incompatible type
     * error - possibly because QueryRequest is defined inside the trait.
     * Instead we create our own fake implementation and delegate to a mockFunction.
     */
    val queryActionMaker = new QueryActionMaker[VatRef, Vat] {

      // ScalaMock cannot stub 'ActionBuilder[QueryRequest, AnyContent] with ActionTransformer[Request, QueryRequest]'
      trait ActionBuilderWithActionTransformer extends ActionBuilder[QueryRequest, AnyContent] with ActionTransformer[Request, QueryRequest]

      val anAction = stub[ActionBuilderWithActionTransformer]
      val delegateFn = mockFunction[VatRef, ActionBuilderWithActionTransformer]

      override def byUnitReference(unitRef: VatRef): ActionBuilder[QueryRequest, AnyContent] with ActionTransformer[Request, QueryRequest] =
        delegateFn(unitRef)
    }
    val underTest = new VatQueryController(stubControllerComponents(), queryActionMaker, queryResultHandler)
  }

  /*
   * While Actions are the recommended element of reuse in Play, it does not seem particularly easy to mock/stub
   * their interactions.  We resort to using a real instance here.
   */
  private trait QueryActionFixture extends Fixture {
    implicit val executionContext = ExecutionContext.Implicits.global
    implicit val materializer = app.materializer
    val queryRepository = mock[QueryRepository[VatRef, Vat]]
    val queryActionMaker = new DefaultQueryActionMaker(stubPlayBodyParsers.default, queryRepository)
    val underTest = new VatQueryController(stubControllerComponents(), queryActionMaker, queryResultHandler)
  }

  "A Query Controller" - {
    "requests the target unit via the query action" in new MockActionFixture {
      queryActionMaker.delegateFn.expects(SampleVatRef).returning(queryActionMaker.anAction)

      underTest.byReference(SampleVatRef)
    }

    "generates a response via a query result handler" in new QueryActionFixture {
      val queryResult = Right(Some(SampleVatUnit))
      val handlerResult = Ok("some-representation-of-sample-vat-unit")
      (queryRepository.queryByUnitReference _).expects(SampleVatRef).returning(Future.successful(queryResult))
      queryResultHandler.expects(queryResult).returning(handlerResult)

      whenReady(underTest.byReference(SampleVatRef)(FakeRequest())) { result =>
        result shouldBe handlerResult
      }
    }
  }
}
