package manifold.api.host;


import manifold.api.fs.IFile;
import manifold.internal.host.ManifoldHost;

public class RefreshRequest
{
  public final IFile file;
  public final IModule module;
  public final ITypeLoader typeLoader;
  public final RefreshKind kind;
  public final String[] types;

  public RefreshRequest( IFile file, String[] types, IModule module, ITypeLoader typeLoader, RefreshKind kind )
  {
    this.file = file;
    this.kind = kind;
    this.types = types;
    this.module = module;
    this.typeLoader = typeLoader;
  }

  public RefreshRequest( IFile file, String[] types, ITypeLoader typeLoader, RefreshKind kind )
  {
    this( file, types, getModule( typeLoader ), typeLoader, kind );
  }

  public RefreshRequest( String[] allTypes, RefreshRequest request, ITypeLoader typeLoader )
  {
    this( request.file, allTypes, typeLoader, request.kind );
  }

  private static IModule getModule( ITypeLoader typeLoader )
  {
    if( typeLoader == null )
    {
      throw new RuntimeException( "A refresh request must have a valid typeloader" );
    }
    return typeLoader.getModule();
  }

  @Override
  public String toString()
  {
    String s = kind + " of ";
    for( String type : types )
    {
      s += type + ", ";
    }
    s += "from " + (typeLoader != null ? typeLoader : module);
    return s;
  }
}
