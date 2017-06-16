package manifold.api.sourceprod;

/**
 * A base class for a Java source producer that is based on a resource file of a specific extension.
 *
 * @param <M> The model you derive backing production of source code.
 */
public abstract class JavaSourceProducer<M extends IModel> extends ResourceFileSourceProducer<M>
{
  @Override
  public SourceKind getSourceKind()
  {
    return SourceKind.Java;
  }

  @Override
  public ProducerKind getProducerKind()
  {
    return ProducerKind.Primary;
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.JavaClass;
  }
}
