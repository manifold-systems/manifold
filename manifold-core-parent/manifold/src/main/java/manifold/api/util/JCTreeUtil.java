package manifold.api.util;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

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

  public static JCTree.JCExpression makeEmptyValue( Type valueType, TreeMaker make, Types types, Symtab syms )
  {
    if( valueType.isPrimitive() )
    {
      return make.Literal( defaultPrimitiveValue( valueType, syms ) );
    }
    return types.isSameType( valueType, types.erasure( valueType ) )
           ? make.TypeCast( valueType, make.Literal( TypeTag.BOT, null ) )
           : make.Literal( TypeTag.BOT, null );
  }

  public static Object defaultPrimitiveValue( Type type, Symtab syms )
  {
    if( type == syms.intType ||
        type == syms.shortType )
    {
      return 0;
    }
    if( type == syms.byteType )
    {
      return (byte)0;
    }
    if( type == syms.longType )
    {
      return 0L;
    }
    if( type == syms.floatType )
    {
      return 0f;
    }
    if( type == syms.doubleType )
    {
      return 0d;
    }
    if( type == syms.booleanType )
    {
      return false;
    }
    if( type == syms.charType )
    {
      return (char)0;
    }
    if( type == syms.botType )
    {
      return null;
    }
    throw new IllegalArgumentException( "Unsupported primitive type: " + type.tsym.getSimpleName() );
  }

  public static JCTree.JCExpression memberAccess( TreeMaker make, Names names, String path )
  {
    return memberAccess( make, names, path.split( "\\." ) );
  }

  public static JCTree.JCExpression memberAccess( TreeMaker make, Names names, String... components )
  {
    JCTree.JCExpression expr = make.Ident( names.fromString( (components[0]) ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, names.fromString( components[i] ) );
    }
    return expr;
  }
}
