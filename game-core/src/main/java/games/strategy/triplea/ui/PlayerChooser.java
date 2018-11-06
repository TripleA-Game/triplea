package games.strategy.triplea.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.PlayerList;
import games.strategy.ui.SwingComponents;
import games.strategy.ui.Util;

class PlayerChooser extends JOptionPane {
  private static final long serialVersionUID = -7272867474891641839L;
  private JList<PlayerId> list;
  private final PlayerList players;
  private final PlayerId defaultPlayer;
  private final UiContext uiContext;
  private final boolean allowNeutral;

  PlayerChooser(final PlayerList players, final UiContext uiContext, final boolean allowNeutral) {
    this(players, null, uiContext, allowNeutral);
  }

  PlayerChooser(final PlayerList players, final PlayerId defaultPlayer, final UiContext uiContext,
      final boolean allowNeutral) {
    setMessageType(JOptionPane.PLAIN_MESSAGE);
    setOptionType(JOptionPane.OK_CANCEL_OPTION);
    setIcon(null);
    this.players = players;
    this.defaultPlayer = defaultPlayer;
    this.uiContext = uiContext;
    this.allowNeutral = allowNeutral;
    createComponents();
  }

  private void createComponents() {
    final Collection<PlayerId> players = new ArrayList<>(this.players.getPlayers());
    if (allowNeutral) {
      players.add(PlayerId.NULL_PLAYERID);
    }
    list = new JList<>(players.toArray(new PlayerId[0]));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedValue(defaultPlayer, true);
    list.setFocusable(false);
    list.setCellRenderer(new PlayerChooserRenderer(uiContext));
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent evt) {
        if (evt.getClickCount() == 2) {
          // set OK_OPTION on DoubleClick, this fires a property change which causes the dialog to close()
          setValue(OK_OPTION);
        }
      }
    });
    setMessage(SwingComponents.newJScrollPane(list));

    final int maxSize = 700;
    final int suggestedSize = this.players.size() * 40;
    final int actualSize = suggestedSize > maxSize ? maxSize : suggestedSize;
    setPreferredSize(new Dimension(300, actualSize));
  }


  /**
   * Returns the selected player or null, or null if the dialog was closed.
   *
   * @return the player or null
   */
  PlayerId getSelected() {
    if (getValue() != null && getValue().equals(JOptionPane.OK_OPTION)) {
      return list.getSelectedValue();
    }
    return null;
  }

  private static final class PlayerChooserRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -2185921124436293304L;
    private final UiContext uiContext;

    PlayerChooserRenderer(final UiContext uiContext) {
      this.uiContext = uiContext;
    }

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
      super.getListCellRendererComponent(list, ((PlayerId) value).getName(), index, isSelected, cellHasFocus);
      if (uiContext == null || value == PlayerId.NULL_PLAYERID) {
        setIcon(new ImageIcon(Util.createImage(32, 32, true)));
      } else {
        setIcon(new ImageIcon(uiContext.getFlagImageFactory().getFlag((PlayerId) value)));
      }
      return this;
    }
  }
}
