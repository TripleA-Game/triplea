package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import games.strategy.util.Triple;


class GameSequenceRow extends DynamicRow {
  private JTextField textFieldSequenceName;
  private JTextField textFieldClassName;
  private JTextField textFieldDisplayName;

  public GameSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String sequenceName,
      final String className, final String displayName) {
    super(sequenceName, parentRowPanel, stepActionPanel);

    textFieldSequenceName = new JTextField(sequenceName);
    textFieldClassName = new JTextField(className);
    textFieldDisplayName = new JTextField(displayName);

    Dimension dimension = textFieldSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    textFieldSequenceName.setPreferredSize(dimension);
    textFieldSequenceName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldSequenceName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.gamePlaySequence.containsKey(inputText)) {
          textFieldSequenceName.selectAll();
          JOptionPane.showMessageDialog(stepActionPanel, "Sequence '" + inputText + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
              textFieldSequenceName.requestFocus();
            }
          });
          return;
        }
        // everything is okay with the new player namer, lets rename everything
        final List<String> values = MapXMLHelper.gamePlaySequence.get(currentRowName);
        MapXMLHelper.gamePlaySequence.put(inputText, values);
        if (!MapXMLHelper.playerSequence.isEmpty()) {
          // Replace Game Sequence for Player Sequence
          final Map<String, Triple<String, String, Integer>> updatesPlayerSequence =
              Maps.newLinkedHashMap();
          for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.playerSequence
              .entrySet()) {
            final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
            if (currentRowName.equals(oldTriple.getFirst())) {
              updatesPlayerSequence.put(playerSequence.getKey(),
                  Triple.of(inputText, oldTriple.getSecond(), oldTriple.getThird()));
            }
          }
          for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet()) {
            MapXMLHelper.playerSequence.put(playerSequence.getKey(), playerSequence.getValue());
          }
        }
        currentRowName = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldSequenceName.selectAll();
      }
    });

    dimension = textFieldClassName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldClassName.setPreferredSize(dimension);
    textFieldClassName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldClassName.getText().trim();
        MapXMLHelper.gamePlaySequence.get(sequenceName).set(0, inputText);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldClassName.selectAll();
      }
    });

    dimension = textFieldDisplayName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldDisplayName.setPreferredSize(dimension);
    textFieldDisplayName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldDisplayName.getText().trim();
        MapXMLHelper.gamePlaySequence.get(sequenceName).set(1, inputText);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldDisplayName.selectAll();
      }
    });

  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldSequenceName);
    componentList.add(textFieldClassName);
    componentList.add(textFieldDisplayName);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldSequenceName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(textFieldClassName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(textFieldDisplayName, gbc_tDisplayName);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 3;
    parent.add(buttonRemoveRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final GameSequenceRow newRowPlayerAndAlliancesRow = (GameSequenceRow) newRow;
    this.textFieldSequenceName.setText(newRowPlayerAndAlliancesRow.textFieldSequenceName.getText());
    this.textFieldClassName.setText(newRowPlayerAndAlliancesRow.textFieldClassName.getText());
    this.textFieldDisplayName.setText(newRowPlayerAndAlliancesRow.textFieldDisplayName.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.gamePlaySequence.remove(currentRowName);

    if (!MapXMLHelper.playerSequence.isEmpty()) {
      // Replace Player Sequences using the deleted Game Sequence
      final ArrayList<String> deleteKeys = new ArrayList<String>();
      for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.playerSequence
          .entrySet()) {
        final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
        if (currentRowName.equals(oldTriple.getFirst())) {
          deleteKeys.add(playerSequence.getKey());
        }
      }
      for (final String deleteKey : deleteKeys)
        MapXMLHelper.playerSequence.remove(deleteKey);
    }
  }
}
