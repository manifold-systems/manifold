package manifold.internal.javac;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Arguments for the javac plugin.
 */
public enum Arg
{
  /**
   * Bootstrap package list.
   */
  bootstrap( "--bootstrap", false, false, null, "Bootstrap package list" )
    {
      @Override
      public void validate( String value, Map<Arg, Set<String>> map )
      {
        if( value == null || value.isEmpty() )
        {
          throw new RuntimeException(
            "Manifold plugin argument: '" + bootstrap.getName() + "' requires one or more package names" );
        }
        if( !value.startsWith( "+" ) && !value.startsWith( "-" ) )
        {
          throw new RuntimeException(
            "Manifold plugin argument: '" + bootstrap.getName() + "' must begin with a + or - indicating a whitelist or blacklist" );
        }
        if( map.containsKey( no_bootstrap ) || map.containsKey( no_bootstrap_deprecated ) )
        {
          throw new RuntimeException(
            "Manifold plugin argument: '" + bootstrap.getName() + "' must not be used with " + "'" + no_bootstrap.getName() + "'" );
        }
      }
    },
  /**
   * No bootstrap flag
   */
  no_bootstrap( "--no-bootstrap", false, true, null, "Turns off runtime bootstrapping code generation" )
    {
      @Override
      protected void validate( String arg, Map<Arg, Set<String>> map )
      {
        if( map.containsKey( bootstrap ) )
        {
          throw new RuntimeException(
            "Manifold plugin argument: '" + bootstrap.getName() + "' must not be used with " + "'" + no_bootstrap.getName() + "'" );
        }
      }
    },
  no_bootstrap_deprecated( "no-bootstrap", false, true, true, null, "Deprecated. Use --no-bootstrap" )
    {
      @Override
      protected void validate( String arg, Map<Arg, Set<String>> map )
      {
        if( map.containsKey( bootstrap ) )
        {
          throw new RuntimeException(
            "Manifold plugin argument: '" + bootstrap.getName() + "' must not be used with " + "'" + no_bootstrap.getName() + "'" );
        }
      }
    },
  /**
   * dynamic compilation flag
   */
  dynamic( "--dynamic", false, true, null, "Turns on runtime compilation mode" ),
  dynamic_deprecated( "dynamic", false, true, true, null, "Deprecated. Use --dynamic" );

  private final String _name;
  private final boolean _required;
  private final boolean _isFlag;
  private final boolean _deprecated;
  private final String _description;
  private final String _defaultValue;

  Arg( String name, boolean required, boolean isFlag, String defaultValue, String description )
  {
    this( name, required, isFlag, false, defaultValue, description );
  }

  Arg( String name, boolean required, boolean isFlag, boolean deprecated, String defaultValue, String description )
  {
    _name = name;
    _required = required;
    _isFlag = isFlag;
    _deprecated = deprecated;
    _description = description;
    _defaultValue = defaultValue;
  }

  public String getName()
  {
    return _name;
  }

  public String getDescription()
  {
    return _description;
  }

  public String getDefaultValue()
  {
    return _defaultValue;
  }

  public boolean isRequired()
  {
    return _required;
  }

  public boolean isFlag()
  {
    return _isFlag;
  }

  @SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
  public boolean isDeprecated()
  {
    return _deprecated;
  }

  public static Set<Arg> allRequired()
  {
    return Arrays.stream( values() ).filter( a -> a.isRequired() && !a.isDeprecated() ).collect( Collectors.toCollection( LinkedHashSet::new ) );
  }

  public static Set<Arg> allOptional()
  {
    return Arrays.stream( values() ).filter( a -> !a.isRequired() && !a.isDeprecated() ).collect( Collectors.toCollection( LinkedHashSet::new ) );
  }

  public static Arg byName( String name )
  {
    return Arrays.stream( values() )
      .filter( e -> e.getName().equals( name ) )
      .findFirst()
      .orElse( null );
  }

  protected void validate( String arg, Map<Arg, Set<String>> map )
  {
  }

  private static String usage()
  {
    StringBuilder sb = new StringBuilder( "--Usage--\n" );
    if( !allRequired().isEmpty() )
    {
      sb.append( "Required parameters:\n" );
      for( Arg arg : allRequired() )
      {
        sb.append( arg.getName() ).append( ": " ).append( arg.getDescription() ).append( "\n" );
      }
    }
    if( !allOptional().isEmpty() )
    {
      sb.append( "Optional parameters:\n" );
      for( Arg arg : allOptional() )
      {
        sb.append( arg.getName() ).append( ": " ).append( arg.getDescription() ).append( "\n" );
      }
    }
    sb.append( "Example:\n" )
      .append( "-Xplugin:Manifold --no-bootstrap" );
    return sb.toString();
  }

  static Map<Arg, Set<String>> processArgs( String[] args )
  {
    Map<Arg, Set<String>> map = new HashMap<>();

    if( args == null || args.length == 0 )
    {
      return map;
    }

    Arg valueArg = null;
    for( String arg : args )
    {
      if( valueArg != null )
      {
        map.computeIfAbsent( valueArg, __ -> new LinkedHashSet<>() )
          .add( arg );
        valueArg.validate( arg, map );
        valueArg = null;
      }
      else
      {
        Arg a = Arg.byName( arg );
        if( a != null )
        {
          // allow args to be used multiple times, values are stored in a set
          map.putIfAbsent( a, null );
          if( !a.isFlag() )
          {
            valueArg = a;
          }
          else
          {
            a.validate( arg, map );
          }
        }
        else
        {
          throw new RuntimeException( "Error:  Invalid Manifold plugin argument: " + arg + "\n" + usage() );
        }
      }
    }
    if( valueArg != null )
    {
      throw new RuntimeException( "Error:  Expecting value for Manifold plugin " + args[args.length - 1] );
    }
    List<String> missingArgs = Arg.allRequired().stream()
      .filter( arg -> !map.containsKey( arg ) )
      .map( arg -> "\n  " + arg.getName() + "  '" + arg.getDescription() + "'" )
      .collect( Collectors.toList() );
    if( !missingArgs.isEmpty() )
    {
      String errorMsg = missingArgs.stream().reduce( "Error:  Missing required Manifold plugin argument[s]:", String::concat );
      throw new RuntimeException( errorMsg );
    }
    assignDefaults( map );
    return map;
  }

  private static void assignDefaults( Map<Arg, Set<String>> map )
  {
    for( Arg arg : Arg.values() )
    {
      if( !map.containsKey( arg ) && !arg.isRequired() )
      {
        String defaultValue = arg.getDefaultValue();
        if( defaultValue != null )
        {
          map.put( arg, Collections.singleton( defaultValue ) );
        }
      }
    }
  }
}
