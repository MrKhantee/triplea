package games.strategy.engine.random;

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.VerifiedRandomNumbers;
import games.strategy.engine.vault.NotUnlockedException;
import games.strategy.engine.vault.Vault;
import games.strategy.engine.vault.VaultID;

public class RemoteRandom implements IRemoteRandom {
  private static final List<VerifiedRandomNumbers> verifiedRandomNumbers = new ArrayList<>();

  public static synchronized List<VerifiedRandomNumbers> getVerifiedRandomNumbers() {
    return new ArrayList<>(verifiedRandomNumbers);
  }

  private static synchronized void addVerifiedRandomNumber(final VerifiedRandomNumbers number) {
    verifiedRandomNumbers.add(number);
  }

  private final PlainRandomSource m_plainRandom = new PlainRandomSource();
  private final IGame m_game;
  // remembered from generate to unlock
  private VaultID m_remoteVaultID;
  private String m_annotation;
  private int m_max;
  // have we recieved a generate request, but not a unlock request
  private boolean m_waitingForUnlock;
  private int[] m_localNumbers;

  /**
   * Creates a new instance of RemoteRandom.
   */
  public RemoteRandom(final IGame game) {
    m_game = game;
  }

  @Override
  public int[] generate(final int max, final int count, final String annotation, final VaultID remoteVaultId)
      throws IllegalStateException {
    if (m_waitingForUnlock) {
      throw new IllegalStateException("Being asked to generate random numbers, but we havent finished last generation. "
          // TODO: maybe we should wait instead of crashing the game?
          + "Asked for: " + count + "x" + max + " for " + annotation);
    }
    m_waitingForUnlock = true;
    // clean up here, we know these keys arent needed anymore so release them
    // we cant do this earlier without synchronizing between the server and the client
    // but here we know they arent needed anymore
    if (m_remoteVaultID != null) {
      m_game.getVault().release(m_remoteVaultID);
    }
    m_remoteVaultID = remoteVaultId;
    m_annotation = annotation;
    m_max = max;
    m_localNumbers = m_plainRandom.getRandom(max, count, annotation);
    m_game.getVault().waitForID(remoteVaultId, 15000);
    if (!m_game.getVault().knowsAbout(remoteVaultId)) {
      throw new IllegalStateException(
          "Vault id not known, have:" + m_game.getVault().knownIds() + " looking for:" + remoteVaultId);
    }
    return m_localNumbers;
  }

  @Override
  public void verifyNumbers() throws IllegalStateException {
    final Vault vault = m_game.getVault();
    vault.waitForIdToUnlock(m_remoteVaultID, 15000);
    if (!vault.isUnlocked(m_remoteVaultID)) {
      throw new IllegalStateException("Server did not unlock random numbers, cheating is suspected");
    }
    int[] remoteNumbers;
    try {
      remoteNumbers = CryptoRandomSource.bytesToInts(vault.get(m_remoteVaultID));
    } catch (final NotUnlockedException e1) {
      e1.printStackTrace();
      throw new IllegalStateException("Could not unlock numbers, cheating suspected");
    }
    final int[] verifiedNumbers = CryptoRandomSource.xor(remoteNumbers, m_localNumbers, m_max);
    addVerifiedRandomNumber(new VerifiedRandomNumbers(m_annotation, verifiedNumbers));
    m_waitingForUnlock = false;
  }
}
