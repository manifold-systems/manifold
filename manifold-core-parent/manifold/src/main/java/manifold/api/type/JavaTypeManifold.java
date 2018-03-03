package manifold.api.type;

/**
 * A base class for a Java source producer that is based on a resource file of a specific extension.
 *
 * @param <M> The model you derive backing production of source code.
 */
public abstract class JavaTypeManifold<M extends IModel> extends ResourceFileTypeManifold<M>
{
  @Override
  public ISourceKind getSourceKind()
  {
    return ISourceKind.Java;
  }

  @Override
  public ContributorKind getContributorKind()
  {
    return ContributorKind.Primary;
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.JavaClass;
  }
}
