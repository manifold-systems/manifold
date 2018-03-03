package manifold.api.host;


import manifold.api.fs.IFile;

public class RefreshRequest
{
  public final IFile file;
  public final IModule module;
  public final IModuleComponent typeLoader;
  public final RefreshKind kind;
  public final String[] types;

  public RefreshRequest( IFile file, String[] types, IModule module, IModuleComponent typeLoader, RefreshKind kind )
  {
    this.file = file;
    this.kind = kind;
    this.types = types;
    this.module = module;
    this.typeLoader = typeLoader;
  }

  public RefreshRequest( IFile file, String[] types, IModuleComponent typeLoader, RefreshKind kind )
  {
    this( file, types, getModule( typeLoader ), typeLoader, kind );
  }

  public RefreshRequest( String[] allTypes, RefreshRequest request, IModuleComponent typeLoader )
  {
    this( request.file, allTypes, typeLoader, request.kind );
  }

  private static IModule getModule( IModuleComponent typeLoader )
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
