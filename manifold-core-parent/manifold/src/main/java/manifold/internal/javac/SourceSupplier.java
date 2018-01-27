package manifold.internal.javac;

import java.util.Set;
import java.util.function.Supplier;
import manifold.api.type.ITypeManifold;

public class SourceSupplier
{
  private Supplier<String> _supplier;
  private final Set<ITypeManifold> _sps;

  public SourceSupplier( Set<ITypeManifold> sps, Supplier<String> supplier )
  {
    _supplier = supplier;
    _sps = sps;
  }

  public String getSource()
  {
    return _supplier.get();
  }

  public boolean isPrimary()
  {
    return _sps.stream().anyMatch( e -> e.getProducerKind() == ITypeManifold.ProducerKind.Primary ||
                                        e.getProducerKind() == ITypeManifold.ProducerKind.Partial );
  }
}
