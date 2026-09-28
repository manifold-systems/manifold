package manifold.ext.parts;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class LinkFieldData
{
  private static final Context.Key<LinkFieldData> KEY = new Context.Key<>();

  private final Map<Symbol.VarSymbol, Set<Type>> _providedClasses = new HashMap<>();


  public static LinkFieldData instance( Context ctx )
  {
    LinkFieldData fd = ctx.get( KEY );
    if( fd == null )
    {
      fd = new LinkFieldData();
      ctx.put( KEY, fd );
    }
    return fd;
  }

  public void putProvided( Symbol.VarSymbol field, Type provided )
  {
    _providedClasses.computeIfAbsent( field, __ -> new LinkedHashSet<>() )
      .add( provided );
  }
  public void putProvided( Symbol.VarSymbol field, Set<Type> provided )
  {
    _providedClasses.put( field, provided );
  }

  public Set<Type> getProvided( Symbol.VarSymbol field )
  {
    return _providedClasses.get( field );
  }
}