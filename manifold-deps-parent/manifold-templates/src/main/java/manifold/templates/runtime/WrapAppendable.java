/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.templates.runtime;

import java.io.IOException;
import manifold.util.ManExceptionUtil;

/**
 * Wraps calls to {@link Appendable} in the generated template to 1. handle {@link IOException}s
 * that otherwise are tedious to handle inside lambdas and 2. handle indentation for {@code nest}ing
 */
@SuppressWarnings("unused")
public class WrapAppendable implements Appendable
{
  private final Appendable _appendable;
  private final StringBuilder _indentHolder;
  private final String _indentation;

  public WrapAppendable( Appendable appendable, String indentation )
  {
    _indentation = indentation;
    _appendable = appendable;
    _indentHolder = new StringBuilder();
  }

  @Override
  public Appendable append( CharSequence csq )
  {
    try
    {
      return getAppendable().append( csq );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public Appendable append( CharSequence csq, int start, int end )
  {
    try
    {
      return getAppendable().append( csq, start, end );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public Appendable append( char c )
  {
    try
    {
      return getAppendable().append( c );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  void complete()
  {
    if( _indentation.length() > 0 )
    {
      indent( _indentHolder );
    }
    else if( _indentHolder.length() > 0 )
    {
      throw new IllegalStateException( "Indentation state is invalid" );
    }
  }

  private Appendable getAppendable()
  {
    return _indentation.length() == 0
           ? _appendable    // append directly to target
           : _indentHolder; // buffer appends, append to target in `complete()`
  }

  private void indent( CharSequence csq )
  {
    try
    {
      _appendable.append( _indentation );
      for( int i = 0; i < csq.length(); i++ )
      {
        char c = csq.charAt( i );
        _appendable.append( c );
        if( c == '\n' )
        {
          _appendable.append( _indentation );
        }
      }
    }
    catch( IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
  }
}
