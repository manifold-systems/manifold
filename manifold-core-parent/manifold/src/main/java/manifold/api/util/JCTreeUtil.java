package manifold.api.util;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

import java.util.Arrays;

public class JCTreeUtil
{
  private JCTreeUtil(){
    // hide utility class constructor
  }

  public static boolean hasAnnotation( JCTree.JCVariableDecl variableDecl, Class<?>... annotationClasses )
  {
    return containsAnnotationOfType( variableDecl.getModifiers().getAnnotations(), annotationClasses );
  }

  public static boolean hasAnnotation( JCTree.JCClassDecl classDecl, Class<?>... annotationClasses )
  {
    return containsAnnotationOfType( classDecl.getModifiers().getAnnotations(), annotationClasses );
  }

  public static boolean hasAnnotation( JCTree.JCMethodDecl methodDecl, Class<?>... annotationClasses )
  {
    return containsAnnotationOfType( methodDecl.getModifiers().getAnnotations(), annotationClasses );
  }

  public static boolean containsAnnotationOfType( List<JCTree.JCAnnotation> annotations, Class<?>... annotationClasses )
  {
    return annotations.stream().anyMatch( annotation -> isAnnotationOfType( annotation, annotationClasses ) );
  }

  public static boolean isAnnotationOfType( JCTree.JCAnnotation annotation, Class<?>... annotationClasses )
  {
    String annotationFqn = annotation.getAnnotationType().type.toString();
    return Arrays.stream( annotationClasses ).anyMatch( annotationClass -> annotationClass.getCanonicalName().equals( annotationFqn ) );
  }
}
