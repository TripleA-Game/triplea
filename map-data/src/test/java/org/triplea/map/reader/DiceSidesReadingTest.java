package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Game;

class DiceSidesReadingTest {
  @Test
  void readInfoTag() {
    final Game game = parseMapXml("dice-sides.xml");

    assertThat(game.getDiceSides().getValue(), is("20"));
  }
}
