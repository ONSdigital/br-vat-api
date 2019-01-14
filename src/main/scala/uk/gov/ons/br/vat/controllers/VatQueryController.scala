package uk.gov.ons.br.vat.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.ons.br.actions.QueryActionMaker
import uk.gov.ons.br.http.QueryResultHandler
import uk.gov.ons.br.vat.models.{Vat, VatRef}

/*
 * In order for the router to invoke methods with arguments that are instances of some model type T, a
 * suitable PathBindable[T] must be defined.
 * We leave validation of path parameters to the router (via regex constraints).
 */
@Singleton
class VatQueryController @Inject()(protected val controllerComponents: ControllerComponents,
                                   queryAction: QueryActionMaker[VatRef, Vat],
                                   responseFor: QueryResultHandler[Vat]) extends BaseController {
  def byReference(vatRef: VatRef): Action[AnyContent] = queryAction.byUnitReference(vatRef) { request =>
    responseFor(request.queryResult)
  }
}
