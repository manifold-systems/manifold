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

package manifold.javadoc.agent;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.*;

public class Agent
{
  private static final int Java_Version = getJavaVersion();
  private static final String JavadocTool_class =
    Java_Version == 8
    ? "com/sun/tools/javadoc/JavadocTool"
    : "jdk/javadoc/internal/tool/JavadocTool";

  public static void premain( String args, Instrumentation instrumentation )
  {
    instrumentation.addTransformer(
      new ClassFileTransformer()
      {
        @Override
        public byte[] transform( ClassLoader loader, String name, Class<?> cls, ProtectionDomain pd, byte[] bytes )
        {
          if( JavadocTool_class.equals( name ) )
          {
            ClassReader reader = new ClassReader( bytes );
            ClassWriter writer = new ClassWriter( reader, 0 );
            JavacPluginAdder visitor = new JavacPluginAdder( writer );
            reader.accept( visitor, 0 );
            return writer.toByteArray();
          }
          return null;
        }
      } );
  }

  /**
   * Weaves in a call to JavacPlugin#initJavacPlugin(Context) at the beginning of JavadocTool#getRootDocImpl().
   * <p/>
   * Note we have to resort to a javagent and weaving because there isn't a more reasonable way to get a hold of the
   * Context that is needed to initialize JavacPlugin so it can do its magic on Java's AST, otherwise Javadoc fails in
   * the Parse phase e.g., properties and operator overloading require manifold's JavacPlugin to transform the AST
   * before the javadoc compiler looks at it.
   */
  private static class JavacPluginAdder extends ClassVisitor
  {
    public JavacPluginAdder( ClassWriter writer )
    {
      super( ASM9, writer );
    }

    @Override
    public MethodVisitor visitMethod( int access, String name, String descriptor, String signature, String[] exceptions )
    {
      MethodVisitor mv = super.visitMethod( access, name, descriptor, signature, exceptions );
      if( name.equals( Java_Version == 8 ? "getRootDocImpl" : "getEnvironment" ) )
      {
        return new MethodVisitor( ASM9, mv )
        {
          @Override
          public void visitCode()
          {
            // push the Context
            visitVarInsn( ALOAD, 0 );
            visitFieldInsn( GETFIELD, JavadocTool_class, "context", "Lcom/sun/tools/javac/util/Context;" );

            // push the ClassLoader
            mv.visitVarInsn( ALOAD, 0 );
            mv.visitFieldInsn( GETFIELD, JavadocTool_class, "context", "Lcom/sun/tools/javac/util/Context;" );
            mv.visitLdcInsn( Type.getType( "Ljavax/tools/JavaFileManager;" ) );
            mv.visitMethodInsn( INVOKEVIRTUAL, "com/sun/tools/javac/util/Context", "get", "(Ljava/lang/Class;)Ljava/lang/Object;", false );
            mv.visitTypeInsn( CHECKCAST, "javax/tools/JavaFileManager" );
            mv.visitFieldInsn( GETSTATIC, "javax/tools/StandardLocation", "CLASS_PATH", "Ljavax/tools/StandardLocation;" );
            mv.visitMethodInsn( INVOKEINTERFACE, "javax/tools/JavaFileManager", "getClassLoader", "(Ljavax/tools/JavaFileManager$Location;)Ljava/lang/ClassLoader;", true );

            // call Util.initJavacPlugin(ClassLoader, Context)
            visitMethodInsn( INVOKESTATIC, "manifold/javadoc/agent/Util", "initJavacPlugin", "(Ljava/lang/Object;Ljava/lang/ClassLoader;)V", false );
            super.visitCode();
          }
        };
      }
      else
      {
        return mv;
      }
    }
  }

  private static int getJavaVersion()
  {
    String version = System.getProperty( "java.version" );
    StringBuilder sb = new StringBuilder();
    for( int i = 0; i < version.length(); i++ )
    {
      char c = version.charAt( i );
      if( Character.isDigit( c ) )
      {
        sb.append( c );
      }
      else
      {
        break;
      }
    }
    int major = Integer.parseInt( sb.toString() );
    // note, android's major is 0, but is really 8 (ish)
    major = major <= 1 ? 8 : major;
    return major;
  }
}
