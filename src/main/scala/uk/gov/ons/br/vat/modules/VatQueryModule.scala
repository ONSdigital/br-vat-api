package uk.gov.ons.br.vat.modules


import com.github.ghik.silencer.silent
import com.google.inject.{AbstractModule, Provides}
import javax.inject.Inject
import play.api.mvc.PlayBodyParsers
import play.api.{Configuration, Environment}
import uk.gov.ons.br.actions.{DefaultQueryActionMaker, QueryActionMaker}
import uk.gov.ons.br.http.{JsonQueryResultHandler, QueryResultHandler}
import uk.gov.ons.br.repository.QueryRepository
import uk.gov.ons.br.vat.models.{Vat, VatRef}

import scala.concurrent.ExecutionContext

/*
 * This class must be listed in application.conf under 'play.modules.enabled' for this to be used.
 */
@SuppressWarnings(Array("UnusedMethodParameter"))
class VatQueryModule(@silent environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = ()

  @Provides
  def providesVatQueryActionMaker(@Inject() bodyParsers: PlayBodyParsers, queryRepository: QueryRepository[VatRef, Vat])
                                 (implicit ec: ExecutionContext): QueryActionMaker[VatRef, Vat] =
    new DefaultQueryActionMaker[VatRef, Vat](bodyParsers.default, queryRepository)

  @Provides
  def providesVatQueryResultHandler: QueryResultHandler[Vat] =
    new JsonQueryResultHandler[Vat]()
}
