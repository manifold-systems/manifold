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

package manifold.ext;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.AttrContextEnv;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import manifold.ExtIssueMsg;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.host.IManifoldHost;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.api.type.IncrementalCompile;
import manifold.api.type.Precompile;
import manifold.ext.api.Extension;
import manifold.ext.api.Jailbreak;
import manifold.ext.api.Self;
import manifold.ext.api.Structural;
import manifold.ext.api.This;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.GeneratedJavaStubFileObject;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.TypeProcessor;
import manifold.util.JreUtil;
import manifold.util.Pair;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.ConcurrentHashSet;

/**
 */
public class ExtensionTransformer extends TreeTranslator
{
  private final ExtensionManifold _sp;
  private final TypeProcessor _tp;
  private boolean _bridgeMethod;

  ExtensionTransformer( ExtensionManifold sp, TypeProcessor typeProcessor )
  {
    _sp = sp;
    _tp = typeProcessor;
  }

  @SuppressWarnings("WeakerAccess")
  public TypeProcessor getTypeProcessor()
  {
    return _tp;
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitIdent( JCTree.JCIdent tree )
  {
    super.visitIdent( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypeUtil.isStructuralInterface( _tp, tree.sym ) && !isReceiver( tree ) )
    {
      Symbol.ClassSymbol objectSym = getObjectClass();
      Tree parent = _tp.getParent( tree );
      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;

        long parameterModifier = 8589934592L; // Flag.Flag.PARAMETER.value
        if( (((JCTree.JCVariableDecl)parent).mods.flags & parameterModifier) != 0 )
        {
          objIdent.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).vartype = objIdent;
        }
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      tree = objIdent;
      tree.type = objectSym.type;
    }
    result = tree;
  }

  @Override
  public void visitLambda( JCTree.JCLambda tree )
  {
    super.visitLambda( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    tree.type = eraseStructureType( tree.type );
    ArrayList<Type> types = new ArrayList<>();
    for( Type target: IDynamicJdk.instance().getTargets( tree ) )
    {
      types.add( eraseStructureType( target ) );
    }
    IDynamicJdk.instance().setTargets( tree, List.from( types ) );
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    super.visitSelect( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypeUtil.isStructuralInterface( _tp, tree.sym ) && !isReceiver( tree ) )
    {
      Symbol.ClassSymbol objectSym = getObjectClass();
      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;
        long parameterModifier = 8589934592L; // Flag.Flag.PARAMETER.value
        if( (((JCTree.JCVariableDecl)parent).mods.flags & parameterModifier) != 0 )
        {
          objIdent.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).vartype = objIdent;
        }
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      result = objIdent;
    }
    else if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitAssign( JCTree.JCAssign tree )
  {
    super.visitAssign( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitAssignop( JCTree.JCAssignOp tree )
  {
    super.visitAssignop( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      // +=, -=, etc. operators not supported with jailbreak, only direct assignment
      _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_COMPOUND_OP_NOT_ALLOWED_REFLECTION.get() );
      Types types = Types.instance( ((BasicJavacTask)_tp.getJavacTask()).getContext() );
      tree.type = types.createErrorType( tree.type );

      result = tree;
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitUnary( JCTree.JCUnary tree )
  {
    super.visitUnary( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      Tree.Kind kind = tree.getKind();
      if( kind == Tree.Kind.POSTFIX_INCREMENT || kind == Tree.Kind.POSTFIX_DECREMENT ||
          kind == Tree.Kind.PREFIX_INCREMENT || kind == Tree.Kind.PREFIX_DECREMENT )
      {
        // ++, -- operators not supported with jailbreak access to fields, only direct assignment
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_INCREMENT_OP_NOT_ALLOWED_REFLECTION.get() );
        Types types = Types.instance( ((BasicJavacTask)_tp.getJavacTask()).getContext() );
        tree.type = types.createErrorType( tree.type );
      }
      result = tree;
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitNewClass( JCTree.JCNewClass tree )
  {
    super.visitNewClass( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  public void visitVarDef( JCTree.JCVariableDecl tree )
  {
    super.visitVarDef( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    JCExpression vartype = tree.vartype;
    if( vartype instanceof JCTree.JCAnnotatedType )
    {
      if( ((JCTree.JCAnnotatedType)tree.vartype).getAnnotations().stream()
        .anyMatch( anno -> Jailbreak.class.getTypeName().equals( anno.attribute.type.toString() ) ) )
      {
        // if the type itself is inaccessible and annotated with @Jailbreak, it will be erased to Object
        tree.type = ((JCTree.JCAnnotatedType)tree.vartype).underlyingType.type;
        tree.sym.type = _tp.getSymtab().objectType;
      }
    }
  }

  @Override
  public void visitTypeCast( JCTypeCast tree )
  {
    super.visitTypeCast( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      eraseCompilerGeneratedCast( tree );

      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypeUtil.isStructuralInterface( _tp, tree.type.tsym ) )
    {
      tree.expr = replaceCastExpression( tree.getExpression(), tree.type );
      tree.type = getObjectClass().type;
    }
    result = tree;
  }

  private void eraseCompilerGeneratedCast( JCTypeCast tree )
  {
    // the javac compiler generates casts e.g., for a generic call such as List#get()

    if( TypeUtil.isStructuralInterface( _tp, tree.type.tsym ) && !isConstructProxyCall( tree.getExpression() ) )
    {
      tree.type = getObjectClass().type;
      TreeMaker make = _tp.getTreeMaker();
      tree.clazz = make.Type( getObjectClass().type );
    }
  }

  private boolean isConstructProxyCall( JCExpression expression )
  {
    if( expression instanceof JCTree.JCMethodInvocation )
    {
      // don't erase cast if we generated it here e.g.., for structural call cast on constructProxy

      JCExpression meth = ((JCTree.JCMethodInvocation)expression).meth;
      return meth instanceof JCTree.JCFieldAccess && ((JCTree.JCFieldAccess)meth).getIdentifier().toString().equals( "constructProxy" );
    }
    return expression instanceof JCTypeCast && isConstructProxyCall( ((JCTypeCast)expression).getExpression() );
  }

  /**
   * Replace all extension method call-sites with static calls to extension methods
   */
  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    super.visitApply( tree );

    eraseGenericStructuralVarargs( tree );

    if( _tp.isGenerate() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    Symbol.MethodSymbol method = findExtMethod( tree );
    if( method != null )
    {
      // Replace with extension method call
      result = replaceExtCall( tree, method );
    }
    else if( isStructuralMethod( tree ) )
    {
      // The structural interface method is implemented directly in the type or supertype hierarchy,
      // replace with proxy call
      result = replaceStructuralCall( tree );
    }
    else if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitReference( JCTree.JCMemberReference tree )
  {
    super.visitReference( tree );

    if( isExtensionMethod( tree.sym ) )
    {
      // Method references not supported on extension methods
      
      _tp.report( tree, Diagnostic.Kind.ERROR,
        ExtIssueMsg.MSG_EXTENSION_METHOD_REF_NOT_SUPPORTED.get( tree.sym.flatName() ) );
    }
    else if( isStructuralMethod( tree.sym ) )
    {
      // Method references not supported on structural interface methods

      _tp.report( tree, Diagnostic.Kind.ERROR,
        ExtIssueMsg.MSG_STRUCTURAL_METHOD_REF_NOT_SUPPORTED.get( tree.sym.flatName() ) );
    }
  }

  private boolean isStructuralMethod( Symbol sym )
  {
    if( sym != null && !sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
    {
      if( !isObjectMethod( sym ) )
      {
        return TypeUtil.isStructuralInterface( _tp, sym.owner );
      }
    }
    return false;
  }

  private boolean isExtensionMethod( Symbol sym )
  {
    if( sym instanceof Symbol.MethodSymbol )
    {
      for( Attribute.Compound annotation: sym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
        {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("WeakerAccess")
  public static boolean isJailbreakReceiver( JCTree tree )
  {
    if( tree instanceof JCTree.JCMethodInvocation )
    {
      JCExpression methodSelect = ((JCTree.JCMethodInvocation)tree).getMethodSelect();
      if( methodSelect instanceof JCTree.JCFieldAccess )
      {
        return isJailbreakReceiver( (JCTree.JCFieldAccess)methodSelect );
      }
    }
    else if( tree instanceof JCTree.JCFieldAccess )
    {
      return isJailbreakReceiver( (JCTree.JCFieldAccess)tree );
    }
    else if( tree instanceof JCTree.JCAssign )
    {
      JCExpression lhs = ((JCTree.JCAssign)tree).lhs;
      if( lhs instanceof JCTree.JCFieldAccess )
      {
        return isJailbreakReceiver( lhs );
      }
    }
    else if( tree instanceof JCTree.JCAssignOp )
    {
      JCExpression lhs = ((JCTree.JCAssignOp)tree).lhs;
      if( lhs instanceof JCTree.JCFieldAccess )
      {
        return isJailbreakReceiver( lhs );
      }
    }
    else if( tree instanceof JCTree.JCUnary )
    {
      JCExpression arg = ((JCTree.JCUnary)tree).arg;
      if( arg instanceof JCTree.JCFieldAccess )
      {
        return isJailbreakReceiver( arg );
      }
    }
    else if( tree instanceof JCTree.JCVariableDecl )
    {
      JCExpression initializer = ((JCTree.JCVariableDecl)tree).init;
      if( initializer instanceof JCTree.JCFieldAccess )
      {
        return isJailbreakReceiver( initializer );
      }
    }
    else if( tree instanceof JCTree.JCNewClass )
    {
      return isJailbreakReceiver( (JCTree.JCNewClass)tree );
    }
    return false;
  }

  // called reflectively from manifold core
  @SuppressWarnings("WeakerAccess")
  public static boolean isJailbreakReceiver( JCTree.JCFieldAccess fieldAccess )
  {
    Symbol sym = null;
    JCExpression selected = fieldAccess.selected;
    if( selected instanceof JCTree.JCIdent )
    {
      sym = ((JCTree.JCIdent)selected).sym;
    }
    else if( selected instanceof JCTree.JCMethodInvocation )
    {
      if( ((JCTree.JCMethodInvocation)selected).meth instanceof JCTree.JCFieldAccess )
      {
        sym = ((JCTree.JCFieldAccess)((JCTree.JCMethodInvocation)selected).meth).sym;
      }
      else if( ((JCTree.JCMethodInvocation)selected).meth instanceof JCTree.JCIdent )
      {
        sym = ((JCTree.JCIdent)((JCTree.JCMethodInvocation)selected).meth).sym;
      }
    }

    return isJailbreakSymbol( sym );
  }

  // Called reflectively from manifold core
  @SuppressWarnings("WeakerAccess")
  public static boolean isJailbreakSymbol( Symbol sym )
  {
    if( sym == null )
    {
      return false;
    }

    SymbolMetadata metadata = sym.getMetadata();
    if( metadata == null || (metadata.isTypesEmpty() && metadata.isEmpty()) )
    {
      return false;
    }

    List<Attribute.TypeCompound> typeAttributes = metadata.getTypeAttributes();
    if( !typeAttributes.isEmpty() )
    {
      return typeAttributes.stream()
        .anyMatch( attr -> attr.type.toString().equals( Jailbreak.class.getTypeName() ) );
    }

    List<Attribute.Compound> attributes = metadata.getDeclarationAttributes();
    if( !attributes.isEmpty() )
    {
      return attributes.stream()
        .anyMatch( attr -> attr.type.toString().equals( Jailbreak.class.getTypeName() ) );
    }
    return false;
  }

  private static boolean isJailbreakReceiver( JCTree.JCNewClass newExpr )
  {
    JCExpression classExpr = newExpr.clazz;
    if( classExpr instanceof JCTree.JCAnnotatedType )
    {
      return ((JCTree.JCAnnotatedType)classExpr).annotations.stream()
        .anyMatch( e -> Jailbreak.class.getTypeName().equals( e.attribute.type.toString() ) );
    }
    return false;
  }

  @Override
  public void visitAnnotation( JCTree.JCAnnotation tree )
  {
    super.visitAnnotation( tree );
    if( !Self.class.getTypeName().equals( tree.getAnnotationType().type.tsym.toString() ) )
    {
      return;
    }

    if( !isSelfInMethodDeclOrFieldDecl( tree, tree ) )
    {
      _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_SELF_NOT_ALLOWED_HERE.get() );
    }
    else
    {
      verifySelfOnThis( tree, tree );
    }
  }

  private void verifySelfOnThis( JCTree annotated, JCTree.JCAnnotation selfAnno )
  {
    String fqn;
    if( annotated instanceof JCTree.JCAnnotatedType )
    {
      fqn = ((JCTree.JCAnnotatedType)annotated).getUnderlyingType().type.tsym.getQualifiedName().toString();
    }
    else if( annotated instanceof JCTree.JCMethodDecl )
    {
      fqn = ((JCTree.JCMethodDecl)annotated).getReturnType().type.tsym.getQualifiedName().toString();
    }
    else
    {
      //## todo: shouldn't happen
      return;
    }

    try
    {
      JCTree.JCClassDecl enclosingClass = _tp.getClassDecl( annotated );
      if( !isDeclaringClassOrExtension( annotated, fqn, enclosingClass ) && !fqn.equals( "Array" ) )
      {
        _tp.report( selfAnno, Diagnostic.Kind.ERROR,
          ExtIssueMsg.MSG_SELF_NOT_ON_CORRECT_TYPE.get( fqn, enclosingClass.sym.getQualifiedName() ) );
      }
    }
    catch( Throwable ignore )
    {
    }
  }

  private boolean isDeclaringClassOrExtension( JCTree annotated, String fqn, JCTree.JCClassDecl enclosingClass )
  {
    if( enclosingClass.sym.getQualifiedName().toString().equals( fqn ) ||
        _tp.getTypes().isAssignable( IDynamicJdk.instance().getTypeElement(
          _tp.getContext(), _tp.getCompilationUnit(), fqn ).asType(), enclosingClass.sym.type ) )
    {
      return true;
    }

    return isOnExtensionMethod( annotated, fqn, enclosingClass );
  }

  private boolean isOnExtensionMethod( JCTree annotated, String fqn, JCTree.JCClassDecl enclosingClass )
  {
    if( isExtensionClass( enclosingClass ) )
    {
      String extendedClassName = getExtendedClassName();
      if( extendedClassName != null && extendedClassName.equals( fqn ) )
      {
        JCTree.JCMethodDecl declMethod = findDeclMethod( annotated );
        if( declMethod != null )
        {
          List<JCTree.JCVariableDecl> parameters = declMethod.getParameters();
          for( JCTree.JCVariableDecl param: parameters )
          {
            if( hasAnnotation( param.getModifiers().getAnnotations(), This.class ) )
            {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private JCTree.JCMethodDecl findDeclMethod( Tree annotated )
  {
    if( annotated == null )
    {
      return null;
    }

    if( annotated instanceof JCTree.JCMethodDecl )
    {
      return (JCTree.JCMethodDecl)annotated;
    }
    return findDeclMethod( _tp.getParent( annotated ) );
  }

  private boolean isSelfInMethodDeclOrFieldDecl( Tree tree, JCTree.JCAnnotation anno )
  {
    if( tree == null )
    {
      return false;
    }

    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCTypeParameter )
    {
      // @Self not allowed on type param
      return false;
    }

    if( parent instanceof JCTree.JCMethodDecl )
    {
      // @Self allowed only on return type and parameters of an instance method
      return !((JCTree.JCMethodDecl)parent).getModifiers()
        .getFlags().contains( javax.lang.model.element.Modifier.STATIC ) ||
             isExtensionClass( getEnclosingClass( parent ) );
    }

    if( parent instanceof JCTree.JCVariableDecl )
    {
      Tree container = _tp.getParent( parent );
      if( container instanceof JCTree.JCClassDecl )
      {
        // @Self allowed only on class var, not local var
        return !((JCTree.JCVariableDecl)parent).getModifiers()
          .getFlags().contains( javax.lang.model.element.Modifier.STATIC ) ||
               isExtensionClass( getEnclosingClass( parent ) );
      }
    }
    return isSelfInMethodDeclOrFieldDecl( parent, anno );
  }

  @Override
  public void visitClassDef( JCTree.JCClassDecl tree )
  {
    super.visitClassDef( tree );

    verifyExtensionInterfaces( tree );

    checkExtensionClassError( tree );

    precompileClasses( tree );

    incrementalCompileClasses( tree );
  }

  private void precompileClasses( JCTree.JCClassDecl tree )
  {
    Map<String, Set<String>> typeNames = new HashMap<>();
    for( JCTree.JCAnnotation anno: tree.getModifiers().getAnnotations() )
    {
      if( anno.getAnnotationType().type.toString().equals( Precompile.class.getCanonicalName() ) )
      {
        getTypesToCompile( anno, typeNames );
      }
    }

    if( !typeNames.isEmpty() )
    {
      precompile( typeNames );
    }
  }

  private void getTypesToCompile( JCTree.JCAnnotation precompileAnno, Map<String, Set<String>> typeNames )
  {
    Attribute.Compound attribute = precompileAnno.attribute;
    if( attribute == null )
    {
      return;
    }

    String typeManifoldClassName = null;
    String regex = ".*";
    String ext = "*";
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair: attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      switch( argName.toString() )
      {
        case "typeManifold":
          typeManifoldClassName = pair.snd.getValue().toString();
          break;
        case "fileExtension":
          ext = pair.snd.getValue().toString();
          break;
        case "typeNames":
          regex = pair.snd.getValue().toString();
          break;
      }
    }

    addToPrecompile( typeNames, typeManifoldClassName, ext, regex );
  }

  private void addToPrecompile( Map<String, Set<String>> typeNames, String typeManifoldClassName, String ext, String regex )
  {
    if( typeManifoldClassName != null )
    {
      Set<String> regexes = typeNames.computeIfAbsent( typeManifoldClassName, tm -> new HashSet<>() );
      regexes.add( regex );
    }
    else
    {
      boolean all = "*".equals( ext );
      _tp.getHost().getSingleModule().getTypeManifolds().stream()
        .filter( tm -> tm.getContributorKind() != ContributorKind.Supplemental )
        .forEach( tm ->
          {
            boolean match = !all && tm.handlesFileExtension( ext );
            if( all || match )
            {
              String classname = tm.getClass().getTypeName();
              Set<String> regexes = typeNames.computeIfAbsent( classname, e -> new HashSet<>() );
              regexes.add( regex );
            }
          } );
    }
  }

  private void precompile( Map<String, Set<String>> typeNames )
  {
    for( ITypeManifold tm: _tp.getHost().getSingleModule().getTypeManifolds() )
    {
      for( Map.Entry<String, Set<String>> entry: typeNames.entrySet() )
      {
        String typeManifoldClassName = entry.getKey();
        if( tm.getClass().getName().equals( typeManifoldClassName ) )
        {
          Collection<String> namesToPrecompile = computeNamesToPrecompile( tm.getAllTypeNames(), entry.getValue() );
          for( String fqn: namesToPrecompile )
          {
            // This call surfaces the type in the compiler.  If compiling in "static" mode, this means
            // the type will be compiled to disk.
            IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), fqn );
          }
        }
      }
    }
  }

  private Collection<String> computeNamesToPrecompile( Collection<String> allTypeNames, Set<String> regexes )
  {
    Set<String> matchingTypes = new HashSet<>();
    for( String fqn: allTypeNames )
    {
      if( regexes.stream().anyMatch( fqn::matches ) )
      {
        matchingTypes.add( fqn );
      }
    }
    return matchingTypes;
  }

  private void incrementalCompileClasses( JCTree.JCClassDecl tree )
  {
    if( _tp.isGenerate() )
    {
      return;
    }

    Set<Object> drivers = findDrivers( tree );

    // Keep track of Manifold types compiled, for hotswap compile drivers
    addCompiledResourceTypeByFile( drivers, tree );

    // Ensure modified resource files are compiled, stemming from incremental compilation only
    incrementalCompile( drivers );
  }

  private Set<Object> findDrivers( JCTree.JCClassDecl tree )
  {
    Set<Object> drivers = new HashSet<>();
    for( JCTree.JCAnnotation anno: tree.getModifiers().getAnnotations() )
    {
      if( anno.getAnnotationType().type.toString().equals( IncrementalCompile.class.getCanonicalName() ) )
      {
        getIncrementalCompileDrivers( anno, drivers );
      }
    }
    _tp.addDrivers( drivers );
    addFileFragmentDriver();
    return _tp.getDrivers();
  }

  private void addFileFragmentDriver()
  {
    Class<?> type = ReflectUtil.type( "manifold.ij.jps.IjFileFragmentIncrementalCompileDriver" );
    if( type != null )
    {
      Object driver = ReflectUtil.method( type, "getInstance" ).invokeStatic();
      _tp.addDrivers( Collections.singleton( driver ) );
    }
  }

  private void getIncrementalCompileDrivers( JCTree.JCAnnotation anno, Set<Object> drivers )
  {
    Attribute.Compound attribute = anno.attribute;
    if( attribute == null )
    {
      return;
    }

    String fqnDriver = null;
    Integer driverId = null;
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair: attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      if( argName.toString().equals( "driverInstance" ) )
      {
        driverId = (int)pair.snd.getValue();
      }
      else if( argName.toString().equals( "driverClass" ) )
      {
        fqnDriver = (String)pair.snd.getValue();
      }
    }

    if( driverId != null )
    {
      Object driver = ReflectUtil.method( fqnDriver, "getInstance", int.class ).invokeStatic( driverId );
      drivers.add( driver );
    }
  }

  private void incrementalCompile( Set<Object> drivers )
  {
    for( Object driver: drivers )
    {
      //noinspection unchecked
      Collection<File> changedFiles = (Collection<File>)ReflectUtil.method( driver, "getChangedFiles" ).invoke();
      if( changedFiles == null || changedFiles.isEmpty() )
      {
        // nothing to compile
        return;
      }

      IManifoldHost host = _tp.getHost();
      Set<IFile> changes = changedFiles.stream().map( ( File f ) -> host.getFileSystem().getIFile( f ) )
        .collect( Collectors.toSet() );
      for( ITypeManifold tm: host.getSingleModule().getTypeManifolds() )
      {
        for( IFile file: changes )
        {
          Set<String> types = Arrays.stream( tm.getTypesForFile( file ) ).collect( Collectors.toSet() );
          if( types.size() > 0 )
          {
            ReflectUtil.method( driver, "mapTypesToFile", Set.class, File.class ).invoke( types, file.toJavaFile() );
            for( String fqn: types )
            {
              // This call surfaces the type in the compiler.  If compiling in "static" mode, this means
              // the type will be compiled to disk.
              Symbol.ClassSymbol classSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), fqn );
              assert classSym != null;
              changedFiles.remove( file.toJavaFile() );
            }
          }
        }
      }
    }
  }

  private void addCompiledResourceTypeByFile( Set<Object> drivers, JCTree.JCClassDecl tree )
  {
    if( tree.sym == null )
    {
      return;
    }

    Name qualifiedName = tree.sym.getQualifiedName();
    if( qualifiedName == null )
    {
      return;
    }

    JavaFileObject sourcefile = tree.sym.sourcefile;
    if( !(sourcefile instanceof GeneratedJavaStubFileObject) )
    {
      // not a Manifold generated type
      return;
    }

    Set<IFile> resourceFiles = ((GeneratedJavaStubFileObject)sourcefile).getResourceFiles();
    File file;
    for( IFile ifile: resourceFiles )
    {
      try
      {
        while( ifile instanceof IFileFragment )
        {
          ifile = ((IFileFragment)ifile).getEnclosingFile();
        }
        file = ifile.toJavaFile();
      }
      catch( Exception e )
      {
        continue;
      }

      Map<File, Set<String>> typesCompiledByFile = _tp.getTypesCompiledByFile();
      Set<String> types = typesCompiledByFile.get( file );
      if( types == null )
      {
        typesCompiledByFile.put( file, types = new ConcurrentHashSet<>() );
      }
      StringBuilder sb = new StringBuilder();
      make$name( tree.sym, sb );
      types.add( sb.toString() );
    }

    addIndirectCompiledTypesToBuildForMapping( drivers );
  }

  private void make$name( Symbol.ClassSymbol sym, StringBuilder sb )
  {
    Symbol enclosingElement = sym.getEnclosingElement();
    if( enclosingElement instanceof Symbol.ClassSymbol )
    {
      make$name( (Symbol.ClassSymbol)enclosingElement, sb );
      sb.append( '$' ).append( sym.getSimpleName() );
    }
    else
    {
      sb.append( sym.getQualifiedName().toString() );
    }
  }

  private void addIndirectCompiledTypesToBuildForMapping( Set<Object> drivers )
  {
    Map<File, Set<String>> typesCompiledByFile = _tp.getTypesCompiledByFile();
    if( typesCompiledByFile.isEmpty() )
    {
      // nothing to add
      return;
    }

    for( Object driver: drivers )
    {
      // Keep track of compiled Manifold types during a Rebuild.
      //
      // Generally, since Manifold types are magically added to the build as
      // they are referenced, they need to be mapped in the JPS compilation
      // process to support hotswap debugging, etc.

      //noinspection unchecked
      Map<File, Set<String>> map = (Map)ReflectUtil.method( driver, "getTypesToFile" ).invoke();
      typesCompiledByFile.forEach( ( file, types ) -> {
        Set<String> existingTypes = map.get( file );
        if( existingTypes != null )
        {
          existingTypes.addAll( types );
        }
        else
        {
          map.put( file, types );
        }
      } );
      typesCompiledByFile.clear();
    }
  }

  private void verifyExtensionInterfaces( JCTree.JCClassDecl tree )
  {
    if( !hasAnnotation( tree.getModifiers().getAnnotations(), Extension.class ) )
    {
      return;
    }

    outer:
    for( JCExpression iface: tree.getImplementsClause() )
    {
      final Symbol.TypeSymbol ifaceSym = iface.type.tsym;
      if( ifaceSym == _tp.getSymtab().objectType.tsym )
      {
        continue;
      }

      for( Attribute.Compound anno: ifaceSym.getAnnotationMirrors() )
      {
        if( anno.type.toString().equals( Structural.class.getName() ) )
        {
          continue outer;
        }
      }
      // extension interfaces must be structural
      _tp.report( iface, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_ONLY_STRUCTURAL_INTERFACE_ALLOWED_HERE.get( iface.toString() ) );
    }
  }

  private boolean shouldProcessForGeneration()
  {
    return _bridgeMethod;
  }

  private boolean isBridgeMethod( JCTree.JCMethodDecl tree )
  {
    long modifiers = tree.getModifiers().flags;
    return (Flags.BRIDGE & modifiers) != 0;
  }

  private Type eraseStructureType( Type type )
  {
    return new StructuralTypeEraser( this ).visit( type );
  }

  private boolean isReceiver( JCTree tree )
  {
    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCFieldAccess )
    {
      return ((JCTree.JCFieldAccess)parent).getExpression() == tree;
    }
    return false;
  }

  Symbol.ClassSymbol getObjectClass()
  {
    Symtab symbols = Symtab.instance( _tp.getContext() );
    return (Symbol.ClassSymbol)symbols.objectType.tsym;
  }

  private void eraseGenericStructuralVarargs( JCTree.JCMethodInvocation tree )
  {
    if( tree.varargsElement instanceof Type.ClassType && TypeUtil.isStructuralInterface( _tp, tree.varargsElement.tsym ) )
    {
      tree.varargsElement = _tp.getSymtab().objectType;
    }
  }

  /**
   * Issue errors/warnings if an extension method violates extension method grammar or conflicts with an existing method
   */
  @Override
  public void visitMethodDef( JCTree.JCMethodDecl tree )
  {
    if( isBridgeMethod( tree ) )
    {
      // we process bridge methods during Generation, since they don't exist prior to Generation
      _bridgeMethod = true;
    }
    try
    {
      super.visitMethodDef( tree );
    }
    finally
    {
      _bridgeMethod = false;
    }

    if( _tp.isGenerate() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( tree.sym.owner.isAnonymous() )
    {
      // Keep track of anonymous classes so we can process any bridge methods added to them
      JCTree.JCClassDecl anonymousClassDef = (JCTree.JCClassDecl)_tp.getTreeUtil().getTree( tree.sym.owner );
      _tp.preserveInnerClassForGenerationPhase( anonymousClassDef );
    }

    verifyExtensionMethod( tree );
    result = tree;
  }

  private void checkExtensionClassError( JCTree.JCClassDecl typeDecl )
  {
    JavacPlugin javacPlugin = JavacPlugin.instance();
    if( javacPlugin == null )
    {
      return;
    }

    if( !isExtensionClass( typeDecl ) )
    {
      return;
    }

    String extendedFqn = getExtendedClassName();
    if( javacPlugin.getJavaInputFiles().stream().anyMatch(
      pair -> !(pair.getSecond() instanceof GeneratedJavaStubFileObject) && pair.getFirst().equals( extendedFqn ) ) )
    {
      _tp.report( typeDecl, Diagnostic.Kind.WARNING, ExtIssueMsg.MSG_CANNOT_EXTEND_SOURCE_FILE.get( extendedFqn ) );
    }
  }

  private void verifyExtensionMethod( JCTree.JCMethodDecl tree )
  {
//    if( JavacPlugin.instance() == null )
//    {
//      // don't perform verification at runtime, slow
//      return;
//    }

    if( !isExtensionClass( _tp.getParent( tree ) ) )
    {
      return;
    }

    String extendedClassName = getExtendedClassName();
    if( extendedClassName == null )
    {
      return;
    }

    boolean thisAnnoFound = false;
    List<JCTree.JCVariableDecl> parameters = tree.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      JCTree.JCVariableDecl param = parameters.get( i );
      long methodModifiers = tree.getModifiers().flags;
      if( hasAnnotation( param.getModifiers().getAnnotations(), This.class ) )
      {
        thisAnnoFound = true;

        if( i != 0 )
        {
          _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_THIS_FIRST.get() );
        }

        if( !(param.type.tsym instanceof Symbol.ClassSymbol) || !((Symbol.ClassSymbol)param.type.tsym).className().equals( extendedClassName ) )
        {
          Symbol.ClassSymbol extendClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), extendedClassName );
          if( extendClassSym != null &&
              !TypeUtil.isStructuralInterface( _tp, extendClassSym ) && // an extended class could be made a structural interface which results in Object as @This param, ignore this
              !TypeUtil.isAssignableFromErased( _tp.getContext(), extendClassSym, param.type.tsym ) )
          {
            _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_EXPECTING_TYPE_FOR_THIS.get( extendedClassName ) );
          }
        }
      }
      else if( i == 0 &&
               Modifier.isStatic( (int)methodModifiers ) &&
               Modifier.isPublic( (int)methodModifiers ) &&
               param.type.toString().equals( extendedClassName ) )
      {
        _tp.report( param, Diagnostic.Kind.WARNING, ExtIssueMsg.MSG_MAYBE_MISSING_THIS.get() );
      }
    }

    if( thisAnnoFound || hasAnnotation( tree.getModifiers().getAnnotations(), Extension.class ) )
    {
      long methodModifiers = tree.getModifiers().flags;
      if( !Modifier.isStatic( (int)methodModifiers ) )
      {
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_MUST_BE_STATIC.get( tree.getName() ) );
      }

      if( Modifier.isPrivate( (int)methodModifiers ) )
      {
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_MUST_NOT_BE_PRIVATE.get( tree.getName() ) );
      }
    }
  }

  private String getExtendedClassName()
  {
    String extendedClassName = _tp.getCompilationUnit().getPackageName().toString();
    int iExt = extendedClassName.indexOf( ExtensionManifold.EXTENSIONS_PACKAGE + '.' );
    if( iExt < 0 )
    {
      return null;
    }

    extendedClassName = extendedClassName.substring( iExt + ExtensionManifold.EXTENSIONS_PACKAGE.length() + 1 );
    return extendedClassName;
  }

  private boolean isExtensionClass( Tree parent )
  {
    if( parent instanceof JCTree.JCClassDecl )
    {
      return hasAnnotation( ((JCTree.JCClassDecl)parent).getModifiers().getAnnotations(), Extension.class );
    }
    return false;
  }

  private JCTree.JCClassDecl getEnclosingClass( Tree tree )
  {
    if( tree == null )
    {
      return null;
    }
    if( tree instanceof JCTree.JCClassDecl )
    {
      return (JCTree.JCClassDecl)tree;
    }
    return getEnclosingClass( _tp.getParent( tree ) );
  }

  private boolean hasAnnotation( List<JCTree.JCAnnotation> annotations, Class<? extends Annotation> annoClass )
  {
    for( JCTree.JCAnnotation anno: annotations )
    {
      if( anno.getAnnotationType().type.toString().equals( annoClass.getCanonicalName() ) )
      {
        return true;
      }
    }
    return false;
  }

  private JCTree replaceStructuralCall( JCTree.JCMethodInvocation theCall )
  {
    JCExpression methodSelect = theCall.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      Symtab symbols = _tp.getSymtab();
      Names names = Names.instance( _tp.getContext() );
      Symbol.ClassSymbol reflectMethodClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), RuntimeMethods.class.getName() );
      Symbol.MethodSymbol makeInterfaceProxyMethod = resolveMethod( theCall.pos(), names.fromString( "constructProxy" ), reflectMethodClassSym.type,
        List.from( new Type[]{symbols.objectType, symbols.classType} ) );

      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCExpression thisArg = m.selected;

      ArrayList<JCExpression> newArgs = new ArrayList<>();
      newArgs.add( thisArg );
      JCTree.JCFieldAccess ifaceClassExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, thisArg.type.tsym.getQualifiedName().toString() + ".class" );
      ifaceClassExpr.type = symbols.classType;
      ifaceClassExpr.sym = symbols.classType.tsym;
      assignTypes( ifaceClassExpr.selected, thisArg.type.tsym );
      newArgs.add( ifaceClassExpr );

      JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, RuntimeMethods.class.getName() + ".constructProxy" ), List.from( newArgs ) );
      makeProxyCall.setPos( theCall.pos );
      makeProxyCall.type = thisArg.type;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)makeProxyCall.getMethodSelect();
      newMethodSelect.sym = makeInterfaceProxyMethod;
      newMethodSelect.type = makeInterfaceProxyMethod.type;
      assignTypes( newMethodSelect.selected, reflectMethodClassSym );

      JCTypeCast cast = make.TypeCast( thisArg.type, makeProxyCall );
      cast.type = thisArg.type;

      ((JCTree.JCFieldAccess)theCall.meth).selected = cast;
      return theCall;
    }
    return null;
  }

  private JCExpression replaceCastExpression( JCExpression expression, Type type )
  {
    TreeMaker make = _tp.getTreeMaker();
    Symtab symbols = _tp.getSymtab();
    Names names = Names.instance( _tp.getContext() );
    Symbol.ClassSymbol reflectMethodClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), RuntimeMethods.class.getName() );

    Symbol.MethodSymbol makeInterfaceProxyMethod = resolveMethod( expression.pos(), names.fromString( "assignStructuralIdentity" ), reflectMethodClassSym.type,
      List.from( new Type[]{symbols.objectType, symbols.classType} ) );

    JavacElements javacElems = _tp.getElementUtil();
    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( expression );
    JCTree.JCFieldAccess ifaceClassExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, type.tsym.getQualifiedName().toString() + ".class" );
    ifaceClassExpr.type = symbols.classType;
    ifaceClassExpr.sym = symbols.classType.tsym;
    assignTypes( ifaceClassExpr.selected, type.tsym );
    newArgs.add( ifaceClassExpr );

    JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, RuntimeMethods.class.getName() + ".assignStructuralIdentity" ), List.from( newArgs ) );
    makeProxyCall.type = symbols.objectType;
    makeProxyCall.setPos( expression.pos );
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)makeProxyCall.getMethodSelect();
    newMethodSelect.sym = makeInterfaceProxyMethod;
    newMethodSelect.type = makeInterfaceProxyMethod.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    JCTypeCast castCall = make.TypeCast( symbols.objectType, makeProxyCall );
    castCall.type = symbols.objectType;

    return castCall;

  }

  private JCTree replaceExtCall( JCTree.JCMethodInvocation tree, Symbol.MethodSymbol method )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCExpression thisArg = m.selected;
      String extensionFqn = method.getEnclosingElement().asType().tsym.toString();
      m.selected = memberAccess( make, javacElems, extensionFqn );
      BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask();
      Symbol.ClassSymbol extensionClassSym = ClassSymbols.instance( _sp.getModule() ).getClassSymbol( javacTask, _tp, extensionFqn ).getFirst();
      assignTypes( m.selected, extensionClassSym );
      m.sym = method;
      m.type = method.type;

      if( !isStatic )
      {
        ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
        newArgs.add( 0, thisArg );
        tree.args = List.from( newArgs );
      }
      return tree;
    }
    else if( methodSelect instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent m = (JCTree.JCIdent)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      String extensionFqn = method.getEnclosingElement().asType().tsym.toString();

      ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
      if( !isStatic )
      {
        JCExpression thisArg = make.This( _tp.getClassDecl( tree ).type );
        newArgs.add( 0, thisArg );
      }
      JCTree.JCMethodInvocation extCall =
        make.Apply( List.nil(),
          memberAccess( make, javacElems, extensionFqn ),
          List.from( newArgs ) );
      extCall.setPos( tree.pos );
      extCall.type = tree.type;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)extCall.getMethodSelect();
      newMethodSelect.sym = method;
      newMethodSelect.type = method.type;
      assignTypes( newMethodSelect.selected, method.owner );
      return extCall;
    }
    return tree;
  }

  private JCTree.JCMethodInvocation replaceWithReflection( JCTree.JCMethodInvocation tree )
  {
    //## todo: maybe try to avoid reflection if the method is accessible -- at least check if the method and its enclosing nest of classes are all public

    Type type = tree.getMethodSelect().type;
    if( type instanceof Type.ErrorType )
    {
      // No such field/method or wrong params
      return tree;
    }

    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
      if( !(m.sym instanceof Symbol.MethodSymbol) )
      {
        return tree;
      }
      Type returnType = ((Symbol.MethodSymbol)m.sym).getReturnType();
      Symbol.MethodSymbol reflectMethodSym = findReflectUtilMethod( tree, returnType, isStatic );

      List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)m.sym).getParameters();
      ArrayList<JCExpression> paramTypes = new ArrayList<>();
      for( Symbol.VarSymbol param: parameters )
      {
        JCExpression classExpr = makeClassExpr( tree, param.type );
        paramTypes.add( classExpr );
      }
      Symtab symTab = _tp.getSymtab();
      JCTree.JCNewArray paramTypesArray = make.NewArray(
        make.Type( symTab.classType ), List.nil(), List.from( paramTypes ) );
      paramTypesArray.type = new Type.ArrayType( symTab.classType, symTab.arrayClass );

      JCTree.JCNewArray argsArray = make.NewArray(
        make.Type( symTab.objectType ), List.nil(), tree.getArguments() );
      argsArray.type = new Type.ArrayType( symTab.objectType, symTab.arrayClass );

      ArrayList<JCExpression> newArgs = new ArrayList<>();
      newArgs.add( isStatic ? makeClassExpr( tree, m.selected.type ) : m.selected ); // receiver or class
      newArgs.add( make.Literal( m.sym.flatName().toString() ) ); // method name
      newArgs.add( paramTypesArray ); // param types
      newArgs.add( argsArray ); // args

      Symbol.ClassSymbol reflectMethodClassSym =
        IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

      JCTree.JCMethodInvocation reflectCall =
        make.Apply( List.nil(),
          memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
          List.from( newArgs ) );
      reflectCall.setPos( tree.pos );
      reflectCall.type = returnType;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
      newMethodSelect.sym = reflectMethodSym;
      newMethodSelect.type = reflectMethodSym.type;
      assignTypes( newMethodSelect.selected, reflectMethodClassSym );

      return reflectCall;
    }
    return tree;
  }

  private JCTree replaceWithReflection( JCTree.JCFieldAccess tree )
  {
    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    boolean isStatic = tree.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
    if( tree.sym instanceof Symbol.MethodSymbol )
    {
      return tree;
    }

    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCAssign && ((JCTree.JCAssign)parent).lhs == tree ||
        parent instanceof JCTree.JCAssignOp && ((JCTree.JCAssignOp)parent).lhs == tree )
    {
      // handled in visitAssign() or visitAssignOp()
      return tree;
    }

    if( parent instanceof JCTree.JCUnary && ((JCTree.JCUnary)parent).arg == tree )
    {
      Tree.Kind kind = parent.getKind();

      if( kind != Tree.Kind.UNARY_MINUS && kind != Tree.Kind.UNARY_PLUS &&
          kind != Tree.Kind.LOGICAL_COMPLEMENT && kind != Tree.Kind.BITWISE_COMPLEMENT )
      {
        // supporting -, +, !, ~  not supporting --, ++
        _tp.report( (JCTree)parent, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_INCREMENT_OP_NOT_ALLOWED_REFLECTION.get() );
        return tree;
      }
    }

    Type type = tree.sym.type;
    if( type instanceof Type.ErrorType )
    {
      // No such field/method
      return tree;
    }

    Symbol.MethodSymbol reflectMethodSym = findFieldAccessReflectUtilMethod( tree, type, isStatic, false );

    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( isStatic ? makeClassExpr( tree, tree.selected.type ) : tree.selected ); // receiver or class
    newArgs.add( make.Literal( tree.sym.flatName().toString() ) ); // field name

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    JCTree.JCMethodInvocation reflectCall =
      make.Apply( List.nil(),
        memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
        List.from( newArgs ) );
    reflectCall.setPos( tree.pos );
    reflectCall.type = type;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
    newMethodSelect.sym = reflectMethodSym;
    newMethodSelect.type = reflectMethodSym.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return reflectCall;
  }

  private JCTree replaceWithReflection( JCTree.JCAssign assignTree )
  {
    JCTree.JCFieldAccess tree = (JCTree.JCFieldAccess)assignTree.lhs;

    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    boolean isStatic = tree.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
    if( tree.sym instanceof Symbol.MethodSymbol )
    {
      return assignTree;
    }

    Type type = tree.sym.type;
    Symbol.MethodSymbol reflectMethodSym = findFieldAccessReflectUtilMethod( tree, type, isStatic, true );

    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( isStatic ? makeClassExpr( tree, tree.selected.type ) : tree.selected ); // receiver or class
    newArgs.add( make.Literal( tree.sym.flatName().toString() ) ); // field name
    newArgs.add( assignTree.rhs ); // field value

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    JCTree.JCMethodInvocation reflectCall =
      make.Apply( List.nil(),
        memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
        List.from( newArgs ) );
    reflectCall.setPos( tree.pos );
    reflectCall.type = type;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
    newMethodSelect.sym = reflectMethodSym;
    newMethodSelect.type = reflectMethodSym.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return reflectCall;
  }

  private JCTree replaceWithReflection( JCTree.JCNewClass tree )
  {
    if( tree.constructor == null )
    {
      return tree;
    }

    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    Type type = ((JCTree.JCAnnotatedType)tree.clazz).underlyingType.type;

    if( tree.constructor instanceof Symbol.ClassSymbol )
    {
      //assert tree.constructor.kind == com.sun.tools.javac.code.Kinds.ERR;
      return tree;
    }

    List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)tree.constructor).getParameters();
    ArrayList<JCExpression> paramTypes = new ArrayList<>();
    for( Symbol.VarSymbol param: parameters )
    {
      paramTypes.add( makeClassExpr( tree, param.type ) );
    }
    Symtab symTab = _tp.getSymtab();
    JCTree.JCNewArray paramTypesArray = make.NewArray(
      make.Type( symTab.classType ), List.nil(), List.from( paramTypes ) );
    paramTypesArray.type = new Type.ArrayType( symTab.classType, symTab.arrayClass );

    JCTree.JCNewArray argsArray = make.NewArray(
      make.Type( symTab.objectType ), List.nil(), tree.getArguments() );
    argsArray.type = new Type.ArrayType( symTab.objectType, symTab.arrayClass );

    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( makeClassExpr( tree, type ) ); // the class
    newArgs.add( paramTypesArray ); // param types
    newArgs.add( argsArray ); // args


    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    Symbol.MethodSymbol reflectMethodSym = findReflectUtilConstructor( tree );
    JCTree.JCMethodInvocation reflectCall =
      make.Apply( List.nil(),
        memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
        List.from( newArgs ) );
    reflectCall.setPos( tree.pos );
    reflectCall.type = type;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
    newMethodSelect.sym = reflectMethodSym;
    newMethodSelect.type = reflectMethodSym.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return reflectCall;
  }

  private JCExpression makeClassExpr( JCTree tree, Type type )
  {
    BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask();
    Types types = Types.instance( javacTask.getContext() );
    type = types.erasure( type );

    JCExpression classExpr;
    if( type.isPrimitive() ||
        (JreUtil.isJava8() && type.tsym.getModifiers().contains( javax.lang.model.element.Modifier.PUBLIC )) )
    {
      // class is publicly accessible, assume we can use class literal
      classExpr = _tp.getTreeMaker().ClassLiteral( type );
    }
    else
    {
      // generate `ReflectUtil.type( typeName )`
      classExpr = classForNameCall( type, tree );
    }
    return classExpr;
  }

  private JCExpression classForNameCall( Type type, JCTree tree )
  {
    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    JCTree.JCMethodInvocation typeCall = make.Apply( List.nil(),
      memberAccess( make, javacElems, ReflectUtil.class.getName() + ".type" ),
      List.of( make.Literal( type.tsym.getQualifiedName().toString() ) ) );
    typeCall.setPos( Position.NOPOS );
    typeCall.type = _tp.getSymtab().classType;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)typeCall.getMethodSelect();

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectUtil.class.getName() );
    Symbol.MethodSymbol typeMethodSymbol = resolveMethod( tree.pos(), Names.instance( _tp.getContext() ).fromString( "type" ), reflectMethodClassSym.type, List.of( _tp.getSymtab().stringType ) );
    newMethodSelect.sym = typeMethodSymbol;
    newMethodSelect.type = typeMethodSymbol.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return typeCall;
  }

  private void assignTypes( JCExpression m, Symbol symbol )
  {
    if( m instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)m;
      fieldAccess.sym = symbol;
      fieldAccess.type = symbol.type;
      assignTypes( fieldAccess.selected, symbol.owner );
    }
    else if( m instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent ident = (JCTree.JCIdent)m;
      ident.sym = symbol;
      ident.type = symbol.type;
    }
  }

  private Symbol.MethodSymbol findExtMethod( JCTree.JCMethodInvocation tree )
  {
    Symbol sym = null;
    if( tree.meth instanceof JCTree.JCFieldAccess )
    {
      sym = ((JCTree.JCFieldAccess)tree.meth).sym;
    }
    else if( tree.meth instanceof JCTree.JCIdent )
    {
      sym = ((JCTree.JCIdent)tree.meth).sym;
    }

    if( sym == null || !sym.hasAnnotations() )
    {
      return null;
    }

    for( Attribute.Compound annotation: sym.getAnnotationMirrors() )
    {
      if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
      {
        String extensionClass = (String)annotation.values.get( 0 ).snd.getValue();
        boolean isStatic = (boolean)annotation.values.get( 1 ).snd.getValue();
        BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask(); //JavacHook.instance() != null ? (JavacTaskImpl)JavacHook.instance().getJavacTask_PlainFileMgr() : ClassSymbols.instance( _sp.getModule() ).getJavacTask_PlainFileMgr();
        Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( _sp.getModule() ).getClassSymbol( javacTask, _tp, extensionClass );
        if( classSymbol == null )
        {
          // In module mode if a package in another module is not exported, classes in the package
          // will not be accessible to other modules, hence the null classSymbol
          continue;
        }

        Symbol.ClassSymbol extClassSym = classSymbol.getFirst();
        if( extClassSym == null )
        {
          // This can happen during bootstrapping with Dark Java classes from Manifold itself
          // So we short-circuit that here (ManClassFinder_9 or any other darkj class used during bootstrapping doesn't really need to use extensions)
          return null;
        }
        Types types = Types.instance( javacTask.getContext() );
        outer:
        for( Symbol elem: IDynamicJdk.instance().getMembers( extClassSym ) )
        {
          if( elem instanceof Symbol.MethodSymbol && elem.flatName().toString().equals( sym.name.toString() ) )
          {
            Symbol.MethodSymbol extMethodSym = (Symbol.MethodSymbol)elem;
            List<Symbol.VarSymbol> extParams = extMethodSym.getParameters();
            List<Symbol.VarSymbol> calledParams = ((Symbol.MethodSymbol)sym).getParameters();
            int thisOffset = isStatic ? 0 : 1;
            if( extParams.size() - thisOffset != calledParams.size() )
            {
              continue;
            }
            for( int i = thisOffset; i < extParams.size(); i++ )
            {
              Symbol.VarSymbol extParam = extParams.get( i );
              Symbol.VarSymbol calledParam = calledParams.get( i - thisOffset );
              if( !types.isSameType( types.erasure( extParam.type ), types.erasure( calledParam.type ) ) )
              {
                continue outer;
              }
            }
            return extMethodSym;
          }
        }
      }
    }
    return null;
  }

  //## todo: cache these
  private Symbol.MethodSymbol findReflectUtilMethod( JCTree tree, Type returnType, boolean isStatic )
  {
    String name = "invoke" + (isStatic ? "Static" : "") + '_' + typeForReflect( returnType );

    Symtab symtab = _tp.getSymtab();
    Type.ArrayType classArrayType = new Type.ArrayType( symtab.classType, symtab.arrayClass );
    Type.ArrayType objectArrayType = new Type.ArrayType( symtab.objectType, symtab.arrayClass );
    List<Type> paramTypes;
    if( isStatic )
    {
      paramTypes = List.of( symtab.classType, symtab.stringType, classArrayType, objectArrayType );
    }
    else
    {
      paramTypes = List.of( symtab.objectType, symtab.stringType, classArrayType, objectArrayType );
    }

    Names names = Names.instance( _tp.getContext() );
    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    return resolveMethod( tree.pos(), names.fromString( name ), reflectMethodClassSym.type, paramTypes );
  }

  //## todo: cache these
  private Symbol.MethodSymbol findFieldAccessReflectUtilMethod( JCTree tree, Type type, boolean isStatic, boolean setter )
  {
    String name = (setter ? "set" : "get") + "Field" + (isStatic ? "Static" : "") + '_' + typeForReflect( type );

    Symtab symtab = _tp.getSymtab();
    List<Type> paramTypes;
    if( setter )
    {
      paramTypes = List.of( isStatic ? symtab.classType : symtab.objectType, symtab.stringType, type );
    }
    else
    {
      paramTypes = List.of( isStatic ? symtab.classType : symtab.objectType, symtab.stringType );
    }

    Names names = Names.instance( _tp.getContext() );
    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    return resolveMethod( tree.pos(), names.fromString( name ), reflectMethodClassSym.type, paramTypes );
  }

  //## todo: cache this
  private Symbol.MethodSymbol findReflectUtilConstructor( JCTree.JCNewClass tree )
  {
    Symtab symtab = _tp.getSymtab();
    Type.ArrayType classArrayType = new Type.ArrayType( symtab.classType, symtab.arrayClass );
    Type.ArrayType objectArrayType = new Type.ArrayType( symtab.objectType, symtab.arrayClass );
    List<Type> paramTypes = List.of( symtab.classType, classArrayType, objectArrayType );

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    Names names = Names.instance( _tp.getContext() );
    return resolveMethod( tree.pos(), names.fromString( "construct" ), reflectMethodClassSym.type, paramTypes );
  }

  private String typeForReflect( Type returnType )
  {
    if( returnType.isPrimitive() )
    {
      return returnType.tsym.getSimpleName().toString();
    }
    return "Object";
  }

  private boolean isStructuralMethod( JCTree.JCMethodInvocation tree )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      if( m.sym != null && !m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
      {
        if( !isObjectMethod( m.sym ) )
        {
          JCExpression thisArg = m.selected;
          return TypeUtil.isStructuralInterface( _tp, thisArg.type.tsym );
        }
      }
    }
    return false;
  }

  private boolean isObjectMethod( Symbol sym )
  {
    return sym.owner != null && sym.owner.type == _tp.getSymtab().objectType;
  }

  private JCExpression memberAccess( TreeMaker make, JavacElements javacElems, String path )
  {
    return memberAccess( make, javacElems, path.split( "\\." ) );
  }

  private JCExpression memberAccess( TreeMaker make, JavacElements node, String... components )
  {
    JCExpression expr = make.Ident( node.getName( components[0] ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, node.getName( components[i] ) );
    }
    return expr;
  }

  private Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    return resolveMethod( pos, _tp.getContext(), (JCTree.JCCompilationUnit)_tp.getCompilationUnit(), name, qual, args );
  }

  private static Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Context ctx, JCTree.JCCompilationUnit compUnit, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = compUnit;
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }
}
