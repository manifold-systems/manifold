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
 * Wraps calls to {@link Appendable} in the generated template to handle {@link IOException}s
 * that otherwise are tedious to handle inside lambdas.
 */
@SuppressWarnings("unused")
public class WrapAppendable implements Appendable
{
  private final Appendable _appendable;

  public WrapAppendable( Appendable appendable )
  {
    _appendable = appendable;
  }

  @Override
  public Appendable append( CharSequence csq )
  {
    try
    {
      return _appendable.append( csq );
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
      return _appendable.append( csq, start, end );
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
      return _appendable.append( c );
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }
}
