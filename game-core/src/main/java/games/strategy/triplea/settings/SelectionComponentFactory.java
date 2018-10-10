package games.strategy.triplea.settings;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.google.common.base.Strings;

import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * Logic for building UI components that "bind" to ClientSettings.
 * For example, if we have a setting that needs a number, we could create an integer text field with this
 * class. This class takes care of the UI code to ensure we render the proper swing component with validation.
 */
final class SelectionComponentFactory {
  private SelectionComponentFactory() {}

  static SelectionComponent<JComponent> proxySettings(
      final ClientSetting<HttpProxy.ProxyChoice> proxyChoiceClientSetting,
      final ClientSetting<String> proxyHostClientSetting,
      final ClientSetting<String> proxyPortClientSetting) {
    return new SelectionComponent<JComponent>() {
      final HttpProxy.ProxyChoice proxyChoice = proxyChoiceClientSetting.value();
      final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);
      final JRadioButton systemButton =
          new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
      final JRadioButton userButton =
          new JRadioButton("Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final JTextField hostText = new JTextField(proxyHostClientSetting.value(), 20);
      final JTextField portText = new JTextField(proxyPortClientSetting.value(), 6);
      final JPanel radioPanel = JPanelBuilder.builder()
          .verticalBoxLayout()
          .addLeftJustified(noneButton)
          .addLeftJustified(systemButton)
          .addLeftJustified(userButton)
          .addLeftJustified(JPanelBuilder.builder()
              .horizontalBoxLayout()
              .addHorizontalStrut(getRadioButtonLabelHorizontalOffset())
              .add(JPanelBuilder.builder()
                  .verticalBoxLayout()
                  .addLeftJustified(new JLabel("Proxy Host:"))
                  .addLeftJustified(hostText)
                  .addLeftJustified(new JLabel("Proxy Port:"))
                  .addLeftJustified(portText)
                  .build())
              .build())
          .build();
      final ActionListener enableUserSettings = e -> {
        if (userButton.isSelected()) {
          hostText.setEnabled(true);
          portText.setEnabled(true);
        } else {
          hostText.setEnabled(false);
          portText.setEnabled(false);
        }
      };

      @Override
      public JComponent getUiComponent() {
        SwingComponents.createButtonGroup(noneButton, systemButton, userButton);
        enableUserSettings.actionPerformed(null);
        userButton.addActionListener(enableUserSettings);
        noneButton.addActionListener(enableUserSettings);
        systemButton.addActionListener(enableUserSettings);

        return radioPanel;
      }

      @Override
      public boolean isValid() {
        return !userButton.isSelected() || (isHostTextValid() && isPortTextValid());
      }

      private boolean isHostTextValid() {
        return !Strings.nullToEmpty(hostText.getText()).trim().isEmpty();
      }

      private boolean isPortTextValid() {
        final String value = Strings.nullToEmpty(portText.getText()).trim();
        if (value.isEmpty()) {
          return false;
        }

        try {
          return Integer.parseInt(value) > 0;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      public String validValueDescription() {
        return "Proxy host can be a network name or an IP address, port should be number, usually 4 to 5 digits.";
      }

      @Override
      public Map<GameSetting<?>, String> readValues() {
        final Map<GameSetting<?>, String> values = new HashMap<>();
        if (noneButton.isSelected()) {
          values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.NONE.toString());
        } else if (systemButton.isSelected()) {
          values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS.toString());
          HttpProxy.updateSystemProxy();
        } else {
          values.put(proxyChoiceClientSetting, HttpProxy.ProxyChoice.USE_USER_PREFERENCES.toString());
          values.put(proxyHostClientSetting, hostText.getText().trim());
          values.put(proxyPortClientSetting, portText.getText().trim());
        }
        return values;
      }

      @Override
      public void resetToDefault() {
        ClientSetting.flush();
        hostText.setText(proxyHostClientSetting.defaultValue());
        portText.setText(proxyPortClientSetting.defaultValue());
        setProxyChoice(proxyChoiceClientSetting.defaultValue());
      }

      private void setProxyChoice(final HttpProxy.ProxyChoice proxyChoice) {
        noneButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.NONE);
        systemButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);
        userButton.setSelected(proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
        enableUserSettings.actionPerformed(null);
      }

      @Override
      public void reset() {
        ClientSetting.flush();
        hostText.setText(proxyHostClientSetting.value());
        portText.setText(proxyPortClientSetting.value());
        setProxyChoice(proxyChoiceClientSetting.value());
      }
    };
  }

  private static int getRadioButtonLabelHorizontalOffset() {
    final JRadioButton radioButton = new JRadioButton("\u200B"); // zero-width space
    return radioButton.getPreferredSize().width - radioButton.getInsets().right;
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static SelectionComponent<JComponent> intValueRange(
      final ClientSetting<String> clientSetting,
      final int lo,
      final int hi) {
    return intValueRange(clientSetting, lo, hi, false);
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static SelectionComponent<JComponent> intValueRange(final ClientSetting<String> clientSetting, final int lo,
      final int hi, final boolean allowUnset) {
    return new SelectionComponent<JComponent>() {
      private final JSpinner component = new JSpinner(new SpinnerNumberModel(
          toValidIntValue(clientSetting.value()), lo - (allowUnset ? 1 : 0), hi, 1));

      @Override
      public JComponent getUiComponent() {
        return component;
      }

      private int toValidIntValue(final String value) {
        return value.isEmpty() && allowUnset ? lo - 1 : Integer.parseInt(value);
      }

      private String toValidStringValue(final int value) {
        return allowUnset && value == lo - 1 ? "" : String.valueOf(value);
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public String validValueDescription() {
        return "";
      }

      @Override
      public Map<GameSetting<?>, String> readValues() {
        return Collections.singletonMap(clientSetting, toValidStringValue((int) component.getValue()));
      }

      @Override
      public void resetToDefault() {
        component.setValue(toValidIntValue(clientSetting.defaultValue()));
      }

      @Override
      public void reset() {
        component.setValue(toValidIntValue(clientSetting.value()));
      }
    };
  }

  /**
   * yes/no radio buttons.
   */
  static SelectionComponent<JComponent> booleanRadioButtons(final ClientSetting<Boolean> clientSetting) {
    return new AlwaysValidInputSelectionComponent() {
      final boolean initialSelection = clientSetting.value();
      final JRadioButton yesButton = new JRadioButton("True");
      final JRadioButton noButton = new JRadioButton("False");
      final JPanel buttonPanel = JPanelBuilder.builder()
          .horizontalBoxLayout()
          .add(yesButton)
          .add(noButton)
          .build();

      @Override
      public JComponent getUiComponent() {
        yesButton.setSelected(initialSelection);
        noButton.setSelected(!initialSelection);
        SwingComponents.createButtonGroup(yesButton, noButton);
        return buttonPanel;
      }

      @Override
      public Map<GameSetting<?>, String> readValues() {
        final String value = yesButton.isSelected() ? String.valueOf(true) : String.valueOf(false);
        final Map<GameSetting<?>, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        yesButton.setSelected(Boolean.valueOf(clientSetting.defaultValue()));
        noButton.setSelected(!Boolean.valueOf(clientSetting.defaultValue()));
      }

      @Override
      public void reset() {
        yesButton.setSelected(clientSetting.value());
        noButton.setSelected(!clientSetting.value());
      }
    };
  }

  /**
   * File selection prompt.
   */
  static SelectionComponent<JComponent> filePath(final ClientSetting<String> clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.FILES);
  }

  private static SelectionComponent<JComponent> selectFile(
      final ClientSetting<String> clientSetting,
      final SwingComponents.FolderSelectionMode folderSelectionMode) {
    return new AlwaysValidInputSelectionComponent() {
      final int expectedLength = 20;
      final JTextField field = new JTextField(clientSetting.value(), expectedLength);
      final JButton button = JButtonBuilder.builder()
          .title("Select")
          .actionListener(
              () -> SwingComponents.showJFileChooser(folderSelectionMode)
                  .ifPresent(file -> field.setText(file.getAbsolutePath())))
          .build();

      @Override
      public JComponent getUiComponent() {
        field.setEditable(false);

        return JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(field)
            .addHorizontalStrut(5)
            .add(button)
            .build();
      }

      @Override
      public Map<GameSetting<?>, String> readValues() {
        final String value = field.getText();
        final Map<GameSetting<?>, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        field.setText(clientSetting.defaultValue());
      }

      @Override
      public void reset() {
        field.setText(clientSetting.value());
      }
    };
  }

  /**
   * Folder selection prompt.
   */
  static SelectionComponent<JComponent> folderPath(final ClientSetting<String> clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.DIRECTORIES);
  }

  static <T> SelectionComponent<JComponent> selectionBox(
      final ClientSetting<String> clientSetting,
      final List<T> availableOptions,
      final T selectedOption,
      final Function<T, ?> renderFunction) {
    return new AlwaysValidInputSelectionComponent() {
      final JComboBox<T> comboBox = getCombobox();

      private JComboBox<T> getCombobox() {
        final JComboBox<T> comboBox = new JComboBox<>();
        availableOptions.forEach(comboBox::addItem);
        comboBox.setSelectedItem(selectedOption);
        comboBox.setRenderer(new DefaultListCellRenderer() {
          private static final long serialVersionUID = -3094995494539073655L;

          @Override
          @SuppressWarnings("unchecked")
          public Component getListCellRendererComponent(
              final JList<?> list, final Object value, final int index, final boolean isSelected,
              final boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, renderFunction.apply((T) value), index, isSelected,
                cellHasFocus);
          }
        });
        return comboBox;
      }

      @Override
      public JComponent getUiComponent() {
        comboBox.setSelectedItem(clientSetting.value());
        return comboBox;
      }

      @Override
      public Map<GameSetting<?>, String> readValues() {
        final String value = String.valueOf(comboBox.getSelectedItem());
        final Map<GameSetting<?>, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        comboBox.setSelectedItem(clientSetting.defaultValue());
      }

      @Override
      public void reset() {
        comboBox.setSelectedItem(clientSetting.value());
      }
    };
  }

  static SelectionComponent<JComponent> textField(final ClientSetting<String> clientSetting) {
    return new AlwaysValidInputSelectionComponent() {
      final JTextField textField = new JTextField(clientSetting.value(), 20);

      @Override
      public JComponent getUiComponent() {
        return textField;
      }

      @Override
      public Map<GameSetting<?>, String> readValues() {
        final Map<GameSetting<?>, String> map = new HashMap<>();
        map.put(clientSetting, textField.getText());
        return map;
      }

      @Override
      public void reset() {
        textField.setText(clientSetting.value());
      }

      @Override
      public void resetToDefault() {
        textField.setText(clientSetting.defaultValue());
      }
    };
  }

  private abstract static class AlwaysValidInputSelectionComponent implements SelectionComponent<JComponent> {
    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public String validValueDescription() {
      return "";
    }
  }
}
