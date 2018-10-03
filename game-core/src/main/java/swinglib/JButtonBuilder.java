package swinglib;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Component;
import java.awt.Font;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.UIManager;

import com.google.common.base.Strings;

/**
 * Example usage:.
 * <code><pre>
 *   JButton button = JButtonBuilder.builder()
 *     .text("button text")
 *     .actionListener(() -> handleClickAction())
 *     .build();
 * </pre></code>
 */
public class JButtonBuilder {

  private String title;
  private String toolTip;
  private String componentName;
  private Function<Component, Runnable> clickAction;
  private boolean enabled = true;
  private boolean selected = false;
  private int biggerFont = 0;

  private JButtonBuilder() {}

  public static JButtonBuilder builder() {
    return new JButtonBuilder();
  }

  /**
   * Constructs a Swing JButton using current builder values.
   * Values that must be set: title, actionlistener
   */
  public JButton build() {
    checkNotNull(title);

    final JButton button = new JButton(title);
    Optional.ofNullable(clickAction)
        .ifPresent(listener -> button.addActionListener(e -> clickAction.apply(button).run()));
    Optional.ofNullable(componentName).ifPresent(button::setName);
    Optional.ofNullable(toolTip).ifPresent(button::setToolTipText);

    button.setEnabled(enabled);
    button.setSelected(selected);

    if (biggerFont > 0) {
      button.setFont(
          new Font(
              button.getFont().getName(),
              button.getFont().getStyle(),
              button.getFont().getSize() + biggerFont));
    }

    return button;
  }


  /** required - The text that will be on the button. */
  public JButtonBuilder title(final String title) {
    checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
    this.title = title;
    return this;
  }

  /**
   * Sets the name of the component used internally by swing, can be used later for programmatic lookup of
   * components (notably useful in test context).
   */
  public JButtonBuilder componentName(final String componentName) {
    this.componentName = checkNotNull(componentName);
    return this;
  }

  /**
   * Sets the button title to the system's OK button text.
   */
  public JButtonBuilder okTitle() {
    return title(UIManager.getString("OptionPane.okButtonText"));
  }

  /**
   * Sets the button title to the system's Cancel button text.
   */
  public JButtonBuilder cancelTitle() {
    return title(UIManager.getString("OptionPane.cancelButtonText"));
  }

  /**
   * Toggles the button as 'selected', which gives keyboard focus to the button. By default button is not selected.
   */
  public JButtonBuilder selected(final boolean value) {
    selected = value;
    return this;
  }

  /**
   * Increases button text size by a default amount.
   */
  public JButtonBuilder biggerFont() {
    return biggerFont(4);
  }

  /**
   * Increases button text size.
   *
   * @param plusAmount Text increase amount, typically somewhere around +2 to +4
   */
  public JButtonBuilder biggerFont(final int plusAmount) {
    checkArgument(plusAmount > 0);
    biggerFont = plusAmount;
    return this;
  }


  /**
   * Sets the text shown when hovering on the button.
   */
  public JButtonBuilder toolTip(final String toolTip) {
    checkArgument(!Strings.nullToEmpty(toolTip).trim().isEmpty());
    this.toolTip = toolTip;
    return this;
  }

  /**
   * Sets the event that occurs when the button is clicked.
   * SIDE EFFECT: Enables the button
   */
  public JButtonBuilder actionListener(final Runnable actionListener) {
    checkNotNull(actionListener);
    return actionListener(c -> actionListener);
  }


  /**
   * Sets up an action listener that is passed an instance of the button it is attached to.
   * For example: many components want a 'parent' so that the new component can be located above
   * the 'parent' component on screen. We could easily have a button that opens a window
   * that should be located above the button. Since we do not have a button reference yet while
   * constructing the button, this method can be used in that situation.
   * 
   * @param clickAction The action listener to invoke when the button is clicked, this listener will be evaluated
   *        and attached to the button when the button is constructed (ie: {@code #build}.
   */
  public JButtonBuilder actionListener(final Function<Component, Runnable> clickAction) {
    this.clickAction = checkNotNull(clickAction);
    return this;
  }

  /**
   * Whether the button can be clicked on or not.
   */
  public JButtonBuilder disabled() {
    enabled = false;
    return this;
  }
}
