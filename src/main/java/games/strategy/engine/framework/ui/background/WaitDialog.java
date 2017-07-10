package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * A dialog that can be displayed during a long-running operation that optionally provides the user with the ability to
 * cancel the operation.
 */
public final class WaitDialog extends JDialog {
  private static final long serialVersionUID = 7433959812027467868L;

  public WaitDialog(final Component parent, final String waitMessage) {
    this(parent, waitMessage, null);
  }

  public WaitDialog(final Component parent, final String waitMessage, final Action cancelAction) {
    super(JOptionPane.getFrameForComponent(parent), "Please Wait", true);
    final WaitPanel panel = new WaitPanel(waitMessage);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panel, BorderLayout.CENTER);
    if (cancelAction != null) {
      final JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(cancelAction);
      getContentPane().add(cancelButton, BorderLayout.SOUTH);
    }

    pack();
    setLocationRelativeTo(parent);
  }
}
