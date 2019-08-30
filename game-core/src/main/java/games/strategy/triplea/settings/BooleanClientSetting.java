package games.strategy.triplea.settings;

import org.triplea.swing.JMenuItemCheckBoxBuilder;

public final class BooleanClientSetting extends ClientSetting<Boolean>
    implements JMenuItemCheckBoxBuilder.SettingPersistence {

  BooleanClientSetting(final String name) {
    this(name, false);
  }

  BooleanClientSetting(final String name, final boolean defaultValue) {
    super(Boolean.class, name, defaultValue);
  }

  @Override
  protected String encodeValue(final Boolean value) {
    return value.toString();
  }

  @Override
  protected Boolean decodeValue(final String encodedValue) {
    return Boolean.valueOf(encodedValue);
  }

  @Override
  public void saveSetting(final boolean value) {
    setValueAndFlush(value);
  }

  @Override
  public boolean getSetting() {
    return getDefaultValue().orElse(false);
  }
}
