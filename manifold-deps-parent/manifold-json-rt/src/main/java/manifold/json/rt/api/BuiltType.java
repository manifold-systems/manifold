/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

import manifold.json.rt.api.IJsonBindingsBacked;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface BuiltType<T> extends IJsonBindingsBacked
{
  default Class<T> findBuiltTypeFrom( Class builderClass )
  {
    Class<?>[] interfaces = getClass().getInterfaces();
    for( Class<?> e : interfaces )
    {
      for( Type iface : e.getGenericInterfaces() )
      {
        if( iface.getTypeName().startsWith( builderClass.getTypeName() ) &&
          iface instanceof ParameterizedType )
        {
          Type typeArg = ((ParameterizedType)iface).getActualTypeArguments()[0];
          if( typeArg instanceof ParameterizedType )
          {
            typeArg = ((ParameterizedType)typeArg).getRawType();
          }
          //noinspection unchecked
          return (Class<T>)typeArg;
        }
      }
    }
    throw new IllegalStateException();
  }
}
