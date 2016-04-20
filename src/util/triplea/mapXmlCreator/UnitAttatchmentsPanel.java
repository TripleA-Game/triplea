package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingAction;


public class UnitAttatchmentsPanel extends DynamicRowsPanel {

  private String unitName;

  public UnitAttatchmentsPanel(final JPanel stepActionPanel, final String unitName) {
    super(stepActionPanel);
    this.unitName = unitName;
  }

  public static void layout(final MapXmlCreator mapXmlCreator, final JPanel stepActionPanel, final String unitName) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof UnitAttatchmentsPanel)
        || ((UnitAttatchmentsPanel) me.get()).unitName != unitName)
      me = Optional.of(new UnitAttatchmentsPanel(stepActionPanel, unitName));
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {
    final ArrayList<ArrayList<String>> unitAttatchments = new ArrayList<ArrayList<String>>();
    for (final Entry<String, List<String>> unitAttatchmentEntry : MapXmlHelper.getUnitAttatchmentsMap().entrySet()) {
      final String unitAttatmentKey = unitAttatchmentEntry.getKey();
      if (unitAttatmentKey.endsWith("_" + unitName)) {
        final ArrayList<String> newAttachment = new ArrayList<String>();
        newAttachment.add(unitAttatmentKey.substring(0, unitAttatmentKey.lastIndexOf("_" + unitName)));
        newAttachment.add(unitAttatchmentEntry.getValue().get(1));
        unitAttatchments.add(newAttachment);
      }
    }

    final JLabel labelAttatchmentName = new JLabel("Attatchment Name");
    Dimension dimension = labelAttatchmentName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    labelAttatchmentName.setPreferredSize(dimension);

    final JLabel labelValue = new JLabel("Value");
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    labelValue.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, unitAttatchments.size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Unit Name, Alliance Name, Buy Quantity
    GridBagConstraints gridBadConstLabelAttatchmentName = new GridBagConstraints();
    gridBadConstLabelAttatchmentName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelAttatchmentName.gridy = 0;
    gridBadConstLabelAttatchmentName.gridx = 0;
    gridBadConstLabelAttatchmentName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelAttatchmentName, gridBadConstLabelAttatchmentName);


    GridBagConstraints gridBadConstLabelValue = (GridBagConstraints) gridBadConstLabelAttatchmentName.clone();
    gridBadConstLabelValue.gridx = 1;
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    getOwnPanel().add(labelValue, gridBadConstLabelValue);

    // <3> Add Main Input Rows
    int yValue = 1;
    for (final ArrayList<String> unitAttatchment : unitAttatchments) {
      GridBagConstraints gbc_tAttatchmentName = (GridBagConstraints) gridBadConstLabelAttatchmentName.clone();
      gbc_tAttatchmentName.gridx = 0;
      gridBadConstLabelAttatchmentName.gridy = yValue;
      final UnitAttatchmentsRow newRow =
          new UnitAttatchmentsRow(this, getOwnPanel(), unitName, unitAttatchment.get(0), unitAttatchment.get(1));
      newRow.addToParentComponentWithGbc(getOwnPanel(), yValue, gbc_tAttatchmentName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddAttatchment = new JButton("Add Attatchment");

    buttonAddAttatchment.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddAttatchment.addActionListener(SwingAction.of("Add Attatchment", e -> {
      String newAttatchmentName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new attatchment name:",
          "Attatchment" + (rows.size() + 1));
      if (newAttatchmentName == null || newAttatchmentName.isEmpty())
        return;
      newAttatchmentName = newAttatchmentName.trim();
      final String newUnitAttatchmentKey = newAttatchmentName + "_" + unitName;
      if (MapXmlHelper.getUnitAttatchmentsMap().containsKey(newUnitAttatchmentKey)) {
        JOptionPane.showMessageDialog(getOwnPanel(),
            "Attatchment '" + newAttatchmentName + "' already exists for unit '" + unitName + "'.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      final ArrayList<String> unitAttatchment = new ArrayList<String>();
      unitAttatchment.add(unitName);
      unitAttatchment.add("");
      MapXmlHelper.putUnitAttatchments(newUnitAttatchmentKey, unitAttatchment);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), rows.size() + 1);
      addRowWith(newAttatchmentName, "");
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddAttatchment);

    GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelAttatchmentName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newAttatchmentName, final String value) {
    final UnitAttatchmentsRow newRow = new UnitAttatchmentsRow(this, getOwnPanel(), unitName, newAttatchmentName, value);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {}

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 30, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0};
  }
}
