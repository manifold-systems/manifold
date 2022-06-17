/*
 * Copyright (c) 2022 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.tuple.rt.internal;

import manifold.tuple.rt.api.Tuple;
import manifold.tuple.rt.api.TupleItem;
import manifold.util.ReflectUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The super type for tuple classes generated from tuple expressions.
 * It is not designed for general use.
 */
@SuppressWarnings( "unused" )
public abstract class GeneratedTuple implements Tuple, Serializable
{
  @Override
  public List<?> orderedValues()
  {
    return orderedLabels().stream()
      .map( f -> {
        try
        {
          return ReflectUtil.field( this, f ).get();
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      } ).collect( Collectors.toList() );
  }

  public int hashCode()
  {
    return Arrays.hashCode( orderedValues().toArray() );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !Tuple.class.isAssignableFrom( o.getClass() ) )
    {
      return false;
    }
    return orderedValues().equals( ((Tuple)o).orderedValues() ) &&
      orderedLabels().equals( ((Tuple)o).orderedLabels() );
  }

  public String toString()
  {
    List<String> labels = orderedLabels();
    List<?> values = orderedValues();
    StringBuilder sb = new StringBuilder( "(" );
    for( int i = 0; i < labels.size(); i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      sb.append( labels.get( i ) ).append( ": " ).append( values.get( i ) );
    }
    sb.append( ')' );
      return sb.toString();
  }

  @Override
  public Iterator<TupleItem> iterator()
  {
    return
      new Iterator<TupleItem>()
      {
        int _index = 0;
        final List<String> _orderedLabels = orderedLabels();
        final List<?> _orderedValues = orderedValues();

        @Override
        public boolean hasNext()
        {
          return _index < _orderedValues.size();
        }

        @Override
        public TupleItem next()
        {
          return new TupleValueImpl(
            _orderedLabels.get( _index ), _orderedValues.get( _index++ ) );
        }
      };
  }

  private static class TupleValueImpl implements TupleItem
  {
    final String _name;
    final Object _value;

    TupleValueImpl( String name, Object value )
    {
      _name = name;
      _value = value;
    }

    @Override
    public String getName()
    {
      return _name;
    }

    @Override
    public Object getValue()
    {
      return _value;
    }
  }

}
