package swinglib;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.ui.SwingAction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Builder class for building swing text fields. Example usage:
 *
 * <pre>
 * <code>
 *   JTextField field = JTextFieldBuilder.builder()
 *      .columns(15)
 *      .text("initial text")
 *      .actionListener(textValue -> enterPressedWithTextInput(textValue)
 *      .keyTypedListener(textValue -> processKeyTypedWithText(textValue)
 *      .compositeBuilder()
 *      .withButton("buttonText", textValue -> buttonPressedAction(textValue)
 *      .build();
 * </code>
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JTextFieldBuilder {

  private String text;
  private String toolTip;
  private Integer columns;
  private Integer maxLength;
  private String componentName;

  private Consumer<JTextComponent> textEnteredAction;
  private boolean enabled = true;
  private boolean readOnly;
  private Consumer<JTextField> onFocusAction;


  /**
   * Builds the swing component.
   */
  public JTextField build() {
    final JTextField textField = new JTextField(Strings.nullToEmpty(this.text));

    Optional.ofNullable(columns)
        .ifPresent(textField::setColumns);


    Optional.ofNullable(textEnteredAction)
        .ifPresent(
            action ->
                DocumentListenerBuilder.attachDocumentListener(textField, () -> textEnteredAction.accept(textField)));

    Optional.ofNullable(maxLength)
        .map(JTextFieldLimit::new)
        .ifPresent(textField::setDocument);

    Optional.ofNullable(componentName)
        .ifPresent(textField::setName);

    Optional.ofNullable(onFocusAction)
        .ifPresent(action -> textField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            action.accept(textField);
          }
        }));

    Optional.ofNullable(text)
        .ifPresent(textField::setText);
    Optional.ofNullable(toolTip)
        .ifPresent(textField::setToolTipText);

    textField.setEnabled(enabled);
    textField.setEditable(!readOnly);
    return textField;
  }


  /**
   * Sets the initial value displayed on the text field.
   */
  public JTextFieldBuilder text(final String value) {
    Preconditions.checkNotNull(value);
    this.text = value;
    return this;
  }

  /**
   * Convenience method for setting the text value to a numeric value.
   */
  public JTextFieldBuilder text(final int value) {
    this.text = String.valueOf(value);
    return this;
  }

  public JTextFieldBuilder toolTip(final String toolTip) {
    this.toolTip = toolTip;
    return this;
  }

  /**
   * Defines the width of the text field. Value is passed directly to swing.
   * TODO: list some typical/reasonable value examples
   */
  public JTextFieldBuilder columns(final int columns) {
    Preconditions.checkArgument(columns > 0);
    this.columns = columns;
    return this;
  }

  /**
   * Sets a max of how many characters can be entered into the text field.
   *
   * @param maxChars Character limit of the text field.
   */
  public JTextFieldBuilder maxLength(final int maxChars) {
    maxLength = maxChars;
    return this;
  }

  /*
   * {@code JTextFieldLimit} is from:
   * https://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
   */
  private static class JTextFieldLimit extends PlainDocument {
    private static final long serialVersionUID = -6269113182585526764L;
    private final int limit;

    JTextFieldLimit(final int limit) {
      this.limit = limit;
    }

    @Override
    public void insertString(final int offset, final String str, final AttributeSet attr)
        throws BadLocationException {
      if (str == null) {
        return;
      }

      if ((getLength() + str.length()) <= limit) {
        super.insertString(offset, str, attr);
      }
    }
  }


  public static JTextFieldBuilder builder() {
    return new JTextFieldBuilder();
  }

  /**
   * Adds an action listener that is fired when the user presses enter after entering text into the text field.
   *
   * @param textEnteredAction Action to fire on 'enter', input value is the current value of the
   *        text field.
   */
  public JTextFieldBuilder actionListener(final Consumer<JTextComponent> textEnteredAction) {
    Preconditions.checkNotNull(textEnteredAction);
    this.textEnteredAction = textEnteredAction;
    return this;
  }

  /**
   * Adds a listener action to the text field that is fired whenever a key is typed.
   */
  public static void keyTypedListener(final Consumer<KeyEvent> listener, final JTextField... fields) {
    Preconditions.checkNotNull(listener);
    Preconditions.checkArgument(fields.length > 0);
    Preconditions.checkNotNull(fields[0]);

    Arrays.asList(fields).forEach(field -> field.addKeyListener(SwingAction.keyReleaseListener(listener)));
  }

  /**
   * A ready only text field can't be changed, but the user can still click on it and select the text.
   */
  public JTextFieldBuilder readOnly() {
    this.readOnly = true;
    return this;
  }

  /**
   * Disables a text field, can no longer be clicked on.
   */
  public JTextFieldBuilder disabled() {
    this.enabled = false;
    return this;
  }

  public JTextFieldBuilder componentName(final String name) {
    this.componentName = name;
    return this;
  }

  public JTextFieldBuilder onFocusAction(final Consumer<JTextField> onFocusAction) {
    this.onFocusAction = onFocusAction;
    return this;
  }

  public JTextFieldBuilder selectAllTextOnFocus() {
    onFocusAction(field -> {
      field.select(0, field.getText().length());
    });
    return this;
  }

}
