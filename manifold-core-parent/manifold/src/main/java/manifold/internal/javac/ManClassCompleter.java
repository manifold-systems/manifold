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

package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import java.util.ArrayList;

public class ManClassCompleter implements Symbol.Completer
{
  private final Symbol.Completer _thisCompleter;
  private final ArrayList<Symbol.Completer> _userCompletes = new ArrayList<>();

  /**
   * Replace the {@code ClassReader.thisCompleter} with our own so that after a .class file loads we can restore the
   * property fields to their original declared setting. In the case of a property having a backing field, the field's
   * {@code private} access modifier is changed back to whatever it was declared to be in source. If a property field
   * is not a backing field, it does not exist in the .class file, therefore this is where we recreate it.
   * <p/>
   * Properties are also inferred immediately following a .class file's completion. The ancestry of the class is forced
   * to complete before properties can be inferred.
   * <p/>
   * Note the .class file remains untouched; the changes made here are only to the compiler's ClassSymbol.
   */
  public static ManClassCompleter instance( Context context )
  {
    ReflectUtil.LiveFieldRef thisCompleterField;
    if( JreUtil.isJava8() )
    {
      ClassReader classReader = ClassReader.instance( context );
      thisCompleterField = ReflectUtil.field( classReader, "thisCompleter" );
    }
    else
    {
      Object classFinder = ReflectUtil.method( "com.sun.tools.javac.code.ClassFinder", "instance", Context.class )
        .invokeStatic( context );
      thisCompleterField = ReflectUtil.field( classFinder, "thisCompleter" );
    }

    Symbol.Completer thisCompleter = (Symbol.Completer)thisCompleterField.get();
    if( thisCompleter instanceof ManClassCompleter )
    {
      return (ManClassCompleter)thisCompleter;
    }

    ManClassCompleter myCompleter = new ManClassCompleter( thisCompleter );
    thisCompleterField.set( myCompleter );

    if( JreUtil.isJava9orLater() )
    {
      ReflectUtil.field( Symtab.instance( context ), "initialCompleter" ).set( myCompleter );
      ReflectUtil.field( JavacProcessingEnvironment.instance( context ), "initialCompleter" ).set( myCompleter );
    }
    return myCompleter;
  }

  private ManClassCompleter( Symbol.Completer thisCompleter )
  {
    _thisCompleter = thisCompleter;
  }

  @Override
  public void complete( Symbol sym ) throws Symbol.CompletionFailure
  {
    _thisCompleter.complete( sym );
    for( Symbol.Completer completer : _userCompletes )
    {
      completer.complete( sym );
    }
  }

  public void addListener( Symbol.Completer completer )
  {
    _userCompletes.add( completer );
  }

  public void removeListener( Symbol.Completer completer )
  {
    _userCompletes.remove( completer );
  }
}
