package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


class UnitDefinitionsRow extends DynamicRow {
  private JTextField textFieldUnitName;
  private JTextField textFieldBuyCost;
  private JTextField textFieldBuyQuantity;

  public UnitDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName,
      final int buyCost, final int buyQuantity) {
    super(unitName, parentRowPanel, stepActionPanel);

    textFieldUnitName = new JTextField(unitName);
    final Integer buyCostInteger = Integer.valueOf(buyCost);
    textFieldBuyCost = new JTextField(buyCostInteger == null ? "0" : Integer.toString(buyCostInteger));
    final Integer buyQuantityInteger = Integer.valueOf(buyQuantity);
    textFieldBuyQuantity = new JTextField(buyQuantityInteger == null ? "1" : Integer.toString(buyQuantityInteger));

    Dimension dimension = textFieldUnitName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    textFieldUnitName.setPreferredSize(dimension);
    textFieldUnitName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldUnitName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.unitDefinitions.containsKey(inputText)) {
          textFieldUnitName.selectAll();
          JOptionPane.showMessageDialog(stepActionPanel, "Unit '" + inputText + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldUnitName.updateUI();
              textFieldUnitName.requestFocus();
              textFieldUnitName.selectAll();
            }
          });
          return;
        }
        // everything is okay with the new player namer, lets rename everything
        final List<Integer> values = MapXMLHelper.unitDefinitions.get(currentRowName);
        MapXMLHelper.unitDefinitions.put(inputText, values);
        if (!MapXMLHelper.productionFrontiers.isEmpty()) {
          for (final Entry<String, List<String>> productionFrontier : MapXMLHelper.productionFrontiers
              .entrySet()) {
            final List<String> frontierValues = productionFrontier.getValue();
            final int index = frontierValues.indexOf(currentRowName);
            if (index >= 0)
              frontierValues.set(index, inputText);
          }
        }
        currentRowName = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldUnitName.selectAll();
      }
    });

    dimension = textFieldBuyCost.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldBuyCost.setPreferredSize(dimension);
    textFieldBuyCost.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldBuyCost.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          MapXMLHelper.unitDefinitions.get(unitName).set(0, newValue);
        } catch (NumberFormatException e) {
          textFieldBuyCost.setText("0");
          MapXMLHelper.unitDefinitions.get(unitName).set(0, 0);
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
        }
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldBuyCost.selectAll();
      }
    });

    dimension = textFieldBuyQuantity.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldBuyQuantity.setPreferredSize(dimension);
    textFieldBuyQuantity.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldBuyQuantity.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          MapXMLHelper.unitDefinitions.get(unitName).set(1, newValue);
        } catch (NumberFormatException e) {
          textFieldBuyQuantity.setText("1");
          MapXMLHelper.unitDefinitions.get(unitName).set(1, 1);
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
        }
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldBuyQuantity.selectAll();
      }
    });

  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldUnitName);
    componentList.add(textFieldBuyCost);
    componentList.add(textFieldBuyQuantity);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldUnitName, gbc_template);

    final GridBagConstraints gbc_tBuyCost = (GridBagConstraints) gbc_template.clone();
    gbc_tBuyCost.gridx = 1;
    parent.add(textFieldBuyCost, gbc_tBuyCost);

    final GridBagConstraints gbc_tBuyQuantity = (GridBagConstraints) gbc_template.clone();
    gbc_tBuyQuantity.gridx = 2;
    parent.add(textFieldBuyQuantity, gbc_tBuyQuantity);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 3;
    parent.add(buttonRemoveRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final UnitDefinitionsRow newRowPlayerAndAlliancesRow = (UnitDefinitionsRow) newRow;
    this.textFieldUnitName.setText(newRowPlayerAndAlliancesRow.textFieldUnitName.getText());
    this.textFieldBuyCost.setText(newRowPlayerAndAlliancesRow.textFieldBuyCost.getText());
    this.textFieldBuyQuantity.setText(newRowPlayerAndAlliancesRow.textFieldBuyQuantity.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.unitDefinitions.remove(currentRowName);
    if (!MapXMLHelper.productionFrontiers.isEmpty()) {
      for (final Entry<String, List<String>> productionFrontier : MapXMLHelper.productionFrontiers.entrySet()) {
        final List<String> frontierValues = productionFrontier.getValue();
        final int index = frontierValues.indexOf(currentRowName);
        if (index >= 0)
          frontierValues.remove(index);
      }
    }
  }
}
