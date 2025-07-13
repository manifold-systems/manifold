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

package manifold.preprocessor.android.syms;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import manifold.api.fs.IFile;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.api.SymbolProvider;
import manifold.preprocessor.definitions.Definitions;
import manifold.rt.api.util.StreamUtil;
import manifold.util.JreUtil;
import manifold.util.ManExceptionUtil;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

public class BuildVariantSymbols implements SymbolProvider
{
  private final LocklessLazyVar<Map<String, String>> _buildConfigSyms =
    LocklessLazyVar.make( () -> BuildConfigFinder.instance().loadBuildConfigSymbols() );

  @Override
  public boolean isDefined( Definitions rootDefinitions, IFile sourceFile, String def )
  {
    return _buildConfigSyms.get().containsKey( def );
  }

  @Override
  public String getValue( Definitions rootDefinitions, IFile sourceFile, String def )
  {
    return _buildConfigSyms.get().get( def );
  }
}
