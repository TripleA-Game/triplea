package games.strategy.engine.data.properties;

import java.io.Serializable;
import java.util.Objects;

import javax.swing.JComponent;

/**
 * Superclass for all implementations of {@link IEditableProperty}.
 */
public abstract class AEditableProperty implements IEditableProperty, Serializable, Comparable<AEditableProperty> {
  private static final long serialVersionUID = -5005729898242568847L;

  private final String name;
  private final String description;

  public AEditableProperty(final String name, final String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public int getRowsNeeded() {
    return 1;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public JComponent getViewComponent() {
    final JComponent component = getEditorComponent();
    component.setEnabled(false);
    return component;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public boolean equals(final Object other) {
    return other instanceof AEditableProperty && ((AEditableProperty) other).name.equals(name);
  }

  @Override
  public int compareTo(final AEditableProperty other) {
    return name.compareTo(other.getName());
  }

  @Override
  public String toString() {
    return getName() + "=" + getValue().toString();
  }
}
