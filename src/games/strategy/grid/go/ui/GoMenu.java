package games.strategy.grid.go.ui;

import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * 
 * @author veqryn
 * 
 */
public class GoMenu extends GridGameMenu<GridGameFrame>
{
	private static final long serialVersionUID = 2522152740134093334L;
	
	public GoMenu(final GridGameFrame frame)
	{
		super(frame);
	}
	
	/**
	 * @param parentMenu
	 */
	@Override
	protected void addHowToPlayHelpMenu(final JMenu parentMenu)
	{
		parentMenu.add(new AbstractAction("How to play...")
		{
			private static final long serialVersionUID = 4760939530305280882L;
			
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				// html formatted string
				final String hints = "<p><b>Go</b> (Weiqi / Igo / Baduk)"
							+ "<br />http://en.wikipedia.org/wiki/Rules_of_Go</p> "
							+ "<br /><br /><b>How To Pass Your Turn</b> "
							+ "<br />Press 'P' on your keyboard to pass your turn. "
							+ "<br />If both players pass in a row, the game ends. "
							+ "<br /><br /><b>How To Place Pieces</b> "
							+ "<br />Left Click on the intersection where you want it to go. "
							+ "<br />Pieces go on the intersections of line, not in the spaces between lines. "
							+ "<br />Pieces are connected to each other horizontally and vertically only. There are no diagonal connections. "
							+ "<br />Right Click to show potential captures in black/red and illegal moves in yellow. "
							+ "<br /><br /><b>How To End The Game</b> "
							+ "<br />After two passes, Players must decide if any groups of stones on the board are actually 'dead' stones. "
							+ "This allows players to end a game when it is obvious that some stones are alive and others are dead. "
							+ "<br />Click on any groups that are dead, and the map will change colors to show Territory control. "
							+ "<br />When finished, press 'C' to confirm, which will send your choices to your opponent.  "
							+ "He will then get to confirm it, which ends the game, or make some changes and send it back to you. "
							+ "<br />If you are in disagreement, you may press 'R' to reject the choices, and continue the game where it left off. "
							+ "<br /><br /><br /><b>The Goal of Go</b> "
							+ "<br />The goal of Go is to score points by surrounding and occupying territory. "
							+ "<br />The is done by placing pieces on the board in such a way as to claim more of the board for yourself, and/or capture enemy pieces. "
							+ "<br /><br /><b>Simple Rules</b> "
							+ "<br />1. The board starts empty. "
							+ "<br />2. Black moves first, then players alternate. "
							+ "<br />3. A move consists of placing one piece (stone) on the board at any empty intersection. "
							+ "<br />4. A player may pass his turn at any time. "
							+ "<br />5. A piece or solidly connected group of pieces (chain) of one color is captured and removed from the board when all the intersections directly "
							+ "adjacent to it are occupied by the enemy. (Capture of the enemy takes precedence over self-capture.) "
							+ "<br />6. No piece may be played so as to recreate a former board position. "
							+ "<br />7. Two consecutive passes ends the game. "
							+ "<br />8. A player's territory consists of all the points the player has either occupied or surrounded. "
							+ "<br />9. The player with more territory wins. "
							+ "<br /><br /><b>Capturing</b> "
							+ "<br />In order to capture a single piece, that piece must be completely surrounded. This means the enemy must place a piece of their own north, south, east, and west of that piece. "
							+ "<br />If the piece is by the side of the board, then it only has to be surrounded on 3 sides to be captured. If the piece is in the corner, then it only needs to be surrounded on 2 sides to be captured. "
							+ "<br />A Chain of pieces is any group of pieces of the same color where each pieces is connected to another by vertical or horizontal connections. "
							+ "<br />In order to capture a Chain of pieces, it must be completely surrounded AND it must have no internal spaces as well. "
							+ "<br />So, for example, if you had 8 pieces forming a circle, with a empty intersection in the middle, then in order to capture that chain you would need 13 pieces: 12 to surround the circle, and 1 pieces to put in the middle. "
							+ "If we put a piece in the middle of the circle, it would be immediately captured (suicide) because the middle intersection is already surrounded. "
							+ "However if we surround the entire circle first, then put the 13th piece in the middle, it would First capture the circle pieces, then the middle piece would no longer be surrounded and so would not die. "
							+ "<br />The term 'liberty' means, In a given position, a liberty of a piece or chain is an empty intersection adjacent to that piece/chain or adjacent to a piece which is connected to that piece. "
							+ "<br />The above example of 8 pieces in a circle has 13 liberties in total, of which the middle liberty is completely surrounded. "
							+ "<br />A Chain of pieces that has 2 or more liberties, each completely surrounded, can not be captured, ever. "
							+ "<br /><br /><b>Ban on Repetition (Ko)</b>"
							+ "<br />No piece may be played that would recreate a previous game state. "
							+ "<br />As an example, consider black having surrounded a white piece on 3 sides. The spot on the 4th side is completely surrounded by white. "
							+ "<br />Black may play in that 4th spot, thereby capturing the white piece. Now the situation is completely reversed:  "
							+ "white surrounds that spot on 3 sides, while black surrounds that spot on 4 sides. If white places a piece on the 4th side of that (in the spot of its previously captured white piece), "
							+ "it would capture the black piece.  However, this is illegal because it would recreate the exact board position at the start of this example. If this was not disallowed, the players would remain in this loop forever. "
							+ "Instead, white must play somewhere else first. They may later be able to come back and capture that black piece, if it is still possible by then. "
							+ "<br /><br /><b>Komidashi (bonus for second player)</b>"
							+ "<br />Because there is an advantage to playing first, whoever plays second receives bonus points. "
							+ "<br />Normally on a 19x19 board with evenly matched players, White commonly receives 5, 6, or 7 points, which is called komi. "
							+ "<br />Because White wins in a tie, this often written as 5.5, 6.5, or 7.5 respectively. The Japanese Go Association uses 6.5 as komi. "
							+ "<br />Players my 'bid' using komi. (I am willing to play black against xx komi. You counter with x+1 komi. I counter with x+2. etc etc.) "
							+ "<br />If playing on a smaller board, such as 9x9 or 13x13, the komi is normally smaller as well. "
							+ "<br /><br /><b>End of Game</b>"
							+ "<br />The game ends after both players consecutively pass. "
							+ "<br />Empty Intersections that are surrounded by your pieces are part of your 'territory'. "
							+ "<br />Your 'territory' plus all the intersections that contain a piece you own, are part of your 'area'. (This can be thought of as your territory + the number of pieces you have on the board.) "
							+ "<br />Your total score is the number of intersections in your area (+ any komi if you are white). "
							+ "<br />It is possible that an intersection belongs to noone. It is not possible that an intersection belongs to both players. "
							+ "<br />The player with the higher score wins. If there is a tie, White wins (because Black played first). "
							+ "<br />Players may choose to end the game early when it is obvious which groups of pieces will die, and which will live, and what parts of the board are owned by who. "
							+ "<br />If there is any disagreement on the above, then the game will simply keep going until those disagreements are resolved by actual moves. "
							+ "<br /><br /><b>Handicaps</b>"
							+ "<br />When two players are of uneven skill, the better player plays White. "
							+ "<br />In addition, Komi is set to zero, and Black is often given a number of 'free' moves before the game begins, normally 1 - 9 moves. "
							+ "<br /><br /><br /><b>Rules Variants</b>"
							+ "<br />Due to the long history of Go, there are two major variants. There is no effective difference between these two variants, except in extremely close games (games that would be decided by 1 or 2 points)."
							+ "<br />Chinese Go"
							+ "<br />Chinese Go is the most simple in rules, and is basically what is described above, and how TripleA aims to work. "
							+ "<br />Score is determined by total area, and captured pieces do not count. "
							+ "There is no penalty for playing within one's territory, and any disagreements at end of game can be resolved by playing on. "
							+ "<br /><br />Japanese Go"
							+ "<br />Japanese Go is far more complex in how it scores, although this has little effect on how a game is played. "
							+ "<br />Score is determined by total territory + captured pieces. This means that intersections under your pieces do not count towards your score. "
							+ "<br />This means that playing within your own territory hurts your score. Because of this, there are complex rules on what groups count as dead pieces. "
							+ "<br />In addition, intersections in 'seki' are considered neutral. Seki refers to territories that give life to more than one chain group, and are requird for those groups to live. "
							+ "<br />";
				final JEditorPane editorPane = new JEditorPane();
				editorPane.setEditable(false);
				editorPane.setContentType("text/html");
				editorPane.setText(hints);
				editorPane.setPreferredSize(new Dimension(550, 380));
				editorPane.setCaretPosition(0);
				final JScrollPane scroll = new JScrollPane(editorPane);
				JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
}
