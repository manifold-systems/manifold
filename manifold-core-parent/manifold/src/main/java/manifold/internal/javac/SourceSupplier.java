package manifold.internal.javac;

import java.util.Set;
import java.util.function.Supplier;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;

public class SourceSupplier
{
  private Supplier<String> _supplier;
  private final Set<ITypeManifold> _sps;

  /**
   * @param tms The set of type manifolds responsible for producing the source.  An
   *            empty or null set implies no type manifolds are involved.
   * @param supplier Supplier of the source code.
   */
  public SourceSupplier( Set<ITypeManifold> tms, Supplier<String> supplier )
  {
    _supplier = supplier;
    _sps = tms;
  }

  /**
   * Produce the source.
   */
  public String getSource()
  {
    return _supplier.get();
  }

  /**
   * Is this source supplier the primary or core producer of the source?  As opposed to a
   * supplementary or partial producer.
   */
  public boolean isPrimary()
  {
    return _sps == null || _sps.isEmpty() ||
           _sps.stream().anyMatch( e -> e.getContributorKind() == ContributorKind.Primary ||
                                        e.getContributorKind() == ContributorKind.Partial );
  }
}
