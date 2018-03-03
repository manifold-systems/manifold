package manifold.api.type;

/**
 * Indicates the involvement of a {@link ITypeManifold} toward the completeness of projected source
 */
public enum ContributorKind
{
  /**
   * Contributes complete valid source and does not depend on contributions from other manifolds,
   * however other manifolds may augment the primary source.
   */
  Primary,

  /**
   * Cooperates with other manifolds to collectively provide complete valid source.
   */
  Partial,

  /**
   * Supplements the source produced from a Primary manifold or set of Partial manifolds. Any
   * number of Supplemental manifolds may contribute toward a single source.
   */
  Supplemental,

  /**
   * Does not contribute source.
   */
  None
}
