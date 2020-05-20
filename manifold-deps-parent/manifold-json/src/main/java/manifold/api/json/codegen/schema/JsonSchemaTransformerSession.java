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

package manifold.api.json.codegen.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import manifold.api.fs.IFile;
import manifold.api.json.codegen.IJsonType;
import manifold.rt.api.util.Pair;

/**
 * Manages a cache of base types per URL for a given Json parser/transformer session.  Note
 * a given session is single-threaded, therefore there is one instance of this class per thread
 * and per session accessible via instance().
 */
public class JsonSchemaTransformerSession
{
  private static final ThreadLocal<JsonSchemaTransformerSession> INSTANCE = new ThreadLocal<>();

  private Map<IFile, Pair<IJsonType, JsonSchemaTransformer>> _baseTypeByUrl;
  private Stack<JsonSchemaTransformer> _transformers;

  public static JsonSchemaTransformerSession instance()
  {
    JsonSchemaTransformerSession instance = INSTANCE.get();
    if( instance == null )
    {
      INSTANCE.set( instance = new JsonSchemaTransformerSession() );
    }
    return instance;
  }

  private JsonSchemaTransformerSession()
  {
    _baseTypeByUrl = new HashMap<>();
    _transformers = new Stack<>();
  }

  void pushTransformer( JsonSchemaTransformer transformer )
  {
    _transformers.push( transformer );
  }
  void popTransformer( JsonSchemaTransformer transformer )
  {
    if( _transformers.peek() != transformer )
    {
      throw new IllegalStateException( "Unbalanced transformer pop" );
    }
    _transformers.pop();
  }

  Pair<IJsonType, JsonSchemaTransformer> getCachedBaseType( IFile url )
  {
    return _baseTypeByUrl.get( url );
  }
  void cacheBaseType( IFile url, Pair<IJsonType, JsonSchemaTransformer> pair )
  {
    _baseTypeByUrl.put( url, pair );
  }

  public void maybeClear()
  {
    if( _transformers.size() == 0 )
    {
      _baseTypeByUrl.clear();
    }
  }
}
