/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.util;

public class Pair<F, S> {

  final F _first;
  final S _second;

  public Pair( F first, S second) {
    _first = first;
    _second = second;
  }

  public F getFirst() {
    return _first;
  }

  public S getSecond() {
    return _second;
  }

  public static <T, V> Pair<T, V> make(T f, V s) {
    return new Pair<T,V>(f, s);
  }

  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !(o instanceof Pair) )
    {
      return false;
    }

    Pair pair = (Pair)o;

    if( _first != null ? !_first.equals( pair._first ) : pair._first != null )
    {
      return false;
    }
    if( _second != null ? !_second.equals( pair._second ) : pair._second != null )
    {
      return false;
    }

    return true;
  }

  public int hashCode()
  {
    int result;
    result = (_first != null ? _first.hashCode() : 0);
    result = 31 * result + (_second != null ? _second.hashCode() : 0);
    return result;
  }

  @Override
  public String toString()
  {
    return "(" + _first + ", " + _second + ")";
  }

}