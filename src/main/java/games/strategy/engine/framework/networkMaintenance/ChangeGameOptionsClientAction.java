package games.strategy.engine.framework.networkMaintenance;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;

public class ChangeGameOptionsClientAction extends AbstractAction {
  private static final long serialVersionUID = -6419002646689952824L;
  private final Component m_parent;
  private final IServerStartupRemote m_serverRemote;

  public ChangeGameOptionsClientAction(final Component parent, final IServerStartupRemote serverRemote) {
    super("Edit Game Options");
    m_parent = JOptionPane.getFrameForComponent(parent);
    m_serverRemote = serverRemote;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final byte[] oldBytes = m_serverRemote.getGameOptions();
    if (oldBytes == null || oldBytes.length == 0) {
      return;
    }
    try {
      final List<IEditableProperty> properties = GameProperties.readEditableProperties(oldBytes);
      final PropertiesUI pui = new PropertiesUI(properties, true);
      final JScrollPane scroll = new JScrollPane(pui);
      scroll.setBorder(null);
      scroll.getViewport().setBorder(null);
      final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
      final String ok = "OK";
      final String cancel = "Cancel";
      pane.setOptions(new Object[] {ok, cancel});
      final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(m_parent), "Map Options");
      window.setVisible(true);
      final Object buttonPressed = pane.getValue();
      if (buttonPressed != null && !buttonPressed.equals(cancel)) {
        // ok was clicked. changing them in the ui changes the underlying properties,
        // but it doesn't change the hosts, so we need to send it back to the host.
        try {
          m_serverRemote.changeToGameOptions(GameProperties.writeEditableProperties(properties));
        } catch (final IOException ex) {
          ClientLogger.logQuietly(ex);
        }
      }
    } catch (final IOException | ClassCastException ex) {
      ClientLogger.logQuietly(ex);
    }
  }
}
