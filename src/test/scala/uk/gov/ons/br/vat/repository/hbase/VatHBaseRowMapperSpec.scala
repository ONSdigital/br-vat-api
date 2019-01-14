package uk.gov.ons.br.vat.repository.hbase

import org.scalamock.scalatest.MockFactory
import org.slf4j.Logger
import uk.gov.ons.br.models.Lifespan
import uk.gov.ons.br.repository.hbase.{HBaseCell, HBaseColumn, HBaseRow}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.vat.repository.hbase.VatHBaseRowMapperSpec._
import uk.gov.ons.br.vat.test.SampleVat.SampleVatUnitWithOnlyMandatoryFields
import uk.gov.ons.br.vat.test.SampleVat.Values
import uk.gov.ons.br.vat.models.Turnover

class VatHBaseRowMapperSpec extends UnitSpec with MockFactory {

  private trait Fixture {
    def sampleVatRowWith(cells: Seq[HBaseCell]): HBaseRow =
      HBaseRow(key = "unused", cells)

    implicit val logger: Logger = stub[Logger]
    val underTest = VatHBaseRowMapper
  }

  "A Vat HBaseRowMapper" - {
    "can create a VAT admin unit" - {
      "when all columns are defined" in new Fixture {
        underTest.fromRow(sampleVatRowWith(AllCells)) shouldBe Some(SampleVatUnitWithAllFields)
      }

      "ignoring unrecognised columns" in new Fixture {
        underTest.fromRow(sampleVatRowWith(AllCells :+ UnusedCell)) shouldBe Some(SampleVatUnitWithAllFields)
      }

      /*
       * By 'minimal' we mean the mandatory columns + one of the 'name' columns.
       */
      "when only minimal columns are defined" in new Fixture {
        val minimalVatWithName = SampleVatUnitWithOnlyMandatoryFields.copy(name = Name1Cell.value)

        underTest.fromRow(sampleVatRowWith(MandatoryCells :+ Name1Cell)) shouldBe Some(minimalVatWithName)
      }

      "building the 'name' value from multiple columns" - {
        "using 'name1' when it is the only name column defined" in new Fixture {
          val minimalVatWithName1 = SampleVatUnitWithOnlyMandatoryFields.copy(name = Name1Cell.value)

          underTest.fromRow(sampleVatRowWith(MandatoryCells :+ Name1Cell)) shouldBe Some(minimalVatWithName1)
        }

        "using 'name2' when it is the only name column defined" in new Fixture {
          val minimalVatWithName2 = SampleVatUnitWithOnlyMandatoryFields.copy(name = Name2Cell.value)

          underTest.fromRow(sampleVatRowWith(MandatoryCells :+ Name2Cell)) shouldBe Some(minimalVatWithName2)
        }

        "using 'name3' when it is the only name column defined" in new Fixture {
          val minimalVatWithName3 = SampleVatUnitWithOnlyMandatoryFields.copy(name = Name3Cell.value)

          underTest.fromRow(sampleVatRowWith(MandatoryCells :+ Name3Cell)) shouldBe Some(minimalVatWithName3)
        }

        "appending 'name2' to 'name1' when they are both defined and 'name3' is not" in new Fixture {
          val expectedName = Name1Cell.value + Name2Cell.value
          val minimalVatWithName1and2 = SampleVatUnitWithOnlyMandatoryFields.copy(name = expectedName)

          underTest.fromRow(sampleVatRowWith(MandatoryCells ++ Seq(Name1Cell, Name2Cell))) shouldBe Some(minimalVatWithName1and2)
        }

        "appending 'name3' to 'name1' when they are both defined and 'name2' is not" in new Fixture {
          val expectedName = Name1Cell.value + Name3Cell.value
          val minimalVatWithName1and3 = SampleVatUnitWithOnlyMandatoryFields.copy(name = expectedName)

          underTest.fromRow(sampleVatRowWith(MandatoryCells ++ Seq(Name1Cell, Name3Cell))) shouldBe Some(minimalVatWithName1and3)
        }

        "appending 'name3' to 'name2' when they are both defined and 'name1' is not" in new Fixture {
          val expectedName = Name2Cell.value + Name3Cell.value
          val minimalVatWithName2and3 = SampleVatUnitWithOnlyMandatoryFields.copy(name = expectedName)

          underTest.fromRow(sampleVatRowWith(MandatoryCells ++ Seq(Name2Cell, Name3Cell))) shouldBe Some(minimalVatWithName2and3)
        }

        "concatenating 'name1' with 'name2' with 'name3' when they are all defined" in new Fixture {
          val expectedName = Name1Cell.value + Name2Cell.value + Name3Cell.value
          val minimalVatWithName = SampleVatUnitWithOnlyMandatoryFields.copy(name = expectedName)

          underTest.fromRow(sampleVatRowWith(MandatoryCells ++ Seq(Name1Cell, Name2Cell, Name3Cell))) shouldBe Some(minimalVatWithName)
        }
      }
      TurnoverCell
      "building the 'tradingStyle' value from multiple columns" - {
        "using 'tradstyle1' when it is the only trading style column defined" in new Fixture {
          val vatWithTradingStyle1 = SampleVatUnitWithAllFields.copy(tradingStyle = Some(TradingStyle1Cell.value))
          val tradingStyle2and3 = Set(TradingStyle2Cell, TradingStyle3Cell).map(_.column)
          val noTradingStyle2and3 = AllCells.filterNot(cell => tradingStyle2and3.contains(cell.column))

          underTest.fromRow(sampleVatRowWith(noTradingStyle2and3)) shouldBe Some(vatWithTradingStyle1)
        }

        "using 'tradstyle2' when it is the only trading style column defined" in new Fixture {
          val vatWithTradingStyle2 = SampleVatUnitWithAllFields.copy(tradingStyle = Some(TradingStyle2Cell.value))
          val tradingStyle1and3 = Set(TradingStyle1Cell, TradingStyle3Cell).map(_.column)
          val noTradingStyle1and3 = AllCells.filterNot(cell => tradingStyle1and3.contains(cell.column))

          underTest.fromRow(sampleVatRowWith(noTradingStyle1and3)) shouldBe Some(vatWithTradingStyle2)
        }

        "using 'tradstyle3' when it is the only trading style column defined" in new Fixture {
          val vatWithTradingStyle3 = SampleVatUnitWithAllFields.copy(tradingStyle = Some(TradingStyle3Cell.value))
          val tradingStyle1and2 = Set(TradingStyle1Cell, TradingStyle2Cell).map(_.column)
          val noTradingStyle1and2 = AllCells.filterNot(cell => tradingStyle1and2.contains(cell.column))

          underTest.fromRow(sampleVatRowWith(noTradingStyle1and2)) shouldBe Some(vatWithTradingStyle3)
        }

        "appending 'tradstyle2' to 'tradstyle1' when they are both defined and 'tradstyle3' is not" in new Fixture {
          val vatWithTradingStyle1and2 = SampleVatUnitWithAllFields.copy(tradingStyle =
            Some(TradingStyle1Cell.value + TradingStyle2Cell.value))
          val noTradingStyle3 = AllCells.filterNot(cell => cell.column == TradingStyle3Cell.column)

          underTest.fromRow(sampleVatRowWith(noTradingStyle3)) shouldBe Some(vatWithTradingStyle1and2)
        }

        "appending 'tradstyle3' to 'tradstyle1' when they are both defined and 'tradstyle2' is not" in new Fixture {
          val vatWithTradingStyle1and3 = SampleVatUnitWithAllFields.copy(tradingStyle =
            Some(TradingStyle1Cell.value + TradingStyle3Cell.value))
          val noTradingStyle2 = AllCells.filterNot(cell => cell.column == TradingStyle2Cell.column)

          underTest.fromRow(sampleVatRowWith(noTradingStyle2)) shouldBe Some(vatWithTradingStyle1and3)
        }

        "appending 'tradstyle3' to 'tradstyle2' when they are both defined and 'tradstyle1' is not" in new Fixture {
          val vatWithTradingStyle2and3 = SampleVatUnitWithAllFields.copy(tradingStyle =
            Some(TradingStyle2Cell.value + TradingStyle3Cell.value))
          val noTradingStyle1 = AllCells.filterNot(cell => cell.column == TradingStyle1Cell.column)

          underTest.fromRow(sampleVatRowWith(noTradingStyle1)) shouldBe Some(vatWithTradingStyle2and3)
        }

        // concatenation of all 3 tradingStyle columns is covered by the 'all columns' scenario
      }

      "when lifespan is only partially populated" in new Fixture {
        val cellsIncludingPartialLifespan = MandatoryCells ++ Seq(Name1Cell, BirthDateCell)
        val vatWithIncompleteLifespan = SampleVatUnitWithOnlyMandatoryFields.copy(name = Name1Cell.value,
          lifespan = Some(Lifespan(
            birthDate = BirthDateCell.value,
            deathDate = None,
            deathCode = None))
        )

        underTest.fromRow(sampleVatRowWith(cellsIncludingPartialLifespan)) shouldBe Some(vatWithIncompleteLifespan)
      }

      "when turnover is only partially populated" in new Fixture {
        val cellsIncludingPartialTurnover = MandatoryCells ++ Seq(Name1Cell, TurnoverCell)
        val vatWithIncompleteTurnover = SampleVatUnitWithOnlyMandatoryFields.copy(name = Name1Cell.value,
          turnover = Some(Turnover(
            amount = Some(TurnoverCell.value.toInt),
            date = None
          ))
        )

        underTest.fromRow(sampleVatRowWith(cellsIncludingPartialTurnover)) shouldBe Some(vatWithIncompleteTurnover)
      }

    }
  }

  "cannot create a VAT admin unit" - {
    "when any mandatory column is missing" in new Fixture {
      MandatoryColumns.foreach { columnName =>
        withClue(s"with missing column [$columnName]") {
          val withMissingColumn = sampleVatRowWith(AllCells.filterNot(_.column == columnName))

          underTest.fromRow(withMissingColumn) shouldBe None
        }
      }
    }

    "when none of the name columns are defined" in new Fixture {
      val nameColumns = Set(Name1Cell, Name2Cell, Name3Cell).map(_.column)
      val withMissingName = sampleVatRowWith(AllCells.filterNot(cell => nameColumns.contains(cell.column)))

      underTest.fromRow(withMissingName) shouldBe None
    }

    "when any numeric column" - {
      "contains a non-numeric value" in new Fixture {
        NumericColumns.foreach { columnName =>
          withClue(s"with a non-numeric value for column [$columnName]") {
            val badCell = HBaseCell(column = columnName, value = "not-a-number")
            val withBadCell = sampleVatRowWith(AllCells.filterNot(_.column == columnName) :+ badCell)

            underTest.fromRow(withBadCell) shouldBe None
          }
        }
      }

      "contains a non-integral value" in new Fixture {
        NumericColumns.foreach { columnName =>
          withClue(s"with a non-integral value for column [$columnName]") {
            val badCell = HBaseCell(column = columnName, value = "3.14159")
            val withBadCell = sampleVatRowWith(AllCells.filterNot(_.column == columnName) :+ badCell)

            underTest.fromRow(withBadCell) shouldBe None
          }
        }
      }
    }
  }
}

/*
 * Because we need to test that we can correctly concatenate name & tradingStyle fields from multiple HBase cells,
 * we explicitly define the individual cells here.  This implies that for consistency reasons, we cannot reference
 * the standard SampleVat without adjusting for this.
 */
private object VatHBaseRowMapperSpec {

  import uk.gov.ons.br.vat.test.SampleVat

  val VatRefCell = HBaseCell(Columns.vatref, Values.Vatref)
  val Name1Cell = HBaseCell(Columns.name1, value = "Big Box")
  val Name2Cell = HBaseCell(Columns.name2, value = " Cereal")
  val Name3Cell = HBaseCell(Columns.name3, value = " Limited")
  val TradingStyle1Cell = HBaseCell(Columns.tradingStyle1, value = "Big")
  val TradingStyle2Cell = HBaseCell(Columns.tradingStyle2, value = " Box")
  val TradingStyle3Cell = HBaseCell(Columns.tradingStyle3, value = " Cereal")
  val LegalStatusCell = HBaseCell(Columns.legalStatus, Values.LegalStatus)
  val SicCell = HBaseCell(Columns.sic, Values.Sic)
  val RecordTypeCell = HBaseCell(Columns.recordType, Values.RecordType)
  val BirthDateCell = HBaseCell(Columns.Lifespan.birthDate, Values.BirthDate)
  val DeathDateCell = HBaseCell(Columns.Lifespan.deathDate, Values.DeathDate)
  val DeathCodeCell = HBaseCell(Columns.Lifespan.deathCode, Values.DeathCode)
  val TurnoverCell = HBaseCell(Columns.Turnover.amount, Values.TurnoverAmount.toString)
  val TurnoverDateCell = HBaseCell(Columns.Turnover.date, Values.TurnoverDate)
  val AddressLine1Cell = HBaseCell(Columns.Address.line1, Values.AddressLine1)
  val AddressLine2Cell = HBaseCell(Columns.Address.line2, Values.AddressLine2)
  val AddressLine3Cell = HBaseCell(Columns.Address.line3, Values.AddressLine3)
  val AddressLine4Cell = HBaseCell(Columns.Address.line4, Values.AddressLine4)
  val AddressLine5Cell = HBaseCell(Columns.Address.line5, Values.AddressLine5)
  val PostcodeCell = HBaseCell(Columns.Address.postcode, Values.Postcode)
  val UbrnCell = HBaseCell(Columns.Links.ubrn, Values.Ubrn)
  val UnusedCell = HBaseCell(column = HBaseColumn.name(HBaseColumn(family = "cf", qualifier = "unused")), value = "unused")

  // we generate the Seq via a Set to guarantee that we do not have any cell ordering dependency
  val AllCells = Set(
    VatRefCell, Name1Cell, Name2Cell, Name3Cell, TradingStyle1Cell, TradingStyle2Cell, TradingStyle3Cell,
    LegalStatusCell, SicCell, RecordTypeCell, BirthDateCell,
    DeathDateCell, DeathCodeCell, TurnoverCell, TurnoverDateCell, AddressLine1Cell,
    AddressLine2Cell, AddressLine3Cell, AddressLine4Cell, AddressLine5Cell, PostcodeCell, UbrnCell
  ).toSeq

  // name is a mandatory field - but because it is derived from multiple columns it is not included here
  val MandatoryColumns = Set(Columns.vatref, Columns.legalStatus, Columns.recordType, Columns.sic, Columns.Address.line1, Columns.Address.postcode)

  // at least one of the name cells must be added to this in order to generate a valid Vat
  val MandatoryCells = AllCells.filter { cell => MandatoryColumns.contains(cell.column) }

  val NumericColumns = Set(Columns.Turnover.amount)

  val SampleVatUnitWithAllFields = SampleVat.SampleVatUnitWithAllFields.copy(
    name = "Big Box Cereal Limited", tradingStyle = Some("Big Box Cereal"))
}
