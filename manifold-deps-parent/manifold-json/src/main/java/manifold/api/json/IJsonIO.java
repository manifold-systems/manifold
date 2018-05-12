package manifold.api.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.script.Bindings;
import manifold.api.templ.DisableStringLiteralTemplates;
import manifold.internal.host.ManifoldHost;

/**
 */
@DisableStringLiteralTemplates
public interface IJsonIO
{
  String TYPE = "$construct_type";

  static <E extends IJsonIO> E read( Bindings bindings )
  {
    return read( null, bindings );
  }

  static <E extends IJsonIO> E read( String tag, Bindings bindings )
  {
    if( tag != null && !tag.isEmpty() )
    {
      bindings = (Bindings)bindings.get( tag );
      if( bindings == null )
      {
        return null;
      }
    }

    String fqn = (String)bindings.get( TYPE );
    try
    {
      //noinspection unchecked
      E obj = (E)Class.forName( fqn ).newInstance();
      obj.load( bindings );
      return obj;
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  static <E extends IJsonIO> void write( E obj, Bindings bindings )
  {
    write( null, obj, bindings );
  }

  static <E extends IJsonIO> void write( String tag, E obj, Bindings bindings )
  {
    if( obj == null )
    {
      if( tag != null && !tag.isEmpty() )
      {
        bindings.put( tag, obj );
      }
    }
    else
    {
      if( tag != null && !tag.isEmpty() )
      {
        Bindings objBindings = ManifoldHost.createBindings();
        obj.save( objBindings );
        bindings.put( tag, objBindings );
      }
      else
      {
        obj.save( bindings );
      }
    }
  }

  static <E extends IJsonIO> List<E> readList( Bindings bindings )
  {
    return readList( null, bindings );
  }

  static <E extends IJsonIO> List<E> readList( String tag, Bindings bindings )
  {
    //noinspection unchecked
    List<Bindings> list = (List<Bindings>)bindings.get( tag );
    if( list == null )
    {
      return Collections.emptyList();
    }

    List<E> objs = list.isEmpty() ? Collections.emptyList() : new ArrayList<>();
    for( Bindings elem : list )
    {
      objs.add( read( elem ) );
    }
    return objs;
  }

  static <E extends IJsonIO> void writeList( String tag, List<E> list, Bindings bindings )
  {
    if( list == null )
    {
      bindings.put( tag, null );
    }
    else if( list.isEmpty() )
    {
      bindings.put( tag, Collections.emptyList() );
    }
    else
    {
      List<Bindings> blist = new ArrayList<>();
      for( E e : list )
      {
        Bindings b = ManifoldHost.createBindings();
        e.save( b );
        blist.add( b );
      }
      bindings.put( tag, blist );
    }
  }

  /**
   * Implement this method to control loading from the Json bindings.  The
   * default behavior simply loads values corresponding with your class's
   * field values.
   * <p>
   * The bindings is just a simple map with name value pairs, which usually
   * maps directly to your class's fields.  But anything goes; you can read
   * and write anything from/to the Bindings.
   * <p>
   * Use #read() etc. to read proper Json data.
   */
  default void load( Bindings bindings )
  {
    Class cls = getClass();
    loadFields( this, cls, bindings );
  }

  /**
   * Implement this method to control saving to a Json bindings.  The default
   * behavior simply saves all your class's non-transient instance fields.
   * <p>
   * Basically, the bindings is a simple map where you write name/value pairs
   * representing your class's format.  Typically you save just your field
   * values, but you can save anything you like.
   * <p>
   * Use #write(String, Object, Bindings) etc. to write proper Json data.
   */
  default void save( Bindings bindings )
  {
    Class cls = getClass();
    bindings.put( TYPE, cls.getName() );
    saveFields( this, cls, bindings );
  }

  static void loadFields( IJsonIO obj, Class cls, Bindings bindings )
  {
    Field[] fields = cls.getDeclaredFields();
    for( Field f : fields )
    {
      if( f.isSynthetic() || Modifier.isStatic( f.getModifiers() ) || Modifier.isTransient( f.getModifiers() ) )
      {
        continue;
      }
      f.setAccessible( true );
      try
      {
        Object value = bindings.get( f.getName() );
        if( value instanceof Bindings )
        {
          value = read( (Bindings)value );
        }
        else if( value instanceof List && ((List)value).size() > 0 && ((List)value).get( 0 ) instanceof Bindings )
        {
          value = readList( bindings );
        }
        else if( f.getType().isEnum() && value instanceof String )
        {
          value = Enum.valueOf( (Class)f.getType(), (String)value );
        }
        else if( f.getType() == Boolean.class || f.getType() == boolean.class )
        {
          value = (Integer)value != 0;
        }
        else if( !isSimpleType( f.getType() ) )
        {
          throw new UnsupportedOperationException( "Unsupported Json type: ${f.getType()}" );
        }
        f.set( obj, value );
      }
      catch( IllegalAccessException e )
      {
        throw new RuntimeException( e );
      }
    }
    Class superclass = cls.getSuperclass();
    if( superclass != null )
    {
      loadFields( obj, superclass, bindings );
    }
  }

  static void saveFields( IJsonIO obj, Class cls, Bindings bindings )
  {
    Class superclass = cls.getSuperclass();
    if( superclass != null )
    {
      saveFields( obj, superclass, bindings );
    }

    Field[] fields = cls.getDeclaredFields();
    for( Field f : fields )
    {
      if( f.isSynthetic() || Modifier.isStatic( f.getModifiers() ) || Modifier.isTransient( f.getModifiers() ) )
      {
        continue;
      }
      f.setAccessible( true );
      try
      {
        Object value = f.get( obj );
        if( isSimpleType( value ) )
        {
          bindings.put( f.getName(), value );
        }
        else if( value instanceof Enum )
        {
          bindings.put( f.getName(), ((Enum)value).name() );
        }
        else if( value instanceof Boolean )
        {
          bindings.put( f.getName(), (Boolean)value ? 1 : 0 );
        }
        else if( value instanceof List )
        {
          if( !((List)value).isEmpty() )
          {
            if( isSimpleType( ((List)value).get( 0 ) ) )
            {
              bindings.put( f.getName(), value );
            }
            else
            {
              writeList( f.getName(), (List)value, bindings );
            }
          }
          else
          {
            bindings.put( f.getName(), value );
          }
        }
        else if( value instanceof IJsonIO )
        {
          write( f.getName(), (IJsonIO)value, bindings );
        }
        else
        {
          throw new UnsupportedOperationException( "Type: ${value.getClass()} does not implement " + IJsonIO.class.getName() );
        }
      }
      catch( IllegalAccessException e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  static boolean isSimpleType( Object value )
  {
    if( value == null )
    {
      return true;
    }
    return isSimpleType( value.getClass() );
  }

  static boolean isSimpleType( Class cls )
  {
    return cls == null ||
           cls == int.class ||
           cls == long.class ||
           cls == float.class ||
           cls == double.class ||
           cls == String.class ||
           cls == Integer.class ||
           cls == Long.class ||
           cls == Float.class ||
           cls == Double.class;
  }
}
