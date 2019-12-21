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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Options
{
  public final String expr;
  public final List<String> imports;
  public final List<String> staticImports;
  public final ClassLoader contextLoader;
  public final Map<String, Symbol> symbols;

  public static Builder builder( String expr )
  {
    return new Builder( expr );
  }

  private Options( String expr, List<String> imports, List<String> staticImports, ClassLoader contextLoader, Map<String, Symbol> symbols )
  {
    this.expr = expr;
    this.imports = imports;
    this.staticImports = staticImports;
    this.contextLoader = contextLoader;
    this.symbols = symbols;
  }

  public static class Builder
  {
    private final String _expr;
    private final List<String> _imports = new ArrayList<>();
    private final List<String> _staticImports = new ArrayList<>();
    private final Map<String, Symbol> _symbols = new ConcurrentHashMap<>();
    private ClassLoader _contextLoader;

    private Builder( String expr )
    {
       _expr = expr;
    }
    
    public Builder importClass( Class clazz )
    {
      _imports.add( clazz.getTypeName() );
      return this;
    }

    public Builder importStatic( Class clazz, String member )
    {
      _staticImports.add( clazz.getTypeName() + '.' + member );
      return this;
    }

    public Builder contextLoader( ClassLoader contextLoader )
    {
      _contextLoader = contextLoader;
      return this;
    }
    
    public Builder symbol( String name, Symbol type )
    {
      _symbols.put( name, type );
      return this;
    }
    
    public Builder symbols( Map<String, Symbol> symbols )
    {
      _symbols.putAll( symbols );
      return this;
    }
    
    public Options build()
    {
      return new Options( _expr, _imports, _staticImports, _contextLoader, _symbols );
    }
  }
}
