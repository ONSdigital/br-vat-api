package uk.gov.ons.br.vat.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.ons.br.actions.EditAction
import uk.gov.ons.br.http.PatchResultHandler
import uk.gov.ons.br.models.patch.Patch
import uk.gov.ons.br.services.PatchService
import uk.gov.ons.br.services.PatchService.PatchDescriptor
import uk.gov.ons.br.vat.models.VatRef

import scala.concurrent.ExecutionContext

@Singleton
class VatEditController @Inject() (protected val controllerComponents: ControllerComponents,
                                   editAction: EditAction,
                                   patchService: PatchService[VatRef],
                                   toPatchResult: PatchResultHandler) extends BaseController {
  private implicit val executionContext: ExecutionContext = this.defaultExecutionContext

  def applyPatch(vatRef: VatRef): Action[Patch] = editAction.async { request =>
    val patch = PatchDescriptor(editedBy = request.userId, operations = request.body)
    patchService.applyPatchTo(vatRef, patch).map(toPatchResult)
  }
}
