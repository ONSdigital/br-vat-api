package uk.gov.ons.br.vat.test

import uk.gov.ons.br.models.{Address, Lifespan, LinkToLegalUnit}
import SampleVat.Values
import uk.gov.ons.br.vat.models.{Turnover, Vat, VatRef}

trait SampleVat {

  import Values._

  val SampleVatRef = VatRef(Vatref)
  val SampleVatUnitWithAllFields = Vat(
    vatref = SampleVatRef,
    name = Name,
    tradingStyle = Some(TradingStyle),
    legalStatus = LegalStatus,
    sic = Sic,
    recordType = RecordType,
    lifespan = Some(Lifespan(
      birthDate = BirthDate,
      deathDate = Some(DeathDate),
      deathCode = Some(DeathCode)
    )),
    turnover = Some(Turnover(
      amount = Some(TurnoverAmount),
      date = Some(TurnoverDate)
    )),
    address = Address(
      line1 = AddressLine1,
      line2 = Some(AddressLine2),
      line3 = Some(AddressLine3),
      line4 = Some(AddressLine4),
      line5 = Some(AddressLine5),
      postcode = Postcode
    ),
    links = Some(LinkToLegalUnit(ubrn = Ubrn))
  )

  val SampleVatUnitWithOnlyMandatoryFields = Vat(
    vatref = SampleVatRef,
    name = Name,
    tradingStyle = None,
    legalStatus = LegalStatus,
    sic = Sic,
    recordType = RecordType,
    lifespan = None,
    turnover = None,
    address = Address(
      line1 = AddressLine1,
      line2 = None,
      line3 = None,
      line4 = None,
      line5 = None,
      postcode = Postcode
    ),
    links = None
  )

  val SampleVatUnit = SampleVatUnitWithAllFields
}

object SampleVat extends SampleVat {

  object Values {
    val Vatref = "553769640200"
    val Name = "Big Box Cereal Limited"
    val TradingStyle = "Big Box Cereal"
    val LegalStatus = "A"
    val Sic = "10612"
    var RecordType = "0"
    val BirthDate = "11/12/2011"
    val DeathDate = "16/11/2018"
    val DeathCode = "590723"
    val TurnoverAmount = 5008798
    val TurnoverDate = "01/10/2018"
    val AddressLine1 = "Lane Top Farm"
    val AddressLine2 = "1 Bottom Lane"
    val AddressLine3 = "Blackshaw Head"
    val AddressLine4 = "Hebden Bridge"
    val AddressLine5 = "West Yorkshire"
    val Postcode = "SS5 4PR"
    val Ubrn = "1000012345000080"
  }

}