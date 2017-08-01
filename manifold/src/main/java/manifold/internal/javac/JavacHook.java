package manifold.internal.javac;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileManager;


import static javax.lang.model.SourceVersion.RELEASE_8;

/**
 */
@SupportedSourceVersion(RELEASE_8)
@SupportedAnnotationTypes({"*"})
public class JavacHook extends AbstractProcessor
{
  private static JavacHook INSTANCE = null;

  private static final String GOSU_SOURCE_FILES = "gosu.source.files";

  private JavacProcessingEnvironment _jpe;
  private JavacPlugin _plugin;
  private TypeProcessor _typeProcessor;

  public static JavacHook instance()
  {
    return INSTANCE;
  }

  public JavacHook()
  {
    INSTANCE = this;
  }

  @Override
  public synchronized void init( ProcessingEnvironment processingEnv )
  {
    super.init( processingEnv );
    _plugin = JavacPlugin.instance();
    _jpe = (JavacProcessingEnvironment)processingEnv;
    _typeProcessor = new TypeProcessor( JavacTask.instance( _jpe ) );
  }

  ManifoldJavaFileManager getManFileManager()
  {
    return _plugin.getManifoldFileManager();
  }

  JavaFileManager getJavaFileManager()
  {
    return _plugin.getJavaFileManager();
  }

  Context getContext()
  {
    return _plugin.getContext();
  }

  public JavacTask getJavacTask()
  {
    return _plugin.getJavacTask();
  }

  TreeMaker getTreeMaker()
  {
    return _plugin.getTreeMaker();
  }

  JavacElements getJavacElements()
  {
    return _plugin.getJavacElements();
  }

  @Override
  public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv )
  {
    _typeProcessor.addTypesToProcess( roundEnv );

    if( roundEnv.processingOver() )
    {
      return false;
    }

    insertBootstrap( roundEnv );
    return false;
  }

  private void insertBootstrap( RoundEnvironment roundEnv )
  {
    Trees trees = Trees.instance( _jpe );

    Set<? extends Element> elements = roundEnv.getRootElements();
    for( Element elem : elements )
    {
      if( elem.getKind() == ElementKind.CLASS )
      {
        JCTree tree = (JCTree)trees.getTree( elem );
        TreeTranslator visitor = new BootstrapInserter( this );
        tree.accept( visitor );
      }
    }
  }
}
