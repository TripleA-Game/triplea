package tools.map.xml.creator;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;


public class TerritoryDefinitionDialog extends JDialog {
  public static enum DEFINITION {
    IS_WATER, IS_VICTORY_CITY, IMPASSABLE, IS_CAPITAL
  }

  public static String getDefinitionString(final DEFINITION def) {
    switch (def) {
      case IS_WATER:
        return TERRITORY_DEFINITION_IS_WATER;
      case IS_VICTORY_CITY:
        return TERRITORY_DEFINITION_IS_VICTORY_CITY;
      case IMPASSABLE:
        return TERRITORY_DEFINITION_IMPASSABLE;
      case IS_CAPITAL:
        return TERRITORY_DEFINITION_IS_CAPITAL;
      default:
        throw new IllegalArgumentException(
            "Provided value is not valid for " + DEFINITION.class);
    }
  }

  public static DEFINITION valueOf(final String defString) {
    if (defString.equals(TERRITORY_DEFINITION_IS_WATER)) {
      return DEFINITION.IS_WATER;
    } else if (defString.equals(TERRITORY_DEFINITION_IS_VICTORY_CITY)) {
      return DEFINITION.IS_VICTORY_CITY;
    } else if (defString.equals(TERRITORY_DEFINITION_IMPASSABLE)) {
      return DEFINITION.IMPASSABLE;
    } else if (defString.equals(TERRITORY_DEFINITION_IS_CAPITAL)) {
      return DEFINITION.IS_CAPITAL;
    }
    throw new IllegalArgumentException(
        "'" + defString + "' is not a valid string for " + DEFINITION.class);
  }

  private static final long serialVersionUID = -2504102953599720855L;
  public static final String TERRITORY_DEFINITION_IS_WATER = "water";
  public static final String TERRITORY_DEFINITION_IS_VICTORY_CITY = "victoryCity";
  public static final String TERRITORY_DEFINITION_IMPASSABLE = "isImpassable"; // typo in engine!!!
  public static final String TERRITORY_DEFINITION_IS_CAPITAL = "capital";

  private final Map<DEFINITION, Boolean> properties;
  private final JButton okButton;

  TerritoryDefinitionDialog(final MapXmlCreator mapXmlCreator, final String territoryName,
      final Map<DEFINITION, Boolean> properties) {
    this(mapXmlCreator, JOptionPane.getFrameForComponent(null), territoryName, properties);
  }

  private TerritoryDefinitionDialog(final MapXmlCreator mapXmlCreator, final Frame parentFrame,
      final String territoryName, final Map<DEFINITION, Boolean> properties) {
    super(parentFrame, "Edit Territory Definitions of " + territoryName, true);
    this.properties = properties;
    okButton = new JButton("OK");
    okButton.addActionListener(new AbstractAction("OK") {
      private static final long serialVersionUID = 8014389179875584858L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        MapXmlHelper.getTerritoryDefintionsMap().put(territoryName, properties);
        setVisible(false);
      }
    });
    layoutCoponents(territoryName);
    mapXmlCreator.validateAndRepaint();
    // Listen for windowOpened event to set focus
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(final WindowEvent e) {
        okButton.requestFocus();
      }

      @Override
      public void windowClosing(final WindowEvent e) {
        MapXmlHelper.getTerritoryDefintionsMap().put(territoryName, properties);
        super.windowClosing(e);
      }
    });

    this.pack();
    this.setLocationRelativeTo(parentFrame);
    this.setVisible(true);
  }

  private void layoutCoponents(final String territoryName) {
    setLayout(new BorderLayout());
    final JPanel buttonsPanel = new JPanel();
    add(buttonsPanel, BorderLayout.CENTER);
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
    buttonsPanel.add(Box.createGlue());
    buttonsPanel.add(new JLabel("Territory Definitions of " + territoryName + ": "));
    buttonsPanel.add(new JLabel(" "));
    // add buttons here:
    final JCheckBox cbIsWater = new JCheckBox("is water territory");
    final Boolean isWater = properties.get(DEFINITION.IS_WATER);
    cbIsWater.setSelected((isWater != null ? isWater : false));
    cbIsWater.addActionListener(e -> properties.put(DEFINITION.IS_WATER, cbIsWater.isSelected()));
    buttonsPanel.add(cbIsWater);
    buttonsPanel.add(new JLabel(" "));

    final JCheckBox cbIsVictoryCity = new JCheckBox("is victory city");
    final Boolean isVictoryCity = properties.get(DEFINITION.IS_VICTORY_CITY);
    cbIsVictoryCity.setSelected((isVictoryCity != null ? isVictoryCity : false));
    cbIsVictoryCity.addActionListener(e -> properties.put(DEFINITION.IS_VICTORY_CITY, cbIsVictoryCity.isSelected()));
    buttonsPanel.add(cbIsVictoryCity);
    buttonsPanel.add(new JLabel(" "));

    final JCheckBox cbImpassable = new JCheckBox("is impassable");
    final Boolean impassable = properties.get(DEFINITION.IMPASSABLE);
    cbImpassable.setSelected((impassable != null ? impassable : false));
    cbImpassable.addActionListener(e -> properties.put(DEFINITION.IMPASSABLE, cbImpassable.isSelected()));
    buttonsPanel.add(cbImpassable);
    buttonsPanel.add(new JLabel(" "));

    final JCheckBox cbIsCapital = new JCheckBox("is capital");
    final Boolean isCapital = properties.get(DEFINITION.IS_CAPITAL);
    cbIsCapital.setSelected((isCapital != null ? isCapital : false));
    cbIsCapital.addActionListener(e -> properties.put(DEFINITION.IS_CAPITAL, cbIsCapital.isSelected()));
    buttonsPanel.add(cbIsCapital);

    buttonsPanel.add(Box.createGlue());
    buttonsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    final JPanel main = new JPanel();
    main.setBorder(new EmptyBorder(30, 30, 30, 30));
    main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
    main.add(okButton);
    add(main, BorderLayout.SOUTH);
  }


  // public void show(final JComponent parent, final String territoryName, final HashMap<String,Boolean> properties)
  // {
  // final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
  // final TerritoryDefinitionDialog dialog = new TerritoryDefinitionDialog(parentFrame,territoryName,properties);
  // dialog.pack();
  // dialog.setLocationRelativeTo(parentFrame);
  // dialog.setVisible(true);
  // }
}
