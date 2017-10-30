package manifold.api.type;

import manifold.api.host.IModule;
import manifold.api.host.ITypeLoader;

public class TypeName implements Comparable
{
  public final String name;
  public final Kind kind;
  public final Visibility visibility;
  public final ITypeLoader loader;

  public TypeName( String name, ITypeLoader loader, Kind kind, Visibility visibility )
  {
    this.name = name;
    this.loader = loader;
    this.kind = kind;
    this.visibility = visibility;
  }

  @Override
  public int compareTo( Object o )
  {
    return -(kind.ordinal() - ((TypeName)o).kind.ordinal());
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    TypeName typeName = (TypeName)o;

    if( kind != typeName.kind )
    {
      return false;
    }
    if( loader != null ? !loader.equals( typeName.loader ) : typeName.loader != null )
    {
      return false;
    }
    if( name != null ? !name.equals( typeName.name ) : typeName.name != null )
    {
      return false;
    }
    if( visibility != typeName.visibility )
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (kind != null ? kind.hashCode() : 0);
    result = 31 * result + (visibility != null ? visibility.hashCode() : 0);
    result = 31 * result + (loader != null ? loader.hashCode() : 0);
    return result;
  }

  public IModule getModule()
  {
    return loader.getModule();
  }

  public static enum Kind
  {
    TYPE,
    NAMESPACE
  }

  public static enum Visibility
  {
    PUBLIC,
    PROTECTED,
    PRIVATE,
  }

  @Override
  public String toString()
  {
    return kind + " " + name + ": " + visibility;
  }
}
