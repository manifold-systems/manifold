package manifold.api.image;

import java.lang.reflect.Modifier;
import java.net.URL;
import javax.swing.ImageIcon;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcType;
import manifold.api.type.SourcePosition;
import manifold.util.ManClassUtil;
import manifold.util.ManEscapeUtil;

/**
 */
public class ImageCodeGen
{
  private final String _fqn;
  private final String _url;

  ImageCodeGen( String url, String topLevelFqn )
  {
    _url = url;
    _fqn = topLevelFqn;
  }

  public SrcClass make()
  {
    try
    {
      String simpleName = ManClassUtil.getShortClassName( _fqn );
      return new SrcClass( _fqn, SrcClass.Kind.Class ).imports( URL.class, SourcePosition.class )
        .superClass( new SrcType( ImageIcon.class ) )
        .addField( new SrcField( "INSTANCE", simpleName ).modifiers( Modifier.STATIC ) )
        .addConstructor( new SrcConstructor()
                           .addParam( new SrcParameter( "url" )
                                        .type( URL.class ) )
                           .modifiers( Modifier.PRIVATE )
                           .body( new SrcStatementBlock()
                                    .addStatement( new SrcRawStatement()
                                                     .rawText( "super(url);" ) )
                                    .addStatement( new SrcRawStatement()
                                                     .rawText( "INSTANCE = this;" ) ) ) )
        .addMethod( new SrcMethod().modifiers( Modifier.PUBLIC | Modifier.STATIC )
                      .name( "get" )
                      .returns( simpleName )
                      .body( new SrcStatementBlock()
                               .addStatement(
                                 new SrcRawStatement()
                                   .rawText( "try {" )
                                   .rawText( "  return INSTANCE != null ? INSTANCE : new " + simpleName + "(new URL(\"" + ManEscapeUtil.escapeForJavaStringLiteral( _url ) + "\"));" )
                                   .rawText( "} catch(Exception e) {" )
                                   .rawText( "  throw new RuntimeException(e);" )
                                   .rawText( "}" ) ) ) );

    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
