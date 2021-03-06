package games.strategy.engine.data;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;

public class ChangeTripleATest {
  private GameData gameData;
  private Territory can;

  @Before
  public void setUp() throws Exception {
    ClientContext.gameEnginePropertyReader();
    gameData = TestMapGameData.BIG_WORLD_1942.getGameData();
    can = gameData.getMap().getTerritory("Western Canada");
    assertEquals(can.getUnits().getUnitCount(), 2);
  }

  private Change serialize(final Change change) throws Exception {
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    final ObjectOutputStream output = new GameObjectOutputStream(sink);
    output.writeObject(change);
    output.flush();
    // System.out.println("bytes:" + sink.toByteArray().length);
    final InputStream source = new ByteArrayInputStream(sink.toByteArray());
    final ObjectInputStream input =
        new GameObjectInputStream(new GameObjectStreamFactory(gameData), source);
    final Change newChange = (Change) input.readObject();
    input.close();
    output.close();
    return newChange;
  }

  @Test
  public void testUnitsAddTerritory() {
    // add some units
    final Change change =
        ChangeFactory.addUnits(can, GameDataTestUtil.infantry(gameData).create(10, null));
    gameData.performChange(change);
    assertEquals(can.getUnits().getUnitCount(), 12);
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(can.getUnits().getUnitCount(), 2);
  }

  @Test
  public void testUnitsRemoveTerritory() {
    // remove some units
    final Collection<Unit> units = can.getUnits().getUnits(GameDataTestUtil.infantry(gameData), 1);
    final Change change = ChangeFactory.removeUnits(can, units);
    gameData.performChange(change);
    assertEquals(can.getUnits().getUnitCount(), 1);
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(can.getUnits().getUnitCount(), 2);
  }

  @Test
  public void testSerializeUnitsRemoteTerritory() throws Exception {
    // remove some units
    final Collection<Unit> units = can.getUnits().getUnits(GameDataTestUtil.infantry(gameData), 1);
    Change change = ChangeFactory.removeUnits(can, units);
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(can.getUnits().getUnitCount(), 1);
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(can.getUnits().getUnitCount(), 2);
  }
}
