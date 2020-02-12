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

package manifold.api.type;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class BasicIncrementalCompileDriver implements IIncrementalCompileDriver
{
  private final boolean _incremental;

  public BasicIncrementalCompileDriver( boolean incremental )
  {
    _incremental = incremental;
  }

  @Override
  public boolean isIncremental()
  {
    return _incremental;
  }

  @Override
  public Collection<File> getChangedFiles()
  {
    String manFilesProp = System.getProperty( "manifold.source.files" );
    if( manFilesProp == null )
    {
      return Collections.emptySet();
    }

    List<String> paths = new ArrayList<>();
    for( StringTokenizer tokenizer = new StringTokenizer( manFilesProp, File.pathSeparator ); tokenizer.hasMoreTokens(); )
    {
      paths.add( tokenizer.nextToken() );
    }
    return paths.stream().map( File::new ).collect( Collectors.toList() );
  }
}
