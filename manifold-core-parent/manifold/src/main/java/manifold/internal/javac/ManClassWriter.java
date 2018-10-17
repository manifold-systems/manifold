package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.OutputStream;
import javax.tools.JavaFileObject;
import manifold.api.type.ISelfCompiledFile;

public class ManClassWriter extends ClassWriter
{
  public static ManClassWriter instance( Context ctx )
  {
    ClassWriter classWriter = ctx.get( classWriterKey );
    if( !(classWriter instanceof ManClassWriter) )
    {
      ctx.put( classWriterKey, (ClassWriter)null );
      classWriter = new ManClassWriter( ctx );
    }

    return (ManClassWriter)classWriter;
  }

  private ManClassWriter( Context ctx )
  {
    super( ctx );
  }

  @Override
  public void writeClassFile( OutputStream out, Symbol.ClassSymbol c ) throws StringOverflow, IOException, PoolOverflow
  {
    JavaFileObject sourceFile = c.sourcefile;
    if( sourceFile instanceof ISelfCompiledFile && ((ISelfCompiledFile)sourceFile).isSelfCompile() )
    {
      out.write( ((ISelfCompiledFile)sourceFile).compile() );
    }
    else
    {
      super.writeClassFile( out, c );
    }
  }
}