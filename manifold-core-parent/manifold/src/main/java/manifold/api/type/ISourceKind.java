package manifold.api.type;

/**
 * Indicates the source language a {@link ITypeManifold} uses in projected types.
 * <p/>
 * The {@link #Java}, {@link #JavaScript}, and {@link #None} constants are all
 * handled directly when using Manifold with Java. Support for other JVM
 * languages must be provided via third parties implementing {@link manifold.api.host.IManifoldHost}.
 */
public interface ISourceKind
{
  /** Java source */
  ISourceKind Java = new ISourceKind() {};

  /** JavaScript source */
  ISourceKind JavaScript = new ISourceKind() {};

  /** The {@ITypeManifold} does not contribute source */
  ISourceKind None = new ISourceKind() {};
}
