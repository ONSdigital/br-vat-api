package uk.gov.ons.br.vat.repository.hbase

import org.slf4j.Logger
import uk.gov.ons.br.models.{Address, Lifespan, LinkToLegalUnit}
import uk.gov.ons.br.repository.Field._
import uk.gov.ons.br.repository.hbase.HBaseRow.asFields
import uk.gov.ons.br.repository.hbase.{HBaseColumn, HBaseRow, HBaseRowMapper}
import uk.gov.ons.br.vat.models.{Turnover, Vat, VatRef}

import scala.util.Try


/*
 * Use the support provided by uk.gov.ons.br.repository.Field to extract & parse fields in a declarative manner.
 *
 * Note that a mapping is not always 1:1.  For example, name & tradingStyle variables are assembled by concatenating
 * multiple database cell values.
 *
 * Note that for optional string fields we use '=' within the body of the for expression (as opposed to a generator
 * '<-') so that we capture the field as an Option value.  By convention we prefix such field values with opt to
 * highlight that they are Option values.  The situation is slightly different when dealing with numeric fields, which
 * return a Try[Option[T]].  We therefore use a generator '<-' so that the for expression is aborted if the value of
 * the field is non-numeric (the Try is a Failure).  Within the body of the for expression, the generated value is now
 * the inner option.
 *
 * Note that in some cases we have a sub-object that consists entirely of optional fields.  If none of the fields are
 * defined we do not want an instance of the sub-object.  The Field helper function 'whenExistsNonEmpty' handles this
 * for us.
 */
private[hbase] object Columns {
  private val columnNameForQualifier: String => String = HBaseColumn.name(family = "d")

  val vatref = columnNameForQualifier("vatref")
  val legalStatus = columnNameForQualifier("legalstatus")
  val sic = columnNameForQualifier("sic92")
  val recordType = columnNameForQualifier("recordtype")
  val name1 = columnNameForQualifier("nameline1")
  val name2 = columnNameForQualifier("nameline2")
  val name3 = columnNameForQualifier("nameline3")
  val tradingStyle1 = columnNameForQualifier("tradstyle1")
  val tradingStyle2 = columnNameForQualifier("tradstyle2")
  val tradingStyle3 = columnNameForQualifier("tradstyle3")

  object Lifespan {
    val birthDate = columnNameForQualifier("birthdate")
    val deathDate = columnNameForQualifier("deathdate")
    val deathCode = columnNameForQualifier("deathcode")
  }

  object Turnover {
    val amount = columnNameForQualifier("turnover")
    val date = columnNameForQualifier("turnoverdate")
  }

  object Address {
    val line1 = columnNameForQualifier("address1")
    val line2 = columnNameForQualifier("address2")
    val line3 = columnNameForQualifier("address3")
    val line4 = columnNameForQualifier("address4")
    val line5 = columnNameForQualifier("address5")
    val postcode = columnNameForQualifier("postcode")
  }

  object Links {
    val ubrn = columnNameForQualifier("ubrn")
  }

}

object VatHBaseRowMapper extends HBaseRowMapper[Vat] {
  /*
   * Return None if a valid Vat unit cannot be constructed from the HBaseRow.
   */
  override def fromRow(row: HBaseRow)(implicit logger: Logger): Option[Vat] = {
    import Columns._
    val fields = asFields(row)
    for {
      vatref <- mandatoryStringNamed(vatref).apply(fields)
      name <- mandatoryConcatenatedStringFrom(name1, name2, name3).apply(fields)
      optTradingStyle = optionalConcatenatedStringFrom(tradingStyle1, tradingStyle2, tradingStyle3).apply(fields)
      legalStatus <- mandatoryStringNamed(legalStatus).apply(fields)
      sic <- mandatoryStringNamed(sic).apply(fields)
      recordType <- mandatoryStringNamed(recordType).apply(fields)
      optLifespan = toLifespan(fields)
      optTurnover <- tryToTurnover(fields).toOption
      address <- toAddress(fields)
      optLinks = toLinks(fields)
    } yield Vat(
      VatRef(vatref),
      name,
      address,
      optTradingStyle,
      legalStatus,
      sic,
      recordType,
      optTurnover,
      optLifespan,
      optLinks
    )
  }

  private def toLifespan(fields: Map[String, String]): Option[Lifespan] = {
    import Columns.Lifespan._
    for {
      birthDate <- optionalStringNamed(birthDate).apply(fields)
      optDeathDate = optionalStringNamed(deathDate).apply(fields)
      optDeathCode = optionalStringNamed(deathCode).apply(fields)
    } yield Lifespan(birthDate, optDeathDate, optDeathCode)
  }

  private def tryToTurnover(fields: Map[String, String])(implicit logger: Logger): Try[Option[Turnover]] = {
    import Columns.Turnover._
    for {
      optAmount <- optionalIntNamed(amount).apply(fields)
    } yield for {
      amount <- optAmount
      optDate = optionalStringNamed(date).apply(fields)
    } yield Turnover(amount, optDate)
  }
  
  private def toAddress(fields: Map[String, String])(implicit logger: Logger): Option[Address] = {
    import Columns.Address._
    for {
      line1 <- mandatoryStringNamed(line1).apply(fields)
      optLine2 = optionalStringNamed(line2).apply(fields)
      optLine3 = optionalStringNamed(line3).apply(fields)
      optLine4 = optionalStringNamed(line4).apply(fields)
      optLine5 = optionalStringNamed(line5).apply(fields)
      postcode <- mandatoryStringNamed(postcode).apply(fields)
    } yield Address(line1, optLine2, optLine3, optLine4, optLine5, postcode)
  }

  private def toLinks(fields: Map[String, String]): Option[LinkToLegalUnit] = {
    import Columns.Links._
    optionalStringNamed(ubrn).apply(fields).map {
      LinkToLegalUnit(_)
    }
  }
}
