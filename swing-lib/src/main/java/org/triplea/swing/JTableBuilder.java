package org.triplea.swing;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.google.common.base.Preconditions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Example usage:.
 * <code><pre>
 *   final JTable panel = JTableBuilder.builder()
 *       .columnNames(columns)
 *       .tableData(rows)
 *       .build();
 * </pre></code>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JTableBuilder {

  private List<List<String>> rowData;
  private List<String> columnNames;


  public static JTableBuilder builder() {
    return new JTableBuilder();
  }

  public JTable build() {
    Preconditions.checkNotNull(columnNames);
    Preconditions.checkNotNull(rowData);
    verifyRowLengthsMatchHeader(rowData, columnNames.size());

    final DefaultTableModel model = new DefaultTableModel();
    columnNames.forEach(model::addColumn);
    rowData.stream()
        .map(row -> row.toArray(new String[0]))
        .forEach(model::addRow);
    return new JTable(model);
  }

  /**
   * Make sure that the table is 'rectangular'. Given a number of headers, each row of data should
   * have the same number of columns.
   */
  private static void verifyRowLengthsMatchHeader(final List<List<String>> rowData, final int headerCount) {
    IntStream.range(0, rowData.size())
        .forEach(
            i -> checkArgument(
                rowData.get(i).size() == headerCount,
                String.format(
                    "Data row number: %s, had incorrect length: %s, needed to match number of column headers: %s,"
                        + "data row: %s",
                    i,
                    rowData.get(i).size(),
                    headerCount,
                    rowData.get(i))));
  }

  /**
   * Convenience method for adding data rows to a given JTable.
   *
   * @param table The table to modify where we will add data rows.
   * @param rows Data rows to be added.
   */
  public static void addRows(final JTable table, final List<List<String>> rows) {
    final DefaultTableModel model = (DefaultTableModel) table.getModel();
    rows.forEach(row -> model.addRow(row.toArray(new String[0])));
  }


  public JTableBuilder columnNames(final String... columnNames) {
    return columnNames(Arrays.asList(columnNames));
  }

  public JTableBuilder columnNames(final List<String> columnNames) {
    checkArgument(!columnNames.isEmpty());
    this.columnNames = columnNames;
    return this;
  }

  public JTableBuilder tableData(final List<List<String>> rowData) {
    this.rowData = rowData;
    return this;
  }
}
