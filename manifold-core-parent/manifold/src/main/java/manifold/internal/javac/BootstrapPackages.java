package manifold.internal.javac;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import javax.tools.Diagnostic;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import static manifold.internal.javac.BootstrapPackages.Kind.*;

public class BootstrapPackages
{
  enum Kind { Whitelist, Blacklist, All }

  private final Kind _kind;
  private final Set<String> _packages;

  public BootstrapPackages( Set<String> values, JavacProcessingEnvironment jpe )
  {
    if( values == null || values.isEmpty() )
    {
      // no packages indicate all packages
      _kind = All;
      _packages = null;
      return;
    }

    char kindChar = values.iterator().next().charAt( 0 );
    _kind = kindChar == '+' ? Whitelist : Blacklist;
    if( values.stream().anyMatch( e -> e.charAt( 0 ) != kindChar ) )
    {
      jpe.getMessager().printMessage( Diagnostic.Kind.ERROR,
        "Manifold plugin --bootstrap arguments must be consistently + or -, not both" );
    }

    _packages = parsePackages( values );
  }

  private Set<String> parsePackages( Set<String> values )
  {
    final Set<String> packages = new LinkedHashSet<>();
    for( String value: values )
    {
      for( StringTokenizer tokenizer = new StringTokenizer( value.substring( 1 ), ";, " ); tokenizer.hasMoreTokens(); )
      {
        String pkg = tokenizer.nextToken();
        if( !pkg.isEmpty() )
        {
          packages.add( pkg );
        }
      }
    }
    return packages;
  }

  public boolean contains( String pkg )
  {
    switch( _kind )
    {
      case Whitelist:
        return _packages.stream().anyMatch( e -> pkg.equals( e ) || pkg.startsWith( e + '.' ) );
      case Blacklist:
        return _packages.stream().noneMatch( e -> pkg.equals( e ) || pkg.startsWith( e + '.' ) );
      case All:
        return true;
    }
    throw new IllegalStateException( "Unknown kind: " + _kind );
  }
}
