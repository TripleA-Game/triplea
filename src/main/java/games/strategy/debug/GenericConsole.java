package games.strategy.debug;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.PrintStream;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import games.strategy.ui.SwingAction;

/**
 * Superclass for all debug console windows.
 */
public abstract class GenericConsole extends JFrame {
  private static final long serialVersionUID = 5754914217052820386L;

  private final JTextArea textArea = new JTextArea(20, 50);
  private final Action threadDiagnoseAction =
      SwingAction.of("Enumerate Threads", e -> System.out.println(DebugUtils.getThreadDumps()));

  protected GenericConsole(final String title) {
    super(title);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
    final JToolBar actions = new JToolBar(SwingConstants.HORIZONTAL);
    getContentPane().add(actions, BorderLayout.SOUTH);
    actions.setFloatable(false);
    actions.add(threadDiagnoseAction);
    actions.add(SwingAction.of("Memory", e -> append(DebugUtils.getMemory())));
    actions.add(SwingAction.of("Properties", e -> append(DebugUtils.getProperties())));
    actions.add(SwingAction.of("Copy to clipboard", e -> {
      final String text = textArea.getText();
      final StringSelection select = new StringSelection(text);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
    }));
    actions.add(SwingAction.of("Clear", e -> textArea.setText("")));
    SwingUtilities.invokeLater(() -> pack());
  }

  public abstract GenericConsole getConsoleInstance();

  public void append(final String s) {
    textArea.append(s);
  }

  public void dumpStacks() {
    threadDiagnoseAction.actionPerformed(null);
  }

  /**
   * Displays standard error to the console.
   */
  protected void displayStandardError() {
    final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.err);
    final ThreadReader reader = new ThreadReader(out, textArea, true, getConsoleInstance());
    final Thread thread = new Thread(reader, "Console std err reader");
    thread.setDaemon(true);
    thread.start();
    final PrintStream print = new PrintStream(out);
    System.setErr(print);
  }

  protected void displayStandardOutput() {
    final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.out);
    final ThreadReader reader = new ThreadReader(out, textArea, false, getConsoleInstance());
    final Thread thread = new Thread(reader, "Console std out reader");
    thread.setDaemon(true);
    thread.start();
    final PrintStream print = new PrintStream(out);
    System.setOut(print);
  }
}
