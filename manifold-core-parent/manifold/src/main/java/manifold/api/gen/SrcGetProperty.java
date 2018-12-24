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

package manifold.api.gen;

/**
 */
public class SrcGetProperty extends AbstractSrcMethod<SrcGetProperty>
{
  public SrcGetProperty( String name, Class type )
  {
    this( null );
    name( "get" + name );
    returns( type );
  }

  public SrcGetProperty( String name, String type )
  {
    this( null );
    name( "get" + name );
    returns( type );
  }

  public SrcGetProperty( String name, SrcType type )
  {
    this( null );
    name( "get" + name );
    returns( type );
  }

  public SrcGetProperty( SrcClass srcClass )
  {
    super( srcClass );
  }
}
