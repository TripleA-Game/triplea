package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;


public class RepairPanel extends ActionPanel {
  private static final long serialVersionUID = 3045997038627313714L;
  private final JLabel actionLabel = new JLabel();
  private HashMap<Unit, IntegerMap<RepairRule>> m_repair;
  private boolean m_bid;
  private Collection<PlayerID> m_allowedPlayersToRepair;
  private final SimpleUnitPanel m_unitsPanel;
  private final JLabel m_repairdSoFar = new JLabel();
  private final JButton m_buyButton;
  private final String BUY = "Repair...";
  private final String CHANGE = "Change...";

  /** Creates new RepairPanel */
  public RepairPanel(final GameData data, final MapPanel map) {
    super(data, map);
    m_unitsPanel = new SimpleUnitPanel(map.getUIContext());
    m_buyButton = new JButton(BUY);
    m_buyButton.addActionListener(PURCHASE_ACTION);
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    m_repair = new HashMap<Unit, IntegerMap<RepairRule>>();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeAll();
        actionLabel.setText(id.getName() + " repair");
        m_buyButton.setText(BUY);
        add(actionLabel);
        add(m_buyButton);
        add(new JButton(DoneAction));
        m_repairdSoFar.setText("");
        add(Box.createVerticalStrut(9));
        add(m_repairdSoFar);
        add(Box.createVerticalStrut(4));
        m_unitsPanel.setUnitsFromRepairRuleMap(new HashMap<Unit, IntegerMap<RepairRule>>(), id, getData());
        add(m_unitsPanel);
        add(Box.createVerticalGlue());
        SwingUtilities.invokeLater(REFRESH);
      }
    });
  }

  private void refreshActionLabelText() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        actionLabel.setText(getCurrentPlayer().getName() + " repair " + (m_bid ? " for bid" : ""));
      }
    });
  }

  public HashMap<Unit, IntegerMap<RepairRule>> waitForRepair(final boolean bid, final Collection<PlayerID> allowedPlayersToRepair) {
    m_bid = bid;
    m_allowedPlayersToRepair = allowedPlayersToRepair;
    refreshActionLabelText();
    // automatically "click" the buy button for us!
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        PURCHASE_ACTION.actionPerformed(null);
      }
    });
    waitForRelease();
    return m_repair;
  }

  private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy") {
    private static final long serialVersionUID = 5572043262815077402L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      final PlayerID player = getCurrentPlayer();
      final GameData data = getData();
      m_repair = ProductionRepairPanel.getProduction(player, m_allowedPlayersToRepair, (JFrame) getTopLevelAncestor(), data, m_bid,
          m_repair, getMap().getUIContext());
      m_unitsPanel.setUnitsFromRepairRuleMap(m_repair, player, data);
      final int totalValues = getTotalValues(m_repair);
      if (totalValues == 0) {
        m_repairdSoFar.setText("");
        m_buyButton.setText(BUY);
      } else {
        m_buyButton.setText(CHANGE);
        m_repairdSoFar.setText(totalValues + MyFormatter.pluralize(" unit", totalValues) + " to be repaired:");
      }
    }
  };

  // Spin through the territories to get this.
  private int getTotalValues(final HashMap<Unit, IntegerMap<RepairRule>> m_repair) {
    final Collection<Unit> units = m_repair.keySet();
    final Iterator<Unit> iter = units.iterator();
    int totalValues = 0;
    while (iter.hasNext()) {
      final Unit unit = iter.next();
      totalValues += m_repair.get(unit).totalValues();
    }
    return totalValues;
  }

  private final Action DoneAction = new AbstractAction("Done") {
    private static final long serialVersionUID = -2002286381161651398L;

    @Override
    public void actionPerformed(final ActionEvent event) {
      final boolean hasPurchased = getTotalValues(m_repair) != 0;
      if (!hasPurchased) {
        final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(RepairPanel.this),
            "Are you sure you dont want to repair anything?", "End Purchase",
            JOptionPane.YES_NO_OPTION);
        if (rVal != JOptionPane.YES_OPTION) {
          return;
        }
      }
      release();
    }
  };

  @Override
  public String toString() {
    return "RepairPanel";
  }
}
