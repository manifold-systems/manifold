/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.json.rt.api;

import manifold.json.rt.Json;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonList<T> implements IJsonList<T>, Serializable
{
  private List _list;
  private Class<T> _finalComponentType;

  public JsonList()
  {
    _list = new ArrayList<>();
    _finalComponentType = (Class<T>)Object.class;
  }

  public JsonList( Class<T> finalComponentType )
  {
    _list = new ArrayList<>();
    _finalComponentType = finalComponentType;
  }

  public JsonList( List jsonList, Class<T> finalComponentType )
  {
    _list = jsonList;
    _finalComponentType = finalComponentType;
  }

  @Override
  public List getList()
  {
    return _list;
  }

  @Override
  public Class<?> getFinalComponentType()
  {
    return _finalComponentType;
  }

  @SuppressWarnings("EqualsDoesntCheckParameterClass")
  @Override
  public boolean equals( Object o )
  {
    if( this == o ) return true;
    if( o == null ) return false;
    return Objects.equals( _list, o );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _list );
  }

  @Override
  public String toString()
  {
    return _list.toString();
  }

  private Object writeReplace()
  {
    return new Serializer<>( this );
  }

  public static class Serializer<T> implements Externalizable
  {
    private Class<T> _type;
    private String _json;

    public Serializer()
    {
    }

    Serializer( JsonList<T> jsonList )
    {
      _json = Json.toJson( jsonList );
      _type = jsonList._finalComponentType;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
      out.writeObject( _type );
      out.writeObject( _json );
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
      //noinspection unchecked
      _type = (Class<T>)in.readObject();
      _json = (String)in.readObject();
    }

    Object readResolve() throws ObjectStreamException
    {
      List<?> list = (List<?>)Json.fromJson( _json );
      return new JsonList<>( list, _type );
    }
  }
}
