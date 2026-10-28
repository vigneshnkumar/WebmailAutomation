package com.selenium.gmail.utilities;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.selenium.gmail.config.Config;

@Test(groups = "unit")
public class ExcelDataReaderTest {

	private Path workbookFile;

	@BeforeClass(alwaysRun = true)
	public void createWorkbook() throws IOException {
		workbookFile = Files.createTempFile("excel-data-reader", ".xlsx");
		try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(workbookFile)) {
			Sheet sheet = wb.createSheet("Data");

			// Two-row table with headers on the marker row
			set(sheet, 0, 0, "Users");
			set(sheet, 0, 1, "name");
			set(sheet, 0, 2, "age");
			set(sheet, 1, 1, "  alice  ");
			sheet.getRow(1).createCell(2).setCellValue(30);
			set(sheet, 2, 1, "bob");
			sheet.getRow(2).createCell(2).setCellFormula("20+5");
			set(sheet, 3, 3, "Users");

			// Table with a completely missing row and a blank cell
			set(sheet, 10, 0, "Gaps");
			set(sheet, 11, 1, "x");
			// row 12 intentionally never created
			set(sheet, 13, 2, "y");
			set(sheet, 14, 3, "Gaps");

			// Header-only table: markers on adjacent rows
			set(sheet, 20, 0, "Empty");
			set(sheet, 21, 2, "Empty");

			// Malformed tables
			set(sheet, 30, 0, "Single");
			set(sheet, 40, 3, "Backwards");
			set(sheet, 41, 0, "Backwards");
			set(sheet, 50, 0, "Triple");
			set(sheet, 51, 2, "Triple");
			set(sheet, 52, 4, "Triple");
			set(sheet, 60, 0, "NoColumns");
			set(sheet, 62, 1, "NoColumns");

			wb.write(out);
		}
	}

	@AfterClass(alwaysRun = true)
	public void deleteWorkbook() throws IOException {
		Files.deleteIfExists(workbookFile);
	}

	@Test
	public void readsTableBetweenMarkers() {
		Object[][] data = ExcelDataReader.read(workbookFile, "Data", "Users");
		assertEquals(data, new Object[][] { { "alice", "30" }, { "bob", "25" } });
	}

	@Test
	public void missingRowsAndCellsBecomeEmptyStrings() {
		Object[][] data = ExcelDataReader.read(workbookFile, "Data", "Gaps");
		assertEquals(data, new Object[][] { { "x", "" }, { "", "" }, { "", "y" } });
	}

	@Test
	public void tableWithoutDataRowsIsEmpty() {
		assertEquals(ExcelDataReader.read(workbookFile, "Data", "Empty").length, 0);
	}

	@Test
	public void missingFileIsReportedClearly() {
		Path missing = workbookFile.resolveSibling("does-not-exist.xlsx");
		IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
				() -> ExcelDataReader.read(missing, "Data", "Users"));
		assertTrue(e.getMessage().contains("does-not-exist.xlsx"), e.getMessage());
	}

	@Test
	public void missingSheetListsAvailableSheets() {
		IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
				() -> ExcelDataReader.read(workbookFile, "Nope", "Users"));
		assertTrue(e.getMessage().contains("[Data]"), e.getMessage());
	}

	@Test
	public void unknownTableIsReported() {
		IllegalStateException e = expectThrows(IllegalStateException.class,
				() -> ExcelDataReader.read(workbookFile, "Data", "Unknown"));
		assertTrue(e.getMessage().contains("found 0"), e.getMessage());
	}

	@Test
	public void tableWithOneMarkerIsRejected() {
		assertThrows(IllegalStateException.class, () -> ExcelDataReader.read(workbookFile, "Data", "Single"));
	}

	@Test
	public void tableWithThreeMarkersIsRejected() {
		IllegalStateException e = expectThrows(IllegalStateException.class,
				() -> ExcelDataReader.read(workbookFile, "Data", "Triple"));
		assertTrue(e.getMessage().contains("found 3"), e.getMessage());
	}

	@Test
	public void endMarkerMustBeBelowAndRightOfStart() {
		IllegalStateException e = expectThrows(IllegalStateException.class,
				() -> ExcelDataReader.read(workbookFile, "Data", "Backwards"));
		assertTrue(e.getMessage().contains("below and to the right"), e.getMessage());
	}

	@Test
	public void tableWithoutDataColumnsIsRejected() {
		IllegalStateException e = expectThrows(IllegalStateException.class,
				() -> ExcelDataReader.read(workbookFile, "Data", "NoColumns"));
		assertTrue(e.getMessage().contains("no data columns"), e.getMessage());
	}

	/** Guards the real test-data workbook that the UI tests depend on. */
	@Test
	public void projectWorkbookTablesAreWellFormed() {
		Path excel = Config.get().getPath("excel_path");

		Object[][] attach = ExcelDataReader.read(excel, "SendMailTests", "Attach");
		assertTrue(attach.length > 0, "Attach table should have rows");
		for (Object[] row : attach) {
			assertEquals(row.length, 3, "sendTo, subject, filePath");
			assertTrue(Files.isRegularFile(Config.resolvePath((String) row[2])), "Attachment must exist: " + row[2]);
		}

		Object[][] search = ExcelDataReader.read(excel, "SearchTests", "Search");
		assertTrue(search.length > 0, "Search table should have rows");
		for (Object[] row : search) {
			assertEquals(row.length, 1, "searchBarText");
			assertTrue(!((String) row[0]).isEmpty(), "Search text must not be blank");
		}
	}

	private static void set(Sheet sheet, int row, int col, String value) {
		Row r = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
		r.createCell(col).setCellValue(value);
	}
}
