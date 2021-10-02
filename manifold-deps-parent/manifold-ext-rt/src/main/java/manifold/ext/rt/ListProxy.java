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

package manifold.ext.rt;

import manifold.ext.rt.api.ICallHandler;
import manifold.ext.rt.api.IListBacked;
import manifold.util.ReflectUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ListProxy
{
  public static Object invoke( List list, Object proxy, Method method, Object[] args )
  {
    assert method.getParameterCount() == (args == null ? 0 : args.length);

    String methodName = method.getName();
    Object result;
    if( method.isDefault() )
    {
      result = ReflectUtil.invokeDefault( proxy, method, args );
    }
    else
    {
      result = ICallHandler.UNHANDLED;

      if( proxy instanceof IListBacked && methodName.equals( "getList" ) )
      {
        result = list;
      }
      if( result == ICallHandler.UNHANDLED )
      {
        switch( methodName )
        {
          case "hashCode":
            result = _hashCode( list );
            break;
          case "equals":
            result = _equals( list, args[0] );
            break;
          case "toString":
            result = _toString( list );
            break;
        }
      }
    }
    if( result == ICallHandler.UNHANDLED )
    {
      throw new RuntimeException( "Missing method: " + methodName + "(" + Arrays.toString( method.getParameterTypes() ) + ")" );
    }
    return result;
  }

  private static String _toString( List list )
  {
    return list.toString();
  }

  private static int _hashCode( List list )
  {
    return list.hashCode();
  }

  private static boolean _equals( List list, Object obj )
  {
    return list.equals( obj ) ||
      obj instanceof IListBacked && list.equals( ((IListBacked)obj).getList() );
  }
}
