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

package manifold.ext.typealias;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import manifold.api.type.ICompilerComponent;
import manifold.ext.typealias.rt.api.TypeAlias;
import manifold.ext.typealias.rt.api.TypeAliasProvider;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.ManAttr;
import manifold.internal.javac.TypeAliasTranslator;
import manifold.internal.javac.TypeProcessor;
import manifold.rt.api.util.Stack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TypeAliasProcessor implements ICompilerComponent, TaskListener {

  private static final String ANNOTATION_TYPE_NAME = TypeAlias.class.getTypeName();
  private static final String ANNOTATION_TYPE_SIMPLE_NAME = TypeAlias.class.getSimpleName();

  private BasicJavacTask _javacTask;
  private Context _context;
  private Stack<JCClassDecl> _classDeclStack;
  private TaskEvent _taskEvent;
  private Watcher _watcher;
  private IdentityHashMap<ClassSymbol, ClassEntry> _cache;

  private HashSet<JCCompilationUnit> _changed;
  private HashSet<JCCompilationUnit> _processed;

  @Override
  public void init(BasicJavacTask javacTask, TypeProcessor typeProcessor) {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _classDeclStack = new Stack<>();
    _watcher = new Watcher();
    _cache = new IdentityHashMap<>();
    _processed = new HashSet<>();
    _changed = new HashSet<>();

    if (JavacPlugin.instance() == null) {
      // does not function at runtime
      return;
    }

    // Ensure TypeProcessor follows this in the listener list e.g., so that delegation integrates with structural
    // typing and extension methods.
    typeProcessor.addTaskListener(this);
  }

  BasicJavacTask getJavacTask() {
    return _javacTask;
  }

  Context getContext() {
    return _context;
  }

  public Types getTypes() {
    return Types.instance(getContext());
  }

  public Names getNames() {
    return Names.instance(getContext());
  }

  public TreeMaker getTreeMaker() {
    return TreeMaker.instance(getContext());
  }

  public TreeMaker getTreeMaker(int pos) {
    TreeMaker make = getTreeMaker();
    make.pos = pos;
    return make;
  }

  public Symtab getSymtab() {
    return Symtab.instance(getContext());
  }

  public Attr getAttr() {
    return Attr.instance(getContext());
  }

  @Override
  public void tailorCompiler() {
    _context = _javacTask.getContext();
    TypeAliasTranslator.IMPORTER = this::include;
  }

  private void include( Name name, ClassSymbol symbol ) {
    _cache.computeIfAbsent(symbol, ClassEntry::new).resolve();
  }

  private CompilationUnitTree getCompilationUnit() {
    if (_taskEvent != null) {
      CompilationUnitTree compUnit = _taskEvent.getCompilationUnit();
      if (compUnit != null) {
        return compUnit;
      }
    }
    return JavacPlugin.instance() != null
            ? JavacPlugin.instance().getTypeProcessor().getCompilationUnit()
            : null;
  }

  @Override
  public void started(TaskEvent e) {
    if (e.getKind() != TaskEvent.Kind.ENTER) {
      return;
    }
    _taskEvent = e;
    try {
      ensureInitialized(_taskEvent);
      JCCompilationUnit compilationUnit = (JCCompilationUnit) e.getCompilationUnit();
      if (!_processed.add(compilationUnit)) {
        // In the multiple project case,
        // ENTER maybe called multiple times,
        // we only need to handle the first time.
        return;
      }
      compilationUnit.accept(new FastAnnotation(_watcher, compilationUnit));
      TypeAliasTranslator.TRANSFORMER = new FastRename(_watcher);
    } finally {
      _taskEvent = null;
    }
//    System.out.println(e.getKind());
  }

  @Override
  public void finished(TaskEvent e) {
    if (e.getKind() != TaskEvent.Kind.ANALYZE) {
      return;
    }
//    JCCompilationUnit compilationUnit = (JCCompilationUnit) e.getCompilationUnit();
//    if (_changed.contains(compilationUnit)) {
//      System.out.println(compilationUnit);
//    }
  }


  static class Watcher {

    HashMap<Name, Type> names = new HashMap<>();
    HashMap<Symbol, Type> symbols = new HashMap<>();
    HashSet<Name> keywords = new HashSet<>();

    public void add(Symbol source, Type newType) {
      //System.out.println("aliasing: " + source + " to " + newType);
      // if the symbol is defined, ignore when not changes.
      if (symbols.get(source) == newType) {
        return;
      }
      Type linkedType = symbols.get(newType.tsym);
      if (linkedType != null) {
        newType = linkedType;
      }
      symbols.put(source, newType);
      names.put(source.flatName(), newType);
      keywords.add(source.name);
    }

    public Type lookup(Symbol symbol) {
      return symbols.get(symbol);
    }

    public Type lookup(Name name) {
      return names.get(name);
    }

    public boolean looking(Name name) {
      return keywords.contains(name);
    }

    public boolean isEmpty() {
      return symbols.isEmpty();
    }
  }

  class FastAnnotation extends TreeScanner {

    final Watcher watcher;
    final JCCompilationUnit compilationUnit;

    FastAnnotation(Watcher watcher, JCCompilationUnit compilationUnit) {
      this.watcher = watcher;
      this.compilationUnit = compilationUnit;
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
      super.visitClassDef(tree);
      JCAnnotation annotation = getTypeAliasAnnotation(tree);
      if (annotation == null) {
        return;
      }
      JCExpression expr2 = getTypeAliasAnnotationValue(annotation);
      if (expr2 == null) {
        expr2 = tree.extending;
      }
      TreeMaker maker = getTreeMaker(tree.pos);
      JCFieldAccess clazz = generateDirectClassAccess(maker, TypeAliasProvider.class);
      JCTypeApply apply = maker.TypeApply(clazz, List.of(expr2));
      tree.implementing = tree.implementing.append(apply);
//      _changed.add(compilationUnit);
    }

    @Override
    public void scan(JCTree tree) {
      if (tree instanceof JCClassDecl || tree instanceof JCCompilationUnit) {
        // we just quick lookup of the class def,
        // so not need check something else.
        super.scan(tree);
      }
    }

    private JCAnnotation getTypeAliasAnnotation(JCClassDecl tree) {
      for (List<JCAnnotation> var2 = tree.mods.annotations; var2.nonEmpty(); var2 = var2.tail) {
        JCAnnotation annotation = var2.head;
        if (isTypeAliasType(annotation.annotationType) && hasTypeAliasAnnotationValue(annotation)) {
          return annotation;
        }
      }
      return null;
    }

    private boolean hasTypeAliasAnnotationValue(JCAnnotation annotation) {
      int size = annotation.args.size();
      if (size == 1) {
        return getTypeAliasAnnotationValue(annotation) != null;
      }
      return size == 0;
    }

    private JCExpression getTypeAliasAnnotationValue(JCAnnotation annotation) {
      JCTree tree = annotation.args.head;
      // @TypeAlias(value = <value>)
      if (tree instanceof JCAssign) {
        // if name not value, it means that it may be an annotation with the same name.
        if (!TypeAliasUtil.fullName(((JCAssign) tree).lhs).equals("value")) {
          return null;
        }
        tree = ((JCAssign) tree).rhs;
      }
      // @TypeAlias({<value>})
      if (tree instanceof JCNewArray) {
        tree = ((JCNewArray) tree).elems.head;
      }
      // @TypeAlias(<value>)
      if (tree instanceof JCFieldAccess) {
        JCFieldAccess field = (JCFieldAccess) tree;
        if (field.name.toString().equals("class")) {
          return field.selected;
        }
      }
      return null;
    }

    private boolean isTypeAliasType(JCTree tree) {
      String name = TypeAliasUtil.fullName(tree);
      if (name.equals(ANNOTATION_TYPE_NAME)) {
        return true;
      }
      // fo avoid the using same-name annotation,
      // we need to find the package of TypeAlias.
      if (!name.equals(ANNOTATION_TYPE_SIMPLE_NAME)) {
        return false;
      }
      // search type alias package in import.
      for (JCTree def : compilationUnit.defs) {
        if (def instanceof JCImport) {
          String pkg = TypeAliasUtil.fullName(((JCImport)def).qualid);
          if (ANNOTATION_TYPE_NAME.equals(pkg.replace("*", ANNOTATION_TYPE_SIMPLE_NAME))) {
            return true;
          }
        }
      }
      // search type alias in current package.
      String pkg = TypeAliasUtil.fullName(compilationUnit.pid);
      return ANNOTATION_TYPE_NAME.equals(pkg + "." + ANNOTATION_TYPE_SIMPLE_NAME);
    }
  }

  class FastReplace extends TreeTranslator {

    private HashMap<JCTree, JCTree> mapping = new HashMap<>();

    FastReplace(JCTree from, JCTree to) {
      mapping.put(from, to);
    }

    @Override
    public <T extends JCTree> T translate(T t) {
      if (mapping.isEmpty()) {
        // ignore when mapping is completed.
        return t;
      }
      T value = super.translate(t);
      if (value == null) {
        // ignored when the value is null.
        return null;
      }
      JCTree result = mapping.remove(value);
      if (result != null) {
        // yep, we found it.
        return (T) result;
      }
      return value;
    }
  }

  class FastRename extends JCTree.Visitor implements TypeAliasTranslator.Transformer {
    final Watcher watcher;
    final Attr attr = getAttr();

    JCTree result = null;

    FastRename(Watcher watcher) {
      this.watcher = watcher;
    }

    @Override
    public JCTree transform(JCTree tree) {
      result = null;
      tree.accept(this);
      return result;
    }

    @Override
    public void visitIdent(JCIdent tree) {
      //System.out.println("visit ident: " + tree);
      if (!isClassSymbol(tree.sym) || !watcher.looking(tree.name)) {
        return;
      }
      Type newType = watcher.lookup(tree.sym);
      if (newType == null) {
        return;
      }
      //Name old = tree.name;
      TreeMaker maker = getTreeMaker(tree.pos);
      JCIdent tree1 = maker.Ident(newType.tsym);
      tree.name = tree1.name;
      tree.sym = tree1.sym;
      tree.type = newType;

      result = maker.Type(newType);
      JCTree parent = getParentTree(tree);
      parent.accept(new FastReplace(tree, result));

//      _changed.add(((ManAttr)attr).getEnv().toplevel);
      //System.out.println("replace ident: " + old + " to " + result);
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
      //System.out.println("visit select: " + tree);
      if (!isClassSymbol(tree.sym) || !watcher.looking(tree.name)) {
        return;
      }
      JCTree parent = getParentTree(tree);
      if (parent == null || parent.getTag() == JCTree.Tag.IMPORT) {
        return;
      }
      // direct class access: A.B.C.Name.[member|field]
      Type newType = watcher.lookup(TreeInfo.fullName(tree));
      if (newType == null) {
        return;
      }
      //Name old = TreeInfo.fullName(tree);
      TreeMaker maker = getTreeMaker(tree.pos);
      JCFieldAccess tree1 = generateDirectClassAccess(maker, newType.tsym);
      tree.name = tree1.name;
      tree.selected = tree1.selected;
      tree.sym = tree1.sym;
      tree.type = newType;

      result = maker.Type(newType);
      parent.accept(new FastReplace(tree, result));

//      _changed.add(((ManAttr)attr).getEnv().toplevel);
      //System.out.println("replace select: " + old + " to " + result);
    }

    @Override
    public void visitTree(JCTree tree) {
      result = null;
    }

    private boolean isClassSymbol(Symbol symbol) {
      return symbol instanceof ClassSymbol;
    }

    private JCTree getParentTree(JCTree tree) {
      return ((ManAttr) attr).getParent(tree);
    }
  }

  class ClassEntry {

    private List<Type> _interfaces;
    private final ClassSymbol _symbol;

    ClassEntry(ClassSymbol symbol) {
      _symbol = symbol;
    }

    void resolve() {
      // when the interface has been changed, it means the symbol was modified.
      List<Type> interfaces = _symbol.getInterfaces();
      if( interfaces == _interfaces ) {
        return;
      }
      _interfaces = interfaces;
      Type newType = TypeAliasUtil.getAliasTypeFromInterface( interfaces );
      //System.out.println("include: " + _symbol);
      if( newType != null ) {
        _watcher.add(_symbol, newType);
      }
    }
  }

  private JCFieldAccess generateDirectClassAccess(TreeMaker maker, Class<?> targetClass) {
    Names names = getNames();
    String[] parts = targetClass.getName().split("\\.");
    JCExpression expr = maker.Ident(names.fromString(parts[0]));
    for (int i = 1; i < parts.length; ++i) {
      expr = maker.Select(expr, names.fromString(parts[i]));
    }
    return (JCFieldAccess) expr;
  }

  private JCFieldAccess generateDirectClassAccess(TreeMaker maker, Symbol.TypeSymbol symbol) {
    return (JCFieldAccess) symbol.accept(new Symbol.Visitor<JCExpression, TreeMaker>() {
      @Override
      public JCExpression visitClassSymbol(ClassSymbol classSymbol, TreeMaker treeMaker) {
        return treeMaker.Select(classSymbol.owner.accept(this, treeMaker), classSymbol);
      }

      @Override
      public JCExpression visitPackageSymbol(Symbol.PackageSymbol packageSymbol, TreeMaker treeMaker) {
        if (packageSymbol.owner == null || packageSymbol.owner.owner == null) {
          return treeMaker.Ident(packageSymbol);
        }
        return treeMaker.Select(packageSymbol.owner.accept(this, treeMaker), packageSymbol);
      }

      @Override
      public JCExpression visitMethodSymbol(Symbol.MethodSymbol methodSymbol, TreeMaker treeMaker) {
        return null;
      }

      @Override
      public JCExpression visitOperatorSymbol(Symbol.OperatorSymbol operatorSymbol, TreeMaker treeMaker) {
        return null;
      }

      @Override
      public JCExpression visitVarSymbol(Symbol.VarSymbol varSymbol, TreeMaker treeMaker) {
        return null;
      }

      @Override
      public JCExpression visitTypeSymbol(Symbol.TypeSymbol typeSymbol, TreeMaker treeMaker) {
        return null;
      }

      @Override
      public JCExpression visitSymbol(Symbol symbol, TreeMaker treeMaker) {
        return null;
      }
    }, maker);
  }

//  private void reportWarning(JCTree location, String message) {
//    report(Diagnostic.Kind.WARNING, location, message);
//  }
//
//  private void reportError(JCTree location, String message) {
//    report(Diagnostic.Kind.ERROR, location, message);
//  }
//
//  private void report(Diagnostic.Kind kind, JCTree location, String message) {
//    report(_taskEvent.getSourceFile(), location, kind, message);
//  }
//
//  public void report(JavaFileObject sourcefile, JCTree tree, Diagnostic.Kind kind, String msg) {
//    IssueReporter<JavaFileObject> reporter = new IssueReporter<>(_javacTask::getContext);
//    JavaFileObject file = sourcefile != null ? sourcefile : TypeAliasUtil.getFile(tree, child -> getParent(child));
//    reporter.report(new JavacDiagnostic(file, kind, tree.getStartPosition(), 0, 0, msg));
//  }

  //  private Type attribType(JCTree tree, JCClassDecl classDecl) {
//    TypeAliasTranslator.Transformer pre = TypeAliasTranslator.APPLY;
//    TypeAliasTranslator.APPLY = null;
//    Env<AttrContext> env = Enter.instance(getContext()).getClassEnv(classDecl.sym);
//    Type ret = getAttr().attribType(tree, env);
//    TypeAliasTranslator.APPLY = pre;
//    if (ret != Type.noType) {
//      return ret;
//    }
//    return null;
//  }

  private void ensureInitialized(TaskEvent e) {
    // ensure JavacPlugin is initialized, particularly for Enter since the order of TaskListeners is evidently not
    // maintained by JavaCompiler i.e., this TaskListener is added after JavacPlugin, but is notified earlier
    JavacPlugin javacPlugin = JavacPlugin.instance();
    if (javacPlugin != null) {
      javacPlugin.initialize(e);
    }
  }
}

