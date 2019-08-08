package games.strategy.sound;

import games.strategy.engine.framework.ui.PropertiesSelector;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/** Sound option window framework. */
public final class SoundOptions {
  private SoundOptions(final JComponent parent) {
    final ClipPlayer clipPlayer = ClipPlayer.getInstance();
    final String ok = "OK";
    final String cancel = "Cancel";
    final String selectAll = "All";
    final String selectNone = "None";

    final List<SoundOptionCheckBox> properties = SoundPath.getSoundOptions();
    final Object pressedButton =
        PropertiesSelector.getButton(
            parent, "Sound Options", properties, ok, selectAll, selectNone, cancel);
    if (pressedButton == null || pressedButton.equals(cancel)) {
      return;
    }
    if (pressedButton.equals(ok)) {
      for (final SoundOptionCheckBox property : properties) {
        clipPlayer.setMute(property.getClipName(), !property.getValue());
      }
      clipPlayer.saveSoundPreferences();
    } else if (pressedButton.equals(selectAll)) {
      for (final SoundOptionCheckBox property : properties) {
        property.setValue(true);
        clipPlayer.setMute(property.getClipName(), false);
      }
      clipPlayer.saveSoundPreferences();
    } else if (pressedButton.equals(selectNone)) {
      for (final SoundOptionCheckBox property : properties) {
        property.setValue(false);
        clipPlayer.setMute(property.getClipName(), true);
      }
      clipPlayer.saveSoundPreferences();
    }
  }

  /**
   * Adds the "Sound Options" menu item to the specified menu.
   *
   * @param parentMenu menu where to add the menu item "Sound Options".
   */
  public static void addToMenu(final JMenu parentMenu) {
    final JMenuItem soundOptions = new JMenuItem("Sound Options");
    soundOptions.setMnemonic(KeyEvent.VK_S);
    soundOptions.addActionListener(e -> new SoundOptions(parentMenu));
    parentMenu.add(soundOptions);
  }

  public static void addGlobalSoundSwitchMenu(final JMenu parentMenu) {
    final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");
    soundCheckBox.setMnemonic(KeyEvent.VK_N);
    soundCheckBox.setSelected(!ClipPlayer.getBeSilent());
    soundCheckBox.addActionListener(e -> ClipPlayer.setBeSilent(!soundCheckBox.isSelected()));
    parentMenu.add(soundCheckBox);
  }
}
