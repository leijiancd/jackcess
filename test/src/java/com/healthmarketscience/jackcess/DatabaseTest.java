/*
Copyright (c) 2007 Health Market Science, Inc.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA

You can contact Health Market Science at info@healthmarketscience.com
or at the following address:

Health Market Science
2700 Horizon Drive
Suite 200
King of Prussia, PA 19406
*/

package com.healthmarketscience.jackcess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Tim McCune
 */
public class DatabaseTest extends TestCase {
  
  public DatabaseTest(String name) throws Exception {
    super(name);
  }
   
  static Database open() throws Exception {
    return Database.open(new File("test/data/test.mdb"));
  }
  
  static Database create() throws Exception {
    return create(false);
  }

  static Database create(boolean keep) throws Exception {
    return Database.create(createTempFile(keep));
  }

  static Database openCopy(File srcFile) throws Exception {
    return openCopy(srcFile, false);
  }
  
  static Database openCopy(File srcFile, boolean keep) throws Exception {
    File tmp = createTempFile(keep);
    copyFile(srcFile, tmp);
    return Database.open(tmp);
  }
  
  public void testInvalidTableDefs() throws Exception {
    Database db = create();

    try {
      db.createTable("test", Collections.<Column>emptyList());
      fail("created table with no columns?");
    } catch(IllegalArgumentException e) {
      // success
    }
    
    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("A");
    col.setType(DataType.TEXT);
    columns.add(col);
    col = new Column();
    col.setName("a");
    col.setType(DataType.MEMO);
    columns.add(col);

    try {
      db.createTable("test", columns);
      fail("created table with duplicate column names?");
    } catch(IllegalArgumentException e) {
      // success
    }

    columns = new ArrayList<Column>();
    col = new Column();
    col.setName("A");
    col.setType(DataType.TEXT);
    col.setLength((short)(352 * DataType.TEXT.getUnitSize()));
    columns.add(col);
    
    try {
      db.createTable("test", columns);
      fail("created table with invalid column length?");
    } catch(IllegalArgumentException e) {
      // success
    }

    columns = new ArrayList<Column>();
    col = new Column();
    col.setName("A");
    col.setType(DataType.TEXT);
    columns.add(col);
    db.createTable("test", columns);
    
    try {
      db.createTable("Test", columns);
      fail("create duplicate tables?");
    } catch(IllegalArgumentException e) {
      // success
    }

  }
      
  public void testReadDeletedRows() throws Exception {
    Table table = Database.open(new File("test/data/delTest.mdb")).getTable("Table");
    int rows = 0;
    while (table.getNextRow() != null) {
      rows++;
    }
    assertEquals(2, rows); 
  }
  
  public void testGetColumns() throws Exception {
    List columns = open().getTable("Table1").getColumns();
    assertEquals(9, columns.size());
    checkColumn(columns, 0, "A", DataType.TEXT);
    checkColumn(columns, 1, "B", DataType.TEXT);
    checkColumn(columns, 2, "C", DataType.BYTE);
    checkColumn(columns, 3, "D", DataType.INT);
    checkColumn(columns, 4, "E", DataType.LONG);
    checkColumn(columns, 5, "F", DataType.DOUBLE);
    checkColumn(columns, 6, "G", DataType.SHORT_DATE_TIME);
    checkColumn(columns, 7, "H", DataType.MONEY);
    checkColumn(columns, 8, "I", DataType.BOOLEAN);
  }
  
  static void checkColumn(List columns, int columnNumber, String name,
      DataType dataType)
    throws Exception
  {
    Column column = (Column) columns.get(columnNumber);
    assertEquals(name, column.getName());
    assertEquals(dataType, column.getType());
  }
  
  public void testGetNextRow() throws Exception {
    Database db = open();
    assertEquals(2, db.getTableNames().size());
    Table table = db.getTable("Table1");
    
    Map<String, Object> row = table.getNextRow();
    assertEquals("abcdefg", row.get("A"));
    assertEquals("hijklmnop", row.get("B"));
    assertEquals(new Byte((byte) 2), row.get("C"));
    assertEquals(new Short((short) 222), row.get("D"));
    assertEquals(new Integer(333333333), row.get("E"));
    assertEquals(new Double(444.555d), row.get("F"));
    Calendar cal = Calendar.getInstance();
    cal.setTime((Date) row.get("G"));
    assertEquals(Calendar.SEPTEMBER, cal.get(Calendar.MONTH));
    assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(1974, cal.get(Calendar.YEAR));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
    assertEquals(Boolean.TRUE, row.get("I"));
    
    row = table.getNextRow();
    assertEquals("a", row.get("A"));
    assertEquals("b", row.get("B"));
    assertEquals(new Byte((byte) 0), row.get("C"));
    assertEquals(new Short((short) 0), row.get("D"));
    assertEquals(new Integer(0), row.get("E"));
    assertEquals(new Double(0d), row.get("F"));
    cal = Calendar.getInstance();
    cal.setTime((Date) row.get("G"));
    assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
    assertEquals(12, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(1981, cal.get(Calendar.YEAR));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
    assertEquals(Boolean.FALSE, row.get("I"));
  }
  
  public void testCreate() throws Exception {
    Database db = create();
    assertEquals(0, db.getTableNames().size());
  }
  
  public void testWriteAndRead() throws Exception {
    Database db = create();
    createTestTable(db);
    Object[] row = createTestRow();
    row[3] = null;
    Table table = db.getTable("Test");
    int count = 1000;
    for (int i = 0; i < count; i++) { 
      table.addRow(row);
    }
    for (int i = 0; i < count; i++) {
      Map<String, Object> readRow = table.getNextRow();
      assertEquals(row[0], readRow.get("A"));
      assertEquals(row[1], readRow.get("B"));
      assertEquals(row[2], readRow.get("C"));
      assertEquals(row[3], readRow.get("D"));
      assertEquals(row[4], readRow.get("E"));
      assertEquals(row[5], readRow.get("F"));
      assertEquals(row[6], readRow.get("G"));
      assertEquals(row[7], readRow.get("H"));
    }
  }
  
  public void testWriteAndReadInBatch() throws Exception {
    Database db = create();
    createTestTable(db);
    int count = 1000;
    List<Object[]> rows = new ArrayList<Object[]>(count);
    Object[] row = createTestRow();
    for (int i = 0; i < count; i++) {
      rows.add(row);
    }
    Table table = db.getTable("Test");
    table.addRows(rows);
    for (int i = 0; i < count; i++) {
      Map<String, Object> readRow = table.getNextRow();
      assertEquals(row[0], readRow.get("A"));
      assertEquals(row[1], readRow.get("B"));
      assertEquals(row[2], readRow.get("C"));
      assertEquals(row[3], readRow.get("D"));
      assertEquals(row[4], readRow.get("E"));
      assertEquals(row[5], readRow.get("F"));
      assertEquals(row[6], readRow.get("G"));
      assertEquals(row[7], readRow.get("H"));
    }
  }

  public void testDeleteCurrentRow() throws Exception {

    // make sure correct row is deleted
    Database db = create();
    createTestTable(db);
    Object[] row1 = createTestRow("Tim1");
    Object[] row2 = createTestRow("Tim2");
    Object[] row3 = createTestRow("Tim3");
    Table table = db.getTable("Test");
    table.addRows(Arrays.asList(row1, row2, row3));
    assertRowCount(3, table);
    
    table.reset();
    table.getNextRow();
    table.getNextRow();
    table.deleteCurrentRow();

    table.reset();

    Map<String, Object> outRow = table.getNextRow();
    assertEquals("Tim1", outRow.get("A"));
    outRow = table.getNextRow();
    assertEquals("Tim3", outRow.get("A"));
    assertRowCount(2, table);

    // test multi row delete/add
    db = create();
    createTestTable(db);
    Object[] row = createTestRow();
    table = db.getTable("Test");
    for (int i = 0; i < 10; i++) {
      row[3] = i;
      table.addRow(row);
    }
    row[3] = 1974;
    assertRowCount(10, table);
    table.reset();
    table.getNextRow();
    table.deleteCurrentRow();
    assertRowCount(9, table);
    table.reset();
    table.getNextRow();
    table.deleteCurrentRow();
    assertRowCount(8, table);
    table.reset();
    for (int i = 0; i < 8; i++) {
      table.getNextRow();
    }
    table.deleteCurrentRow();
    assertRowCount(7, table);
    table.addRow(row);
    assertRowCount(8, table);
    table.reset();
    for (int i = 0; i < 3; i++) {
      table.getNextRow();
    }
    table.deleteCurrentRow();
    assertRowCount(7, table);
    table.reset();
    assertEquals(2, table.getNextRow().get("D"));
  }

  public void testReadLongValue() throws Exception {

    Database db = Database.open(new File("test/data/test2.mdb"));
    Table table = db.getTable("MSP_PROJECTS");
    Map<String, Object> row = table.getNextRow();
    assertEquals("Jon Iles this is a a vawesrasoih aksdkl fas dlkjflkasjd flkjaslkdjflkajlksj dfl lkasjdf lkjaskldfj lkas dlk lkjsjdfkl; aslkdf lkasjkldjf lka skldf lka sdkjfl;kasjd falksjdfljaslkdjf laskjdfk jalskjd flkj aslkdjflkjkjasljdflkjas jf;lkasjd fjkas dasdf asd fasdf asdf asdmhf lksaiyudfoi jasodfj902384jsdf9 aw90se fisajldkfj lkasj dlkfslkd jflksjadf as", row.get("PROJ_PROP_AUTHOR"));
    assertEquals("T", row.get("PROJ_PROP_COMPANY"));
    assertEquals("Standard", row.get("PROJ_INFO_CAL_NAME"));
    assertEquals("Project1", row.get("PROJ_PROP_TITLE"));
    byte[] foundBinaryData = (byte[])row.get("RESERVED_BINARY_DATA");
    byte[] expectedBinaryData =
      toByteArray(new File("test/data/test2BinData.dat"));
    assertTrue(Arrays.equals(expectedBinaryData, foundBinaryData));
  }

  public void testWriteLongValue() throws Exception {

    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("A");
    col.setType(DataType.TEXT);
    columns.add(col);
    col = new Column();
    col.setName("B");
    col.setType(DataType.MEMO);
    columns.add(col);
    col = new Column();
    col.setName("C");
    col.setType(DataType.OLE);
    columns.add(col);
    db.createTable("test", columns);

    String testStr = "This is a test";
    StringBuilder strBuf = new StringBuilder();
    for(int i = 0; i < 2030; ++i) {
      char c = (char)('a' + (i % 26));
      strBuf.append(c);
    }
    String longMemo = strBuf.toString();
    byte[] oleValue = toByteArray(new File("test/data/test2BinData.dat"));
    
    
    Table table = db.getTable("Test");
    table.addRow(testStr, testStr, null);
    table.addRow(testStr, longMemo, oleValue);

    table.reset();

    Map<String, Object> row = table.getNextRow();

    assertEquals(testStr, row.get("A"));
    assertEquals(testStr, row.get("B"));
    assertNull(row.get("C"));

    row = table.getNextRow();
    
    assertEquals(testStr, row.get("A"));
    assertEquals(longMemo, row.get("B"));
    assertTrue(Arrays.equals(oleValue, (byte[])row.get("C")));
    
  }

  public void testMissingFile() throws Exception {
    File bogusFile = new File("fooby-dooby.mdb");
    assertTrue(!bogusFile.exists());
    try {
      Database db = Database.open(bogusFile);
      fail("FileNotFoundException should have been thrown");
    } catch(FileNotFoundException e) {
    }
    assertTrue(!bogusFile.exists());
  }

  public void testReadWithDeletedCols() throws Exception {
    Table table = Database.open(new File("test/data/delColTest.mdb")).getTable("Table1");

    Map<String, Object> expectedRow0 = new LinkedHashMap<String, Object>();
    expectedRow0.put("id", 0);
    expectedRow0.put("id2", 2);
    expectedRow0.put("data", "foo");
    expectedRow0.put("data2", "foo2");

    Map<String, Object> expectedRow1 = new LinkedHashMap<String, Object>();
    expectedRow1.put("id", 3);
    expectedRow1.put("id2", 5);
    expectedRow1.put("data", "bar");
    expectedRow1.put("data2", "bar2");

    int rowNum = 0;
    Map<String, Object> row = null;
    while ((row = table.getNextRow()) != null) {
      if(rowNum == 0) {
        assertEquals(expectedRow0, row);
      } else if(rowNum == 1) {
        assertEquals(expectedRow1, row);
      } else if(rowNum >= 2) {
        fail("should only have 2 rows");
      }
      rowNum++;
    }
  }

  public void testCurrency() throws Exception {
    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("A");
    col.setType(DataType.MONEY);
    columns.add(col);
    db.createTable("test", columns);

    Table table = db.getTable("Test");
    table.addRow(new BigDecimal("-2341234.03450"));
    table.addRow(37L);
    table.addRow("10000.45");

    table.reset();

    List<Object> foundValues = new ArrayList<Object>();
    Map<String, Object> row = null;
    while((row = table.getNextRow()) != null) {
      foundValues.add(row.get("A"));
    }

    assertEquals(Arrays.asList(
                     new BigDecimal("-2341234.0345"),
                     new BigDecimal("37.0000"),
                     new BigDecimal("10000.4500")),
                 foundValues);

    try {
      table.addRow(new BigDecimal("342523234145343543.3453"));
      fail("IOException should have been thrown");
    } catch(IOException e) {
      // ignored
    }
  }

  public void testGUID() throws Exception
  {
    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("A");
    col.setType(DataType.GUID);
    columns.add(col);
    db.createTable("test", columns);

    Table table = db.getTable("Test");
    table.addRow("{32A59F01-AA34-3E29-453F-4523453CD2E6}");
    table.addRow("{32a59f01-aa34-3e29-453f-4523453cd2e6}");
    table.addRow("{11111111-1111-1111-1111-111111111111}");
    table.addRow("{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF}");

    table.reset();

    List<Object> foundValues = new ArrayList<Object>();
    Map<String, Object> row = null;
    while((row = table.getNextRow()) != null) {
      foundValues.add(row.get("A"));
    }

    assertEquals(Arrays.asList(
                     "{32A59F01-AA34-3E29-453F-4523453CD2E6}",
                     "{32A59F01-AA34-3E29-453F-4523453CD2E6}",
                     "{11111111-1111-1111-1111-111111111111}",
                     "{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF}"),
                 foundValues);

    try {
      table.addRow("3245234");
      fail("IOException should have been thrown");
    } catch(IOException e) {
      // ignored
    }
  }

  public void testNumeric() throws Exception
  {
    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("A");
    col.setType(DataType.NUMERIC);
    col.setScale((byte)4);
    col.setPrecision((byte)8);
    columns.add(col);
    assertTrue(col.isVariableLength());
    
    col = new Column();
    col.setName("B");
    col.setType(DataType.NUMERIC);
    col.setScale((byte)8);
    col.setPrecision((byte)28);
    columns.add(col);
    db.createTable("test", columns);

    Table table = db.getTable("Test");
    table.addRow(new BigDecimal("-1234.03450"),
                 new BigDecimal("23923434453436.36234219"));
    table.addRow(37L, 37L);
    table.addRow("1000.45", "-3452345321000");

    table.reset();

    List<Object> foundSmallValues = new ArrayList<Object>();
    List<Object> foundBigValues = new ArrayList<Object>();
    Map<String, Object> row = null;
    while((row = table.getNextRow()) != null) {
      foundSmallValues.add(row.get("A"));
      foundBigValues.add(row.get("B"));
    }

    assertEquals(Arrays.asList(
                     new BigDecimal("-1234.0345"),
                     new BigDecimal("37.0000"),
                     new BigDecimal("1000.4500")),
                 foundSmallValues);
    assertEquals(Arrays.asList(
                     new BigDecimal("23923434453436.36234219"),
                     new BigDecimal("37.00000000"),
                     new BigDecimal("-3452345321000.00000000")),
                 foundBigValues);

    try {
      table.addRow(new BigDecimal("3245234.234"),
                   new BigDecimal("3245234.234"));
      fail("IOException should have been thrown");
    } catch(IOException e) {
      // ignored
    }
  }

  public void testFixedNumeric() throws Exception
  {
    Database db = openCopy(new File("test/data/fixedNumericTest.mdb"));
    Table t = db.getTable("test");

    boolean first = true;
    for(Column col : t.getColumns()) {
      if(first) {
        assertTrue(col.isVariableLength());
        assertEquals(DataType.MEMO, col.getType());
        first = false;
      } else {
        assertFalse(col.isVariableLength());
        assertEquals(DataType.NUMERIC, col.getType());
      }
    }
      
    Map<String, Object> row = t.getNextRow();
    assertEquals("some data", row.get("col1"));
    assertEquals(new BigDecimal("1"), row.get("col2"));
    assertEquals(new BigDecimal("0"), row.get("col3"));
    assertEquals(new BigDecimal("0"), row.get("col4"));
    assertEquals(new BigDecimal("4"), row.get("col5"));
    assertEquals(new BigDecimal("-1"), row.get("col6"));
    assertEquals(new BigDecimal("1"), row.get("col7"));

    Object[] tmpRow = new Object[]{
      "foo", new BigDecimal("1"), new BigDecimal(3), new BigDecimal("13"),
      new BigDecimal("-17"), new BigDecimal("0"), new BigDecimal("8734")};
    t.addRow(tmpRow);
    t.reset();

    t.getNextRow();
    row = t.getNextRow();
    assertEquals(tmpRow[0], row.get("col1"));
    assertEquals(tmpRow[1], row.get("col2"));
    assertEquals(tmpRow[2], row.get("col3"));
    assertEquals(tmpRow[3], row.get("col4"));
    assertEquals(tmpRow[4], row.get("col5"));
    assertEquals(tmpRow[5], row.get("col6"));
    assertEquals(tmpRow[6], row.get("col7"));
    
    db.close();

  }  

  public void testMultiPageTableDef() throws Exception
  {
    List<Column> columns = open().getTable("Table2").getColumns();
    assertEquals(89, columns.size());
  }

  public void testOverflow() throws Exception
  {
    Database mdb = Database.open(new File("test/data/overflowTest.mdb"));
    Table table = mdb.getTable("Table1");

    // 7 rows, 3 and 5 are overflow
    table.getNextRow();
    table.getNextRow();

    Map<String, Object> row = table.getNextRow();
    assertEquals(Arrays.<Object>asList(
                     null, "row3col3", null, null, null, null, null,
                     "row3col9", null),
                 new ArrayList<Object>(row.values()));

    table.getNextRow();

    row = table.getNextRow();
    assertEquals(Arrays.<Object>asList(
                     null, "row5col2", null, null, null, null, null, null,
                     null),
                 new ArrayList<Object>(row.values()));

    table.reset();
    assertRowCount(7, table);
    
  }

  public void testLongValueAsMiddleColumn() throws Exception
  {

    Database db = create();
    Column a = new Column();
    a.setName("a");
    a.setSQLType(Types.INTEGER);
    Column b = new Column();
    b.setName("b");
    b.setSQLType(Types.LONGVARCHAR);
    Column c = new Column();
    c.setName("c");
    c.setSQLType(Types.VARCHAR);
    db.createTable("NewTable", Arrays.asList(a, b, c));
    Table newTable = db.getTable("NewTable");
    
    String lval = createString(2000); // "--2000 chars long text--";
    String tval = createString(40); // "--40chars long text--";
    newTable.addRow(new Integer(1), lval, tval);

    newTable = db.getTable("NewTable");
    Map<String, Object> readRow = newTable.getNextRow();
    assertEquals(new Integer(1), readRow.get("a"));
    assertEquals(lval, readRow.get("b"));
    assertEquals(tval, readRow.get("c"));

  }


  public void testUsageMapPromotion() throws Exception {
    Database db = openCopy(new File("test/data/testPromotion.mdb"));
    Table t = db.getTable("jobDB1");

    String lval = createString(255); // "--255 chars long text--";

    for(int i = 0; i < 1000; ++i) {
      t.addRow(i, 13, 57, 47.0d, lval, lval, lval, lval, lval, lval);
    }

    Set<Integer> ids = new HashSet<Integer>();
    for(Map<String,Object> row : t) {
      ids.add((Integer)row.get("ID"));
    }
    assertEquals(1000, ids.size());

    db.close();
  }  


  public void testLargeTableDef() throws Exception {
    final int numColumns = 90;
    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    List<String> colNames = new ArrayList<String>();
    for(int i = 0; i < numColumns; ++i) {
      String colName = "MyColumnName" + i;
      colNames.add(colName);
      Column col = new Column();
      col.setName(colName);
      col.setType(DataType.TEXT);
      columns.add(col);
    }

    db.createTable("test", columns);

    Table t = db.getTable("test");

    List<String> row = new ArrayList<String>();
    Map<String,Object> expectedRowData = new LinkedHashMap<String, Object>();
    for(int i = 0; i < numColumns; ++i) {
      String value = "" + i + " some row data";
      row.add(value);
      expectedRowData.put(colNames.get(i), value);
    }

    t.addRow(row.toArray());

    t.reset();
    assertEquals(expectedRowData, t.getNextRow());
    
    db.close();
  }

  public void testAutoNumber() throws Exception {
    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("a");
    col.setType(DataType.LONG);
    col.setAutoNumber(true);
    columns.add(col);
    col = new Column();
    col.setName("b");
    col.setType(DataType.TEXT);
    columns.add(col);

    db.createTable("test", columns);

    Table table = db.getTable("test");
    table.addRow(null, "row1");
    table.addRow(13, "row2");
    table.addRow("flubber", "row3");

    table.reset();

    table.addRow(Column.AUTO_NUMBER, "row4");
    table.addRow(Column.AUTO_NUMBER, "row5");

    table.reset();

    List<Map<String, Object>> expectedRows =
      createExpectedTable(
          createExpectedRow(
              "a", 1,
              "b", "row1"),
          createExpectedRow(
              "a", 2,
              "b", "row2"),
          createExpectedRow(
              "a", 3,
              "b", "row3"),
          createExpectedRow(
              "a", 4,
              "b", "row4"),
          createExpectedRow(
              "a", 5,
              "b", "row5"));

    assertTable(expectedRows, table);    
    
    db.close();
  }  

  public void testWriteAndReadDate() throws Exception {
    Database db = create();

    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("name");
    col.setType(DataType.TEXT);
    columns.add(col);
    col = new Column();
    col.setName("date");
    col.setType(DataType.SHORT_DATE_TIME);
    columns.add(col);

    db.createTable("test", columns);
    Table table = db.getTable("test");
    
    DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    List<Date> dates =
      new ArrayList<Date>(
          Arrays.asList(
              df.parse("19801231 00:00:00"),
              df.parse("19930513 14:43:27"),
              null,
              df.parse("20210102 02:37:00"),
              new Date()));

    Calendar c = Calendar.getInstance();
    for(int year = 1801; year < 2050; year +=3) {
      for(int month = 0; month <= 12; ++month) {
        for(int day = 1; day < 29; day += 3) {
          c.clear();
          c.set(Calendar.YEAR, year);
          c.set(Calendar.MONTH, month);
          c.set(Calendar.DAY_OF_MONTH, day);
          dates.add(c.getTime());
        }
      }
    }

    for(Date d : dates) {
      table.addRow("row " + d, d);
    }

    List<Date> foundDates = new ArrayList<Date>();
    for(Map<String,Object> row : table) {
      foundDates.add((Date)row.get("date"));
    }

    assertEquals(dates.size(), foundDates.size());
    for(int i = 0; i < dates.size(); ++i) {
      try {
        assertEquals(dates.get(i), foundDates.get(i));
      } catch(Error e) {
        System.err.println("Expected " + dates.get(i).getTime() + ", found " +
                           foundDates.get(i).getTime());
        throw e;
      }
    }
  }
    
  static Object[] createTestRow(String col1Val) {
    return new Object[] {col1Val, "R", "McCune", 1234, (byte) 0xad, 555.66d,
        777.88f, (short) 999, new Date()};
  }

  static Object[] createTestRow() {
    return createTestRow("Tim");
  }
  
  static void createTestTable(Database db) throws Exception {
    List<Column> columns = new ArrayList<Column>();
    Column col = new Column();
    col.setName("A");
    col.setType(DataType.TEXT);
    columns.add(col);
    col = new Column();
    col.setName("B");
    col.setType(DataType.TEXT);
    columns.add(col);
    col = new Column();
    col.setName("C");
    col.setType(DataType.TEXT);
    columns.add(col);
    col = new Column();
    col.setName("D");
    col.setType(DataType.LONG);
    columns.add(col);
    col = new Column();
    col.setName("E");
    col.setType(DataType.BYTE);
    columns.add(col);
    col = new Column();
    col.setName("F");
    col.setType(DataType.DOUBLE);
    columns.add(col);
    col = new Column();
    col.setName("G");
    col.setType(DataType.FLOAT);
    columns.add(col);
    col = new Column();
    col.setName("H");
    col.setType(DataType.INT);
    columns.add(col);
    col = new Column();
    col.setName("I");
    col.setType(DataType.SHORT_DATE_TIME);
    columns.add(col);
    db.createTable("test", columns);
  }
    
  static String createString(int len) {
    StringBuilder builder = new StringBuilder(len);
    for(int i = 0; i < len; ++i) {
      builder.append((char)('a' + (i % 26)));
    }
    String str = builder.toString();
    return str;
  }

  static void assertRowCount(int expectedRowCount, Table table)
    throws Exception
  {
    assertEquals(expectedRowCount, countRows(table));
    assertEquals(expectedRowCount, table.getRowCount());
  }
  
  static int countRows(Table table) throws Exception {
    int rtn = 0;
    for(Map<String, Object> row : Cursor.createCursor(table)) {
      rtn++;
    }
    return rtn;
  }

  static void assertTable(List<Map<String, Object>> expectedTable, Table table)
  {
    List<Map<String, Object>> foundTable =
      new ArrayList<Map<String, Object>>();
    for(Map<String, Object> row : Cursor.createCursor(table)) {
      foundTable.add(row);
    }
    assertEquals(expectedTable, foundTable);
  }
  
  static Map<String, Object> createExpectedRow(Object... rowElements) {
    Map<String, Object> row = new LinkedHashMap<String, Object>();
    for(int i = 0; i < rowElements.length; i += 2) {
      row.put((String)rowElements[i],
              rowElements[i + 1]);
    }
    return row;
  }    

  @SuppressWarnings("unchecked")
  static List<Map<String, Object>> createExpectedTable(Map... rows) {
    return Arrays.<Map<String, Object>>asList(rows);
  }    
  
  static void dumpDatabase(Database mdb) throws Exception {
    dumpDatabase(mdb, new PrintWriter(System.out, true));
  }

  static void dumpTable(Table table) throws Exception {
    dumpTable(table, new PrintWriter(System.out, true));
  }

  static void dumpDatabase(Database mdb, PrintWriter writer) throws Exception {
    writer.println("DATABASE:");
    for(Table table : mdb) {
      dumpTable(table, writer);
    }
  }

  static void dumpTable(Table table, PrintWriter writer) throws Exception {
    writer.println("TABLE: " + table.getName());
    List<String> colNames = new ArrayList<String>();
    for(Column col : table.getColumns()) {
      colNames.add(col.getName());
    }
    writer.println("COLUMNS: " + colNames);
    for(Map<String, Object> row : Cursor.createCursor(table)) {

      // make byte[] printable
      for(Map.Entry<String, Object> entry : row.entrySet()) {
        Object v = entry.getValue();
        if(v instanceof byte[]) {
          byte[] bv = (byte[])v;
          entry.setValue(ByteUtil.toHexString(ByteBuffer.wrap(bv), bv.length));
        }
      }
      
      writer.println(row);
    }
  }

  static void copyFile(File srcFile, File dstFile)
    throws IOException
  {
    // FIXME should really be using commons io FileUtils here, but don't want
    // to add dep for one simple test method
    byte[] buf = new byte[1024];
    OutputStream ostream = new FileOutputStream(dstFile);
    InputStream istream = new FileInputStream(srcFile);
    try {
      int numBytes = 0;
      while((numBytes = istream.read(buf)) >= 0) {
        ostream.write(buf, 0, numBytes);
      }
    } finally {
      ostream.close();
    }
  }

  static File createTempFile(boolean keep) throws Exception {
    File tmp = File.createTempFile("databaseTest", ".mdb");
    if(keep) {
      System.out.println("Created " + tmp);
    } else {
      tmp.deleteOnExit();
    }
    return tmp;
  }

  static byte[] toByteArray(File file)
    throws IOException
  {
    // FIXME should really be using commons io IOUtils here, but don't want
    // to add dep for one simple test method
    FileInputStream istream = new FileInputStream(file);
    try {
      byte[] bytes = new byte[(int)file.length()];
      istream.read(bytes);
      return bytes;
    } finally {
      istream.close();
    }
  }
  
}
