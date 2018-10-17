package manifold.api.type;

public interface ISelfCompiledFile
{
  boolean isSelfCompile();

  byte[] compile();
}
