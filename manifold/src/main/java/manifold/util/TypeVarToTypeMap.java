package manifold.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public class TypeVarToTypeMap
{
  public static final TypeVarToTypeMap EMPTY_MAP = new TypeVarToTypeMap( Collections.emptyMap() );

  private Map<TypeVariable, Pair<Type, Boolean>> _map;
  private Set<TypeVariable> _typesInferredFromCovariance;
  private boolean _bStructural;
  private boolean _bReparsing;

  public TypeVarToTypeMap()
  {
    _map = new LinkedHashMap<>( 2 );
    _typesInferredFromCovariance = new HashSet<>( 2 );
  }

  private TypeVarToTypeMap( Map<TypeVariable, Pair<Type, Boolean>> emptyMap )
  {
    _map = emptyMap;
    _typesInferredFromCovariance = new HashSet<>( 2 );
  }

  public TypeVarToTypeMap( TypeVarToTypeMap from )
  {
    this();
    _map.putAll( from._map );
    _typesInferredFromCovariance.addAll( from._typesInferredFromCovariance );
    _bStructural = from._bStructural;
  }

  public Type get( TypeVariable tvType )
  {
    Pair<Type, Boolean> pair = _map.get( tvType );
    return pair != null ? pair.getFirst() : null;
  }
  public Pair<Type, Boolean> getPair( TypeVariable tvType )
  {
    return _map.get( tvType );
  }

  public <E> Type getByMatcher( E tv, ITypeVarMatcher<E> matcher )
  {
    for( TypeVariable key : _map.keySet() )
    {
      if( matcher.matches( tv, key ) )
      {
        return get( key );
      }
    }
    return null;
  }

  public Type getByString( String tv )
  {
    for( TypeVariable key: _map.keySet() )
    {
      if( tv.equals( key.getName() ) || tv.equals( key.getName() ) )
      {
        return key;
      }
    }
    return null;
  }

  public boolean containsKey( TypeVariable tvType )
  {
    return _map.containsKey( tvType );
  }

  public Type put( TypeVariable tvType, Type type )
  {
    return put( tvType, type, false );
  }
  public Type put( TypeVariable tvType, Type type, boolean bReverse )
  {
    Type existing = remove( tvType );
    _map.put( tvType, type == null ? null : new Pair<>( type, bReverse ) );
    return existing;
  }

  public void putAll( TypeVarToTypeMap from )
  {
    for( TypeVariable x : from._map.keySet() )
    {
      put( x, from.get( x ) );
    }
  }

  public void putAllAndInferred( TypeVarToTypeMap from )
  {
    for( TypeVariable x : from._map.keySet() )
    {
      put( x, from.get( x ) );
    }
    _typesInferredFromCovariance.addAll( from._typesInferredFromCovariance );
  }

  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  public int size()
  {
    return _map.size();
  }

  public Set<TypeVariable> keySet()
  {
    return _map.keySet();
  }

  public Set<Map.Entry<TypeVariable,Pair<Type, Boolean>>> entrySet()
  {
    return _map.entrySet();
  }

  public Type remove( TypeVariable tvType )
  {
    Pair<Type, Boolean> pair = _map.remove( tvType );
    return pair != null ? pair.getFirst() : null;
  }

  public Collection<Pair<Type, Boolean>> values()
  {
    return _map.values();
  }

  public boolean isStructural() {
    return _bStructural;
  }
  public void setStructural( boolean bStructural ) {
    _bStructural = bStructural;
  }

  public boolean isInferredForCovariance( TypeVariable tv ) {
    return !isStructural() || _typesInferredFromCovariance.contains( tv );
  }
  public void setInferredForCovariance( TypeVariable tv ) {
    _typesInferredFromCovariance.add( tv );
  }

  public interface ITypeVarMatcher<E> {
    boolean matches( E thisOne, TypeVariable thatOne );
  }

  public boolean isReparsing()
  {
    return _bReparsing;
  }
  public void setReparsing( boolean bReparsing )
  {
    _bReparsing = bReparsing;
  }
}
