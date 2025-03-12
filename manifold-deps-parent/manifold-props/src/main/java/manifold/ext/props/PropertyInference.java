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

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import manifold.ext.props.rt.api.*;
import manifold.internal.javac.IDynamicJdk;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.ReservedWordMapping;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static com.sun.tools.javac.code.TypeTag.CLASS;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.reflect.Modifier.*;
import static java.lang.reflect.Modifier.PRIVATE;
import static manifold.ext.props.Util.*;

class PropertyInference
{
  private final Consumer<VarSymbol> _backingFieldConsumer;
  private final Supplier<Context> _contextSupplier;

  PropertyInference( Consumer<VarSymbol> backingFieldConsumer,
                     Supplier<Context> contextSupplier )
  {
    _backingFieldConsumer = backingFieldConsumer;
    _contextSupplier = contextSupplier;
  }

  private Context context()
  {
    return _contextSupplier.get();
  }

  void inferProperties( ClassSymbol classSym )
  {
    Map<String, Set<PropAttrs>> fromGetter = new HashMap<>();
    Map<String, Set<PropAttrs>> fromSetter = new HashMap<>();
    for( Symbol sym : IDynamicJdk.instance().getMembers( classSym, false ) )
    {
      if( sym instanceof MethodSymbol )
      {
        gatherCandidatesFromClass( (MethodSymbol)sym, fromGetter, fromSetter );
      }
      else if( sym instanceof Symbol.VarSymbol && classSym.getKind().name().equals( "RECORD" ) )
      {
        StreamSupport.stream( IDynamicJdk.instance().getMembers( classSym, false ).spliterator(), false )
          .filter( m -> m instanceof MethodSymbol && m.name.equals( sym.name ) )
          .map( m -> (MethodSymbol)m )
          .findFirst()
          .ifPresent( accessor -> gatherCandidatesFromRecord( accessor, fromGetter ) );
      }
    }

    handleVars( fromGetter, fromSetter );
    handleVals( fromGetter, fromSetter );
    handleWos( fromGetter, fromSetter );
  }

  private void gatherCandidatesFromClass( MethodSymbol m, Map<String, Set<PropAttrs>> fromGetter, Map<String, Set<PropAttrs>> fromSetter )
  {
    if( isSynthetic( m ) )
    {
      // don't derive a property (or property type) from a synthetic accessor
      return;
    }
    
    Attribute.Compound propgenAnno = getAnnotationMirror( m, propgen.class );
    if( propgenAnno != null )
    {
      // already a property
      return;
    }

    PropAttrs derivedFromGetter = derivePropertyNameFromGetter( m );
    if( derivedFromGetter != null )
    {
      fromGetter.computeIfAbsent( derivedFromGetter._name, key -> new HashSet<>() )
        .add( derivedFromGetter );
    }
    PropAttrs derivedFromSetter = derivePropertyNameFromSetter( m );
    if( derivedFromSetter != null )
    {
      fromSetter.computeIfAbsent( derivedFromSetter._name, key -> new HashSet<>() )
        .add( derivedFromSetter );
    }
  }
  private void gatherCandidatesFromRecord( MethodSymbol m, Map<String, Set<PropAttrs>> fromGetter )
  {
    Attribute.Compound propgenAnno = getAnnotationMirror( m, propgen.class );
    if( propgenAnno != null )
    {
      // already a property
      return;
    }

    PropAttrs derivedFromAccessor = derivePropertyNameFromRecordAccessor( m );
    if( derivedFromAccessor != null )
    {
      fromGetter.computeIfAbsent( derivedFromAccessor._name, key -> new HashSet<>() )
        .add( derivedFromAccessor );
    }
  }

  private boolean isSynthetic( MethodSymbol m )
  {
    return (m.flags() & Flags.SYNTHETIC) != 0 ||
      (m.flags() & Flags.BRIDGE) != 0;
  }

  private boolean isInherited( Symbol ancestorSym, ClassSymbol origin )
  {
    ClassSymbol ancestorClass = ancestorSym.enclClass();
    if( ancestorClass == origin )
    {
      return true;
    }
    if( ancestorSym.isStatic() && ancestorClass.isInterface() )
    {
      return false;
    }
    if( Modifier.isPublic( (int)ancestorSym.flags_field ) ||
      Modifier.isProtected( (int)ancestorSym.flags_field ) )
    {
      return true;
    }
    if( Modifier.isPrivate( (int)ancestorSym.flags_field ) )
    {
      return ancestorClass.outermostClass() == origin.outermostClass();
    }
    // package-private
    return !Modifier.isPrivate( (int)ancestorSym.flags_field ) &&
      ancestorClass.packge().equals( origin.packge() );
  }

  private void handleVars( Map<String, Set<PropAttrs>> fromGetter, Map<String, Set<PropAttrs>> fromSetter )
  {
    outer:
    for( Map.Entry<String, Set<PropAttrs>> entry : fromGetter.entrySet() )
    {
      String name = entry.getKey();
      Set<PropAttrs> getters = entry.getValue();
      Set<PropAttrs> setters = fromSetter.get( name );
      if( getters != null && !getters.isEmpty() && setters != null && !setters.isEmpty() )
      {
        Types types = Types.instance( context() );
        for( Iterator<PropAttrs> getterIter = getters.iterator(); getterIter.hasNext(); )
        {
          PropAttrs getAttr = getterIter.next();
          Type getType = getAttr._type;
          for( Iterator<PropAttrs> setterIter = setters.iterator(); setterIter.hasNext(); )
          {
            PropAttrs setAttr = setterIter.next();
            Type setType = setAttr._type;
            if( types.isSubtype( getType, setType ) && getAttr._m.isStatic() == setAttr._m.isStatic() )
            {
              makeVar( getAttr, setAttr );
              getterIter.remove();
              setterIter.remove();
              continue outer;
            }
          }
        }
      }
      // Handle isXxx() where isXxx is the property name and the getter method name
      if( getters != null && !getters.isEmpty() && isIsProperty( name ) )
      {
        setters = fromSetter.get( ManStringUtil.uncapitalize( ManStringUtil.uncapitalize( name.substring( 2 ) ) ) );
        if( setters != null && !setters.isEmpty() )
        {
          Types types = Types.instance( context() );
          for( Iterator<PropAttrs> getterIter = getters.iterator(); getterIter.hasNext(); )
          {
            PropAttrs getAttr = getterIter.next();
            if( getAttr._m.name.toString().equals( name ) ) // only when isXxx is the name of property and getter method
            {
              Type getType = getAttr._type;
              for( Iterator<PropAttrs> setterIter = setters.iterator(); setterIter.hasNext(); )
              {
                PropAttrs setAttr = setterIter.next();
                Type setType = setAttr._type;
                if( types.isSubtype( getType, setType ) && getAttr._m.isStatic() == setAttr._m.isStatic() )
                {
//                  setAttr._name = getAttr._name;
                  makeVar( getAttr, setAttr );
                  getterIter.remove();
                  setterIter.remove();
                  continue outer;
                }
              }
            }
          }
        }
      }
    }
  }

  private boolean isIsProperty( String name )
  {
    return name.length() > 2 && name.startsWith( "is" ) && Character.isUpperCase( name.charAt( 2 ) );
  }

  private void handleVals( Map<String, Set<PropAttrs>> fromGetter, Map<String, Set<PropAttrs>> fromSetter )
  {
    for( Map.Entry<String, Set<PropAttrs>> entry : fromGetter.entrySet() )
    {
      String name = entry.getKey();
      Set<PropAttrs> getters = entry.getValue();
      if( getters != null && !getters.isEmpty() )
      {
        Set<PropAttrs> setters = fromSetter.get( name );
        if( setters == null || setters.isEmpty() )
        {
          PropAttrs getAttr = getters.iterator().next();
          makeVal( getAttr );
          getters.remove( getAttr );
        }
      }
    }
  }

  private void handleWos( Map<String, Set<PropAttrs>> fromGetter, Map<String, Set<PropAttrs>> fromSetter )
  {
    for( Map.Entry<String, Set<PropAttrs>> entry : fromSetter.entrySet() )
    {
      String name = entry.getKey();
      Set<PropAttrs> setters = entry.getValue();
      if( setters != null && !setters.isEmpty() )
      {
        Set<PropAttrs> getters = fromGetter.get( name );
        if( getters == null || getters.isEmpty() )
        {
          PropAttrs setAttr = setters.iterator().next();
          makeWo( setAttr );
          setters.remove( setAttr );
        }
      }
    }
  }

  private void makeVar( PropAttrs getAttr, PropAttrs setAttr )
  {
    Names names = Names.instance( context() );
    Name fieldName = names.fromString( getAttr._name );
    ClassSymbol classSym = getAttr._m.enclClass();

    Type t = getMoreSpecificType( getAttr._type, setAttr._type );
    int flags = weakest( getAccess( getAttr._m ), getAccess( setAttr._m ) );
    flags |= (getAttr._m.flags_field & STATIC);

    Pair<Integer, VarSymbol> res = handleExistingField( fieldName, t, flags, classSym, var.class );
    if( res == null )
    {
      // existing field found and, if local and compatible, changed access privilege in-place and added @var|val|set
      return;
    }

    // Create and enter the prop field

    flags = res.fst == MAX_VALUE ? flags : weakest( res.fst, flags );
    VarSymbol propField = new VarSymbol( flags, fieldName, t, classSym );

    addField( propField, classSym, var.class );
  }

  private void makeVal( PropAttrs getAttr )
  {
    Names names = Names.instance( context() );
    Name fieldName = names.fromString( getAttr._name );
    ClassSymbol classSym = getAttr._m.enclClass();

    Pair<Integer, VarSymbol> res = handleExistingField( fieldName, getAttr._type, (int)getAttr._m.flags_field, classSym, val.class );
    if( res == null )
    {
      // existing field found and, if local and compatible, changed access privilege in-place and added @var|val|set
      return;
    }

    // Create and enter the prop field

    int flags = res.fst == MAX_VALUE
      ? getAccess( (int)getAttr._m.flags_field )
      : weakest( res.fst, (int)getAttr._m.flags_field );
    flags |= (getAttr._m.flags_field & STATIC);
    VarSymbol propField = new VarSymbol( flags, fieldName, getAttr._type, classSym );

    // if super's field is writable, make this one also writable to allow the setter to be used in assignments
    Class<? extends Annotation> varClass =
      isWritableProperty( res.snd )
        ? var.class
        : val.class;
    addField( propField, classSym, varClass );
  }

  private void makeWo( PropAttrs setAttr )
  {
    Names names = Names.instance( context() );
    Name fieldName = names.fromString( setAttr._name );
    ClassSymbol classSym = setAttr._m.enclClass();

    Pair<Integer, VarSymbol> res = handleExistingField( fieldName, setAttr._type, (int)setAttr._m.flags_field, classSym, set.class );
    if( res == null )
    {
      // existing field found and, if local and compatible, changed access privilege in-place
      return;
    }

    // Create and enter the prop field

    int flags = res.fst == MAX_VALUE
      ? getAccess( (int)setAttr._m.flags_field )
      : weakest( res.fst, (int)setAttr._m.flags_field );
    Type t = setAttr._type;

    flags |= (setAttr._m.flags_field & STATIC);
    VarSymbol propField = new VarSymbol( flags, fieldName, t, classSym );

    // if super's field is readable, make this one also readable to allow the getter to be used
    Class<? extends Annotation> varClass =
      isReadableProperty( res.snd )
        ? var.class
        : set.class;
    addField( propField, classSym, varClass );
  }

  private void addField( VarSymbol propField, ClassSymbol classSym, Class<? extends Annotation> varClass )
  {
    addField( propField, classSym, varClass, -1, true );
  }

  private void addField( VarSymbol propField, ClassSymbol classSym, Class<? extends Annotation> varClass,
                         int existingDeclaredAccess, boolean addToClass )
  {
    Object ctx = null;
    if( JreUtil.isJava9orLater() )
    {
      if( JreUtil.isJava9Modular_compiler( context() ) )
      {
        ctx = ReflectUtil.method( JavacElements.instance( context() ), "getModuleElement", CharSequence.class ).invoke( "manifold.props.rt" );
      }
      else // unnamed module
      {
        ctx = ReflectUtil.field( Symtab.instance( context() ), "unnamedModule" ).get();
      }
    }

    if( classSym.isInterface() && ((int)propField.flags_field & STATIC) != 0 )
    {
      // explicitly declared static property in interface, tag with Static
      ClassSymbol staticSym = IDynamicJdk.instance().getTypeElement( context(), ctx, Static.class.getTypeName() );
      Attribute.Compound staticAnno = new Attribute.Compound( staticSym.type, List.nil() );
      propField.appendAttributes( List.of( staticAnno ) );
    }

    ClassSymbol varSym = IDynamicJdk.instance().getTypeElement( context(), ctx, varClass.getTypeName() );
    ClassSymbol autoSym = IDynamicJdk.instance().getTypeElement( context(), ctx, auto.class.getTypeName() );
    if( varSym != null )
    {
      Attribute.Compound varAnno = new Attribute.Compound( varSym.type, List.nil() );
      // tag with @auto to prevent usage within the enclosing class
      Attribute.Compound autoAnno;
      if( existingDeclaredAccess == -1 )
      {
        autoAnno = new Attribute.Compound( autoSym.type, List.nil() );
      }
      else
      {
        // there is an existing field, store its declared access privilege modifier

        Names names = Names.instance( context() );
        Symtab symtab = Symtab.instance( context() );
        MethodSymbol declaredAccessMeth = (MethodSymbol)IDynamicJdk.instance().getMembersByName(
          autoSym, names.fromString( "declaredAccess" ) ).iterator().next();
        autoAnno = new Attribute.Compound( autoSym.type,
          List.of( new Pair<>( declaredAccessMeth,
            new Attribute.Constant( symtab.intType, existingDeclaredAccess ) ) ) );

        // Also add it to backing fields, since this indicates the field should not be erased (apples to compiled source)
        _backingFieldConsumer.accept( propField );
      }
      // add the @var, @val, @get, @set, etc. annotations
      propField.appendAttributes( List.of( varAnno, autoAnno ) );

      if( addToClass )
      {
        // call:  classSym.members_field.enter( propField );
        ReflectUtil.method( ReflectUtil.field( classSym, "members_field" ).get(),
          "enter", Symbol.class ).invoke( propField );
      }
    }
  }

  private Type getMoreSpecificType( Type t1, Type t2 )
  {
    Types types = Types.instance( context() );
    if( types.isSameType( t1, t2 ) )
    {
      return t1;
    }
    return types.isSubtype( t1, t2 ) ? t1 : t2;
  }

  private Pair<Integer, VarSymbol> handleExistingField( Name fieldName, Type t, int flags, ClassSymbol classSym, Class<? extends Annotation> varClass )
  {
    Symbol[] existing = findExistingFieldInAncestry( fieldName, classSym, classSym );
    if( existing != null && existing.length > 0 )
    {
      // a field already exists with this name

      VarSymbol exField = (VarSymbol)existing[0];
      Types types = Types.instance( context() );
      boolean subtype;
      try
      {
        subtype = types.isSubtype( exField.type, t );
      }
      catch( Symbol.CompletionFailure scf )
      {
        // can happen e.g., a type is not included in the classpath or
        // a JRE class is not represented in ct.sym (sun.awt.util.IdentityArrayList)
        return null;
      }

      if( subtype &&
        Modifier.isStatic( (int)exField.flags_field ) == Modifier.isStatic( flags ) && !exField.owner.isInterface() &&
        (!Modifier.isPublic( (int)exField.flags_field ) || isPropertyField( exField )) /* existing public field must always be accessed directly (see keep PropertyProcess#keepRefToField() */ )
      {
        int weakest = weakest( getAccess( (int)exField.flags_field ), getAccess( flags ) );

        int declaredAccess = (int)exField.flags_field & (PUBLIC | PROTECTED | PRIVATE);
        if( isExitingFieldAccessible( classSym, exField, declaredAccess ) )
        {
          if( exField.enclClass() == classSym )
          {
            // make the existing field accessible according to the weakest of property methods
            exField.flags_field = exField.flags_field & ~(PUBLIC | PROTECTED | PRIVATE) | weakest;
            addField( exField, classSym, varClass, declaredAccess, false );
          }
          return null; // don't create another one
        }
        if( isPropertyField( exField ) )
        {
          return new Pair<>( weakest, exField ); // existing field is compatible, create one with `weakest` access (or weaker)
        }
      }
      return null; // existing field is in conflict, don't create another one
    }
    return new Pair<>( MAX_VALUE, null ); // no existing field, create one
  }

  private boolean isExitingFieldAccessible( ClassSymbol classSym, VarSymbol exField, int mod )
  {
    boolean isInferredProperty = exField.getAnnotationMirrors().stream()
      .anyMatch( anno -> anno.type.tsym.getQualifiedName().toString().equals( auto.class.getTypeName() ) );
    boolean propertyField = !isInferredProperty && isPropertyField( exField );
    switch( mod )
    {
      case PUBLIC:
        if( !propertyField )
        {
          // field is public
          return true;
        }
        // fall through
      case PROTECTED:
        if( !propertyField && classSym.isSubClass( exField.enclClass(), Types.instance( context() ) ) )
        {
          // sublcass of field's class
          return true;
        }
        // fall through
      case 0: // PACKAGE
        if( !propertyField && exField.enclClass().packge() == classSym.packge() )
        {
          // same package as field's class
          return true;
        }
        // fall through
      case PRIVATE:
        if( exField.enclClass() == classSym )
        {
          // same class as field
          return true;
        }
    }
    return false;
  }

  private Symbol[] findExistingFieldInAncestry( Name name, Symbol.TypeSymbol c, ClassSymbol origin )
  {
    if( !(c instanceof ClassSymbol) )
    {
      return null;
    }

    Types types = Types.instance( context() );
    for( Symbol sym : IDynamicJdk.instance().getMembersByName( (ClassSymbol)c, name ) )
    {
      if( sym instanceof VarSymbol )
      {
        return isInherited( sym, origin ) ? new Symbol[]{sym} : new Symbol[]{};
      }
    }
    Type st = types.supertype( c.type );
    if( st != null && st.hasTag( CLASS ) )
    {
      Symbol[] sym = findExistingFieldInAncestry( name, st.tsym, origin );
      if( sym != null )
      {
        return sym;
      }
    }
    for( List<Type> l = types.interfaces( c.type ); l.nonEmpty(); l = l.tail )
    {
      Symbol[] sym = findExistingFieldInAncestry( name, l.head.tsym, origin );
      if( sym != null && sym.length > 0 && !Modifier.isStatic( (int)sym[0].flags_field ) )
      {
        return sym;
      }
    }
    return null;
  }

  private static class PropAttrs
  {
    String _prefix;
    String _name;
    Type _type;
    MethodSymbol _m;

    PropAttrs( String prefix, String name, Type type, MethodSymbol m )
    {
      _prefix = prefix;
      _name = name;
      _type = type;
      _m = m;
    }
  }

  private PropAttrs derivePropertyNameFromGetter( MethodSymbol m )
  {
    Symtab symtab = Symtab.instance( context() );
    if( m.getReturnType() == symtab.voidType || !m.getParameters().isEmpty() )
    {
      return null;
    }

    PropAttrs derived = deriveName( m, "get", m.getReturnType() );
    return derived == null ? deriveName( m, "is", m.getReturnType() ) : derived;
  }

  private PropAttrs derivePropertyNameFromSetter( MethodSymbol m )
  {
    return m.getParameters().length() != 1 ? null : deriveName( m, "set", m.getParameters().get( 0 ).type );
  }

  private PropAttrs derivePropertyNameFromRecordAccessor( MethodSymbol m )
  {
    Symtab symtab = Symtab.instance( context() );
    if( m.getReturnType() == symtab.voidType || !m.getParameters().isEmpty() )
    {
      return null;
    }

    return new PropAttrs( "", m.getSimpleName().toString(), m.getReturnType(), m );
  }

  private PropAttrs deriveName( MethodSymbol m, String prefix, Type type )
  {
    String name = m.getSimpleName().toString();
    if( name.startsWith( prefix ) )
    {
      String derived = name.substring( prefix.length() );
      if( !derived.isEmpty() )
      {
        char first = derived.charAt( 0 );
        if( Character.isUpperCase( first ) || first == '$' )
        {
          if( "is".equals( prefix ) && first != '$' )
          {
            // keep "is" in the name to prevent collisions where isBook():true and getBook():Book are both there
            derived = prefix + derived;
          }
          String propName = ManStringUtil.uncapitalize( derived );
          if( propName.equals( ReservedWordMapping.getIdentifierForName( propName ) ) ) // avoid clashing with Java reserved words
          {
            return new PropAttrs( prefix, propName, type, m );
          }
        }
        else if( first == '_' )
        {
          StringBuilder sb = new StringBuilder( derived );
          while( sb.length() > 0 && sb.charAt( 0 ) == '_' )
          {
            sb.deleteCharAt( 0 );
          }
          if( sb.length() > 0 )
          {
            if( "is".equals( prefix ) )
            {
              // keep "is" in the name to prevent collisions where is_book():true and get_book():Book are both there
              sb = new StringBuilder( prefix + ManStringUtil.capitalize( sb.toString() ) );
            }
            String propName = sb.toString();
            if( propName.equals( ReservedWordMapping.getIdentifierForName( propName ) ) ) // avoid clashing with Java reserved words
            {
              return new PropAttrs( prefix, propName, type, m );
            }
          }
        }
      }
    }
    return null;
  }
}
