package uk.gov.ons.br.vat.models

import play.api.libs.json.{Json, Writes}


case class Turnover(amount: Int,
                    date: Option[String])

object Turnover {
  implicit val writes: Writes[Turnover] = Json.writes[Turnover]
}

