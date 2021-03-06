package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.Version;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link Version} class.
 */
@Immutable
public final class VersionProxy implements Proxy {
  private static final long serialVersionUID = 6092507250760560736L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(Version.class, VersionProxy::new);

  private final int major;
  private final int minor;
  private final int point;
  private final int micro;

  public VersionProxy(final Version version) {
    checkNotNull(version);

    major = version.getMajor();
    minor = version.getMinor();
    point = version.getPoint();
    micro = version.getMicro();
  }

  @Override
  public Object readResolve() {
    return new Version(major, minor, point, micro);
  }
}
