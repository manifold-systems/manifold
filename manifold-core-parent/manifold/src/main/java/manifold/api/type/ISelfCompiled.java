package manifold.api.type;

public interface ISelfCompiled
{
  default boolean isSelfCompile( String fqn )
  {
    return false;
  }

  default byte[] compile( String fqn )
  {
    throw new UnsupportedOperationException();
  }
}
