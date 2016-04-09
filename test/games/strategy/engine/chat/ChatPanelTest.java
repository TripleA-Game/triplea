package games.strategy.engine.chat;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import junit.framework.TestCase;

public class ChatPanelTest extends TestCase { 
  public void testTrim() throws Exception {
    final StyledDocument doc = new DefaultStyledDocument();
    final StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      buffer.append("\n");
    }
    doc.insertString(0, buffer.toString(), null);
    ChatMessagePanel.trimLines(doc, 20);
    assertEquals(doc.getLength(), 10);
    ChatMessagePanel.trimLines(doc, 10);
    assertEquals(doc.getLength(), 10);
    ChatMessagePanel.trimLines(doc, 5);
    assertEquals(doc.getLength(), 5);
    ChatMessagePanel.trimLines(doc, 1);
    assertEquals(doc.getLength(), 1);
  }
}
