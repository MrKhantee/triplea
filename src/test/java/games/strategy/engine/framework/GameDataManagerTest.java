package games.strategy.engine.framework;

import static games.strategy.engine.data.Matchers.equalToGameData;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestGameDataFactory;

public class GameDataManagerTest {
  @Test
  public void testLoadStoreKeepsGameUuid() throws IOException {
    final GameData data = new GameData();
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    GameDataManager.saveGame(sink, data);
    final GameData loaded = GameDataManager.loadGame(new ByteArrayInputStream(sink.toByteArray()), null);
    assertEquals(loaded.getProperties().get(GameData.GAME_UUID), data.getProperties().get(GameData.GAME_UUID));
  }

  @Test
  public void shouldBeAbleToRoundTripGameDataInNewFormat() throws Exception {
    final GameData expected = TestGameDataFactory.newValidGameData();

    final byte[] bytes = saveGameInNewFormat(expected);
    final GameData actual = loadGameInNewFormat(bytes);

    assertThat(actual, is(equalToGameData(expected)));
  }

  private static byte[] saveGameInNewFormat(final GameData gameData) throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      GameDataManager.saveGameInNewFormat(baos, gameData, Collections.emptyMap());
      return baos.toByteArray();
    }
  }

  private static GameData loadGameInNewFormat(final byte[] bytes) throws Exception {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
      return GameDataManager.loadGameInNewFormat(bais);
    }
  }

  @Test
  public void loadGameInNewFormat_ShouldNotCloseInputStream() throws Exception {
    try (final InputStream is = spy(newInputStreamWithGameInNewFormat())) {
      GameDataManager.loadGameInNewFormat(is);

      verify(is, never()).close();
    }
  }

  private static InputStream newInputStreamWithGameInNewFormat() throws Exception {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final byte[] bytes = saveGameInNewFormat(gameData);
    return new ByteArrayInputStream(bytes);
  }

  @Test
  public void saveGameInNewFormat_ShouldNotCloseOutputStream() throws Exception {
    final OutputStream os = mock(OutputStream.class);
    final GameData gameData = TestGameDataFactory.newValidGameData();

    GameDataManager.saveGameInNewFormat(os, gameData, Collections.emptyMap());

    verify(os, never()).close();
  }
}
