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

package manifold.ext.props;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import manifold.ext.props.rt.api.propgen;
import manifold.internal.javac.IDynamicJdk;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.PRIVATE;
import static manifold.ext.props.Util.getAnnotationMirror;
import static manifold.ext.props.Util.getFlags;

class ClassReaderCompleter implements Symbol.Completer
{
  private final PropertyProcessor _pp;
  private final Symbol.Completer _thisCompleter;
  private final BasicJavacTask _javacTask;

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
  static void replaceCompleter( PropertyProcessor propertyProcessor )
  {
    Context context = propertyProcessor.getContext();
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
    if( !(thisCompleter instanceof ClassReaderCompleter) )
    {
      Symbol.Completer myCompleter = new ClassReaderCompleter(
        propertyProcessor, thisCompleter, propertyProcessor.getJavacTask() );
      thisCompleterField.set( myCompleter );

      if( JreUtil.isJava9orLater() )
      {
        ReflectUtil.field( Symtab.instance( context ), "initialCompleter" ).set( myCompleter );
        ReflectUtil.field( JavacProcessingEnvironment.instance( context ), "initialCompleter" ).set( myCompleter );
      }
    }
  }

  private ClassReaderCompleter(
    PropertyProcessor propertyProcessor, Symbol.Completer thisCompleter, BasicJavacTask javacTask )
  {
    _pp = propertyProcessor;
    _thisCompleter = thisCompleter;
    _javacTask = javacTask;
  }

  @Override
  public void complete( Symbol sym ) throws Symbol.CompletionFailure
  {
    Context context = _javacTask.getContext();
    _thisCompleter.complete( sym );
    Names names = Names.instance( context );
    if( sym instanceof Symbol.ClassSymbol && sym.name != names.package_info )
    {
      Symbol.ClassSymbol classSym = completeAncestryFirst( (Symbol.ClassSymbol)sym );

      Annotate annotate = Annotate.instance( context );
      if( restorePropFields( classSym, names ) )
      {
        // infer properties in .class files
        _pp.inferPropertiesFromClassReader( classSym );
      }
      else
      {
        // It may be that the class hasn't finished adding annotations, try again after annotations complete

        if( JreUtil.isJava8() )
        {
          annotate.normal( () -> {
            restorePropFields( classSym, names );
            // infer properties in .class files
            _pp.inferPropertiesFromClassReader( classSym );
          } );
        }
        else
        {
          ReflectUtil.method( annotate, "normal", Runnable.class )
            .invoke( (Runnable)() -> {
              restorePropFields( classSym, names );
              // infer properties in .class files
              _pp.inferPropertiesFromClassReader( classSym );
            } );
        }
        annotate.flush();
      }
    }
  }

  // recursively processes ancestry of class sym (for property inference, super's properties must be established)
  // (calling complete on the sym calls comes here)
  private Symbol.ClassSymbol completeAncestryFirst( Symbol.ClassSymbol sym )
  {
    Type superclass = sym.getSuperclass();
    if( superclass instanceof Type.ClassType )
    {
      superclass.tsym.complete();
    }
    sym.getInterfaces().forEach( iface -> iface.tsym.complete() );
    return sym;
  }

  /**
   * Restore the user-defined property field, either by recreating it from @propgen info, or by resetting the
   * access modifier. Note the field is not really in the bytecode of the class, this is just the VarSymbol the
   * compiler needs to resolve refs.
   */
  private boolean restorePropFields( Symbol.ClassSymbol classSym, Names names )
  {
    boolean handled = false;

    // Restore originally declared access on backing fields
    //
    for( Symbol sym : IDynamicJdk.instance().getMembers( classSym, false ) )
    {
      if( sym instanceof Symbol.VarSymbol )
      {
        Attribute.Compound propgen = getAnnotationMirror( sym, manifold.ext.props.rt.api.propgen.class );
        if( propgen != null )
        {
          sym.flags_field = sym.flags_field & ~PRIVATE | getFlags( propgen );
          handled = true;
        }
      }
    }

    // Recreate non-backing property fields based on @propgen annotations on corresponding getter/setter
    //
    outer:
    for( Symbol sym : IDynamicJdk.instance().getMembers( classSym, false ) )
    {
      if( sym instanceof Symbol.MethodSymbol )
      {
        Attribute.Compound propgenAnno = getAnnotationMirror( sym, propgen.class );
        if( propgenAnno != null )
        {
          Name fieldName = names.fromString( getName( propgenAnno ) );
          for( Symbol existing : IDynamicJdk.instance().getMembersByName( classSym, fieldName, false ) )
          {
            if( existing instanceof Symbol.VarSymbol )
            {
              // prop field already exists
              continue outer;
            }
          }

          // Create and enter the prop field

          Symbol.MethodSymbol meth = (Symbol.MethodSymbol)sym;
          Type t = meth.getParameters().isEmpty()
            ? meth.getReturnType()
            : meth.getParameters().get( 0 ).type;
          Symbol.VarSymbol propField = new Symbol.VarSymbol( getFlags( propgenAnno ), fieldName, t, classSym );

          // add the @var, @val, @get, @set, etc. annotations
          propField.appendAttributes( List.from( propgenAnno.values.stream()
            .filter( e -> e.snd instanceof Attribute.Array )
            .map( e -> (Attribute.Compound)((Attribute.Array)e.snd).values[0] )
            .collect( Collectors.toList() ) ) );

          // reflectively call:  classSym.members_field.enter( propField );
          ReflectUtil.method( ReflectUtil.field( classSym, "members_field" ).get(),
            "enter", Symbol.class ).invoke( propField );

          handled = true;
        }
      }
    }

    return handled;
  }

  private String getName( Attribute.Compound anno )
  {
    for( Symbol.MethodSymbol methSym : anno.getElementValues().keySet() )
    {
      if( methSym.getSimpleName().toString().equals( "name" ) )
      {
        return (String)anno.getElementValues().get( methSym ).getValue();
      }
    }
    throw new IllegalStateException();
  }
}
