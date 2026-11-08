package aralbuddy.view;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/** Reusable read-only table with safe live search, sorting, counts, and record selection. */
@SuppressWarnings("serial")
public final class DataTable extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search = new JTextField(22);
    private final JLabel count = Theme.label("No records", 12, false, Theme.MUTED);
    private final List<Object> keys = new ArrayList<>();

    /** Constructs a table whose columns keep their readable width on small screens. */
    public DataTable(String... columns) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        model = createModel(columns);
        table = new JTable(model);
        configureTable(columns);
        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        installSearchListener();
    }

    private DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            /** Read-only cells keep edits inside validated forms. */
            @Override public boolean isCellEditable(int row, int column) { return false; }
            /** Uses the actual numeric type so 2 sorts before 10. */
            @Override public Class<?> getColumnClass(int column) {
                for (int row = 0; row < getRowCount(); row++) if (getValueAt(row, column) instanceof Number) return getValueAt(row, column).getClass();
                return String.class;
            }
        };
    }

    private void configureTable(String[] columns) {
        table.setName("recordsTable");
        table.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(columns.length <= 3 ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font(Theme.FONT, Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, Theme.TABLE_HEADER_HEIGHT));
        DefaultTableCellRenderer renderer = createRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Number.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);
        table.setDefaultRenderer(Long.class, renderer);
        for (int column = 0; column < columns.length; column++) {
            String title = columns[column];
            int width = title.contains("Course") || title.contains("Topic") || title.contains("Feedback") || title.contains("Reason")
                    ? 250 : title.contains("Availability") || title.equals("Email") || title.equals("Venue") ? 230 : 150;
            table.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
    }

    private DefaultTableCellRenderer createRenderer() {
        return new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            { putClientProperty("html.disable", Boolean.TRUE); }
            /** Adds readable spacing, status color, and full-text tooltips. */
            @Override public Component getTableCellRendererComponent(JTable source, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                putClientProperty("html.disable", Boolean.TRUE);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setBackground(selected ? Theme.TINT : row % 2 == 0 ? Theme.SURFACE : new Color(248, 250, 249));
                setForeground(Theme.TEXT);
                String text = value == null ? "" : value.toString();
                if (!selected && List.of("Confirmed", "Completed", "Approved", "Active").contains(text)) setForeground(Theme.TEAL);
                setToolTipText(text.isEmpty() ? null : text);
                return this;
            }
        };
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0)); toolbar.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(10, 0)); left.setOpaque(false);
        JLabel label = Theme.label("Search", 13, true, Theme.MUTED); label.setLabelFor(search);
        left.add(label, BorderLayout.WEST); left.add(search, BorderLayout.CENTER);
        search.setName("tableSearch"); search.setToolTipText("Filter all columns. Ctrl+F focuses this field.");
        search.getAccessibleContext().setAccessibleName("Search table");
        toolbar.add(left, BorderLayout.WEST); toolbar.add(count, BorderLayout.EAST);
        return toolbar;
    }

    private void installSearchListener() {
        search.getDocument().addDocumentListener(new DocumentListener() {
            /** Refreshes the filter after text is inserted. */
            @Override public void insertUpdate(DocumentEvent event) { filter(); }
            /** Refreshes the filter after text is removed. */
            @Override public void removeUpdate(DocumentEvent event) { filter(); }
            /** Keeps the same filter behavior for document changes. */
            @Override public void changedUpdate(DocumentEvent event) { filter(); }
        });
    }

    /** Clears records while preserving the current search and sorting controls. */
    public void clearRows() { keys.clear(); model.setRowCount(0); updateCount(); }

    /** Adds a visible row and its stable record key. */
    public void addRow(Object key, Object... values) { keys.add(key); model.addRow(values); updateCount(); }

    /** Converts a sorted or filtered view row back to its underlying record. */
    public Object selectedKey() {
        int row = table.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a row in the table first.");
        return keys.get(table.convertRowIndexToModel(row));
    }

    /** Gives the keyboard shortcut a predictable search destination. */
    public void focusSearch() { search.requestFocusInWindow(); search.selectAll(); }

    /** Provides the table for focused GUI verification and selection listeners. */
    public JTable getTable() { return table; }

    @SuppressWarnings("unchecked")
    private void filter() {
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        String query = search.getText().trim();
        sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(query)));
        updateCount();
    }

    private void updateCount() {
        count.setText(model.getRowCount() == 0 ? "No records yet" : table.getRowCount() + " of " + model.getRowCount() + " records");
    }
}
