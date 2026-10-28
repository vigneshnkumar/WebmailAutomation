package com.selenium.gmail.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Reads data-driven test tables from an Excel sheet.
 *
 * <p>A table is marked by writing its name in two cells: the top-left corner and the bottom-right
 * corner. The rows and columns strictly between the two markers are the test data, so the marker
 * row itself can hold column headers:
 *
 * <pre>
 *   | Attach | sendTo        | subject | filePath  |        |
 *   |        | a@example.com | Hello   | file.txt  |        |
 *   |        |               |         |           | Attach |
 * </pre>
 *
 * Every cell is returned as the text Excel would display (numbers, dates and formulas included);
 * blank or missing cells become empty strings.
 */
public class ExcelDataReader implements AutoCloseable {

	private final Workbook workbook;
	private final Sheet sheet;
	private final DataFormatter formatter = new DataFormatter();
	private final FormulaEvaluator evaluator;

	/*
	 * Open the Excel file and select a sheet
	 * @params - Excel Path and Sheet Name
	 */
	public ExcelDataReader(Path path, String sheetName) {
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("Excel file not found: " + path.toAbsolutePath());
		}
		try (InputStream in = Files.newInputStream(path)) {
			workbook = WorkbookFactory.create(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not open Excel file " + path.toAbsolutePath(), e);
		}
		sheet = workbook.getSheet(sheetName);
		if (sheet == null) {
			List<String> names = new ArrayList<>();
			workbook.forEach(s -> names.add(s.getSheetName()));
			closeQuietly();
			throw new IllegalArgumentException(
					"Sheet '" + sheetName + "' not found in " + path.getFileName() + ". Available sheets: " + names);
		}
		evaluator = workbook.getCreationHelper().createFormulaEvaluator();
	}

	/** Convenience method: open the file, read one table and close the file. */
	public static Object[][] read(Path path, String sheetName, String tableName) {
		try (ExcelDataReader reader = new ExcelDataReader(path, sheetName)) {
			return reader.getTestData(tableName);
		}
	}

	/**
	 * Returns the data inside the table, one array per row. The return type matches what a TestNG
	 * {@code @DataProvider} expects. A table with no data rows yields an empty array.
	 */
	public String[][] getTestData(String tableName) {
		Cell[] bounds = findCells(tableName);
		int startRow = bounds[0].getRowIndex() + 1;
		int endRow = bounds[1].getRowIndex() - 1;
		int startCol = bounds[0].getColumnIndex() + 1;
		int endCol = bounds[1].getColumnIndex() - 1;

		if (endCol < startCol) {
			throw new IllegalStateException("Table '" + tableName + "' in sheet '" + sheet.getSheetName()
					+ "' has no data columns: the end marker must be at least two columns right of the start marker");
		}
		if (endRow < startRow) {
			return new String[0][];
		}

		String[][] testData = new String[endRow - startRow + 1][endCol - startCol + 1];
		for (int i = startRow; i <= endRow; i++) {
			Row row = sheet.getRow(i);
			for (int j = startCol; j <= endCol; j++) {
				Cell cell = row == null ? null : row.getCell(j);
				// Handle numbers, dates, formulas and strings; null cells become ""
				testData[i - startRow][j - startCol] = formatter.formatCellValue(cell, evaluator).trim();
			}
		}
		return testData;
	}

	/** Returns the start (top-left) and end (bottom-right) marker cells of a table. */
	public Cell[] findCells(String tableName) {
		List<Cell> markers = new ArrayList<>();
		for (Row row : sheet) {
			for (Cell cell : row) {
				if (tableName.equals(formatter.formatCellValue(cell).trim())) {
					markers.add(cell);
				}
			}
		}

		if (markers.size() != 2) {
			throw new IllegalStateException("Table '" + tableName + "' in sheet '" + sheet.getSheetName()
					+ "' must be marked by exactly 2 cells (top-left and bottom-right) but found " + markers.size()
					+ describe(markers));
		}
		Cell start = markers.get(0);
		Cell end = markers.get(1);
		if (end.getRowIndex() <= start.getRowIndex() || end.getColumnIndex() <= start.getColumnIndex()) {
			throw new IllegalStateException("Table '" + tableName + "' in sheet '" + sheet.getSheetName()
					+ "': end marker " + end.getAddress() + " must be below and to the right of start marker "
					+ start.getAddress());
		}
		return new Cell[] { start, end };
	}

	@Override
	public void close() {
		try {
			workbook.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void closeQuietly() {
		try {
			workbook.close();
		} catch (IOException ignored) {
			// already failing; the original error is more useful
		}
	}

	private static String describe(List<Cell> markers) {
		if (markers.isEmpty()) {
			return "";
		}
		List<String> addresses = new ArrayList<>();
		markers.forEach(c -> addresses.add(c.getAddress().formatAsString()));
		return " at " + addresses;
	}
}
