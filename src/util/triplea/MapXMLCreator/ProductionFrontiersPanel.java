package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class ProductionFrontiersPanel extends DynamicRowsPanel {

  private String playerName;
  private TreeSet<String> allUnitNames;

  public ProductionFrontiersPanel(final JPanel stepActionPanel, final String playerName) {
    super(stepActionPanel);
    this.playerName = playerName;
    allUnitNames = new TreeSet<String>(MapXMLHelper.unitDefinitions.keySet());
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel, final String playerName) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof ProductionFrontiersPanel)
        || ((ProductionFrontiersPanel) me.get()).playerName != playerName)
      me = Optional.of(new ProductionFrontiersPanel(stepActionPanel, playerName));
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(ownPanel,
            "Are you sure you want to use the  Auto-Fill feature?\rIt will remove any information you have entered in this step and propose commonly used choices.",
            "Auto-Fill Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
          MapXMLHelper.clearProductionFrontiers();
          for (final String playerName : MapXMLHelper.playerName)
            MapXMLHelper.putProductionFrontiers(playerName, new ArrayList<String>(allUnitNames));
          // Update UI
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              resetRows();
              ownPanel.revalidate();
              ownPanel.repaint();
              ownPanel.requestFocus();
            }
          });
        }
      }
    };
  }

  protected void layoutComponents() {
    final List<String> playersUnitNames = MapXMLHelper.productionFrontiers.get(playerName);

    final JLabel labelUnitName = new JLabel("Unit Name");
    Dimension dimension = labelUnitName.getPreferredSize();
    dimension.width = 140;
    labelUnitName.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, playersUnitNames.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    GridBagConstraints gridBadConstLabelUnitName = new GridBagConstraints();
    gridBadConstLabelUnitName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelUnitName.gridy = 0;
    gridBadConstLabelUnitName.gridx = 0;
    gridBadConstLabelUnitName.anchor = GridBagConstraints.WEST;
    ownPanel.add(labelUnitName, gridBadConstLabelUnitName);

    // <3> Add Main Input Rows
    int yValue = 1;
    final String[] allUnitNamesArray = allUnitNames.toArray(new String[allUnitNames.size()]);
    for (final String unitName : playersUnitNames) {
      GridBagConstraints gbc_tUnitName = (GridBagConstraints) gridBadConstLabelUnitName.clone();
      gbc_tUnitName.gridx = 0;
      gridBadConstLabelUnitName.gridy = yValue;
      final ProductionFrontiersRow newRow =
          new ProductionFrontiersRow(this, ownPanel, playerName, unitName, allUnitNamesArray);
      newRow.addToComponent(ownPanel, yValue, gbc_tUnitName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddUnit = new JButton("Add Unit");

    buttonAddUnit.setFont(MapXMLHelper.defaultMapXMLCreatorFont);
    buttonAddUnit.addActionListener(new AbstractAction("Add Unit") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        final List<String> curr_playersUnitNames = MapXMLHelper.productionFrontiers.get(playerName);

        // UI Update
        setRows((GridBagLayout) ownPanel.getLayout(), MapXMLHelper.unitDefinitions.size());
        final String[] allUnitNamesArray = allUnitNames.toArray(new String[allUnitNames.size()]);
        final HashSet<String> freeUnitNames = new HashSet<String>(allUnitNames);
        freeUnitNames.removeAll(curr_playersUnitNames);
        final String newUnitName = freeUnitNames.iterator().next();
        if (newUnitName == null) {
          JOptionPane.showMessageDialog(ownPanel, "All units already selected.", "Input error",
              JOptionPane.ERROR_MESSAGE);
        } else {
          curr_playersUnitNames.add(newUnitName);
          addRowWith(newUnitName, allUnitNamesArray);
          SwingUtilities.invokeLater(() -> {
            ownPanel.revalidate();
            ownPanel.repaint();
          });
        }
      }
    });
    addButton(buttonAddUnit);

    GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newUnitName, final String[] unitNames) {
    final ProductionFrontiersRow newRow =
        new ProductionFrontiersRow(this, ownPanel, playerName, newUnitName, unitNames);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {}

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0};
  }
}
