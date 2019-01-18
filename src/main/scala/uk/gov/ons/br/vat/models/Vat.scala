package uk.gov.ons.br.vat.models

import play.api.libs.json.{Json, Writes}
import uk.gov.ons.br.models.{Address, Lifespan, LinkToLegalUnit}

/*
 * Only internally validated VAT references should be promoted to the PayeRef type.
 */
case class Vat(vatref: VatRef,
               name: String,
               address: Address,
               tradingStyle: Option[String],
               legalStatus: String,
               sic: String,
               recordType: String,
               turnover: Option[Turnover],
               lifespan: Option[Lifespan],
               links: Option[LinkToLegalUnit])

object Vat {
  implicit val writes: Writes[Vat] = Json.writes[Vat]
}
