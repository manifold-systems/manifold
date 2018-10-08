package manifold.api.host;


import manifold.api.fs.IFile;

public class RefreshRequest
{
  public final IFile file;
  public final IModule module;
  public final RefreshKind kind;
  public final String[] types;

  public RefreshRequest( IFile file, String[] types, IModule module, RefreshKind kind )
  {
    this.file = file;
    this.kind = kind;
    this.types = types;
    this.module = module;
  }

  public RefreshRequest( String[] allTypes, RefreshRequest request, IModule module )
  {
    this( request.file, allTypes, module, request.kind );
  }

  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder( kind + " with " );
    for( String type : types )
    {
      s.append( type ).append( ", " );
    }
    s.append( "module: " ).append( module );
    return s.toString();
  }
}
