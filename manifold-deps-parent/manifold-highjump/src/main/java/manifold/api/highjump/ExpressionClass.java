/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.highjump;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import manifold.util.ReflectUtil;

class ExpressionClass
{
  private static final LongAdder COUNT = new LongAdder();

  final String _fqn;
  final Options _options;

  Class<?> _class;

  public ExpressionClass( Options options, Map<String, ExpressionClass> fqnToExprClass )
  {
    _options = options;
    _fqn = HighjumpTypeManifold.FQN_PREFIX + incCount();
    fqnToExprClass.put( _fqn, this );
  }

  Options getOptions()
  {
    return _options;
  }

  Object evaluate()
  {
    ReflectUtil.ConstructorRef ctor = ReflectUtil.constructor( getExprClass() );
    if( ctor == null )
    {
      throw new RuntimeException( "Missing no-argument constructor: " + getExprClass().getTypeName() );
    }
    return ReflectUtil.method( ctor.newInstance(), "evaluate" ).invoke();
  }

  Class<?> getExprClass()
  {
    if( _class == null )
    {
      try
      {
        _class = _options.contextLoader == null ? Class.forName( _fqn ) : Class.forName( _fqn, true, _options.contextLoader );
      }
      catch( ClassNotFoundException e )
      {
        ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
          _class = Class.forName( _fqn, true, threadContextClassLoader );
        }
        catch( ClassNotFoundException ex )
        {
          throw new RuntimeException( e );
        }
      }
    }
    return _class;
  }

  private static long incCount()
  {
    COUNT.increment();
    return COUNT.longValue();
  }
}
