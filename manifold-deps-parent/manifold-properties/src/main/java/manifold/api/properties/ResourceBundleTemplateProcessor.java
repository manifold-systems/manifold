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

package manifold.api.properties;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import manifold.api.type.ICompilerComponent;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.ManAttr;
import manifold.internal.javac.TypeProcessor;

public class ResourceBundleTemplateProcessor extends TreeTranslator implements ICompilerComponent, TaskListener
{
  private Context _context;
  private TaskEvent _taskEvent;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _context = javacTask.getContext();
    javacTask.addTaskListener( this );
  }

  @Override
  public void started( TaskEvent e )
  {
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ANALYZE
        || e.getSourceFile() instanceof manifold.internal.javac.GeneratedJavaStubFileObject )
    {
      return;
    }
    try {
      _taskEvent = e;
      ensureInitialized( _taskEvent );
      for ( Tree tree : e.getCompilationUnit().getTypeDecls() ) {
        if ( !( tree instanceof JCTree.JCClassDecl ) ) {
          continue;
        }
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) tree;
        classDecl.accept( new Analyze_Finish() );
      }
    } finally {
      _taskEvent = null;
    }
  }

  // Replace field refs with getter/setter calls:
  //
  // foo.bar          ==>  foo.getBar()
  class Analyze_Finish extends TreeTranslator {

    @Override
    public void visitSelect( JCFieldAccess tree ) {
      super.visitSelect( tree) ;
      if( !String.class.getTypeName().equals(tree.type.toString() ) ) {
        return;
      }
      String type = tree.getExpression().type.toString();
      if ( !ResourceBundleFiles.isHandledResourceBundle( type ) ) {
        return;
      }
      result = replaceWithGetter(tree);
    }

    public JCExpression replaceWithGetter( JCFieldAccess tree ) {
      // replace foo.bar with foo.getBar()
      MethodSymbol getMethod = resolveGetMethod( tree.selected.type, tree.sym );
      TreeMaker make = getTreeMaker();
      JCExpression receiver = tree.selected;
      JCMethodInvocation methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select(make, receiver, getMethod )
          , List.nil() );
      return configMethod( tree, methodCall );
    }

    private MethodSymbol resolveGetMethod( Type type, Symbol field ) {
      Types types = getTypes();
      String getterName =  ResourceBundelCodeGen.createGetterForPropertyName( field.getSimpleName().toString() );
      Type fieldType = types.memberType( type, field ); // the type of the field as a member of `type` e.g., a field  of type List<T> inside Bar<T> as seen from class Foo that extends Bar<String> ...
      return ManAttr.getMethodSymbol( types, type, fieldType, getterName, (ClassSymbol)type.tsym, 0 );
    }

    private JCTree.JCMethodInvocation configMethod(JCTree.JCExpression tree, JCTree.JCMethodInvocation methodTree ) {
      methodTree.setPos( tree.pos );
      // Concrete type set in attr
      methodTree.type = tree.type;
      return methodTree;
    }

    public Types getTypes() {
      return Types.instance( _context );
    }

    public TreeMaker getTreeMaker() {
      return TreeMaker.instance( _context );
    }
  }

  private void ensureInitialized( TaskEvent e ) {
    // ensure JavacPlugin is initialized, particularly for Enter since the order of TaskListeners is evidently not
    // maintained by JavaCompiler i.e., this TaskListener is added after JavacPlugin, but is notified earlier
    JavacPlugin javacPlugin = JavacPlugin.instance();
    if (javacPlugin != null ) {
      javacPlugin.initialize( e );
    }
  }
}
