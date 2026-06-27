/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.delegation;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import manifold.util.ReflectUtil;

import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.util.function.Function;

public class Util
{
  static JCTree.JCAnnotation getAnnotation( JCTree.JCVariableDecl field, Class<? extends Annotation> cls )
  {
    for( JCTree.JCAnnotation jcAnno : field.getModifiers().getAnnotations() )
    {
      if( cls.getSimpleName().equals( jcAnno.annotationType.toString() ) )
      {
        return jcAnno;
      }
      else if( cls.getTypeName().equals( jcAnno.annotationType.toString() ) )
      {
        return jcAnno;
      }
    }
    return null;
  }

  static Attribute.Compound getAnnotationMirror( Symbol sym, Class<? extends Annotation> annoClass )
  {
    for( Attribute.Compound anno : sym.getAnnotationMirrors() )
    {
      if( annoClass.getTypeName().equals( anno.type.tsym.getQualifiedName().toString() ) )
      {
        return anno;
      }
    }
    return null;
  }

  public static JavaFileObject getFile( Tree node, Function<Tree, Tree> parentOf )
  {
    JCTree.JCClassDecl classDecl = getClassDecl( node, parentOf );
    if( classDecl == null )
    {
      ReflectUtil.LiveFieldRef symField = ReflectUtil.WithNull.field( node, "sym" );
      Symbol sym = symField == null ? null : (Symbol)symField.get();
      while( sym != null )
      {
        Symbol owner = sym.owner;
        if( owner instanceof Symbol.ClassSymbol )
        {
          return ((Symbol.ClassSymbol)owner).sourcefile;
        }
        sym = owner;
      }
    }
    return classDecl == null ? null : classDecl.sym.sourcefile;
  }

  public static JCTree.JCClassDecl getClassDecl( Tree node, Function<Tree, Tree> parentOf )
  {
    if( node == null || node instanceof JCTree.JCCompilationUnit )
    {
      return null;
    }

    if( node instanceof JCTree.JCClassDecl )
    {
      return (JCTree.JCClassDecl)node;
    }

    return getClassDecl( parentOf.apply( node ), parentOf );
  }
}
