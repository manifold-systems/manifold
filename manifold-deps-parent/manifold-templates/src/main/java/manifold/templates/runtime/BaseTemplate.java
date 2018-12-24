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

package manifold.templates.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import manifold.templates.ManifoldTemplates;
import manifold.util.ManExceptionUtil;
import manifold.util.StreamUtil;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * The base class for all generated template classes.  You can derive your own base class from this one to
 * provide application-specific functionality. See {@link manifold.templates.sparkjava.SparkTemplate}.
 */
public abstract class BaseTemplate
{
  private ILayout _explicitLayout = null;
  private LocklessLazyVar<String> _templateText = LocklessLazyVar.make(
    () -> {
      try
      {
        InputStreamReader reader = new InputStreamReader( getTemplateResourceAsStream() );
        return StreamUtil.getContent( reader ).replace( "\r\n", "\n" );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }
  );

  /**
   * Open an {@link InputStream} for the template resource file in the classpath/module-path.
   * <p>
   * To be implemented internally by the generated template.
   */
  protected abstract InputStream getTemplateResourceAsStream();

  /**
   * Returns the raw content of the template resource file at runtime.
   * <p>
   * If the template is hard-coded in a test and there is no template resource file, the generated template
   * class overrides this method to return the test-provided text directly.
   */
  protected String getTemplateText()
  {
    return _templateText.get();
  }

  protected void setLayout( ILayout layout )
  {
    _explicitLayout = layout;
  }

  protected ILayout getTemplateLayout()
  {
    if( _explicitLayout != null )
    {
      return _explicitLayout;
    }
    else
    {
      return ManifoldTemplates.getDefaultLayout( this.getClass().getName() );
    }
  }

  protected ILayout getExplicitLayout()
  {
    return _explicitLayout;
  }

  protected void beforeRender( Appendable buffer, ILayout override, boolean topLevelTemplate ) throws IOException
  {
    if( topLevelTemplate )
    {
      ILayout templateLayout = override == null ? getTemplateLayout() : override;
      templateLayout.header( buffer );
    }
  }

  @SuppressWarnings("unused")
  protected void afterRender( Appendable buffer, ILayout override, boolean topLevelTemplate, long renderTime ) throws IOException
  {
    if( topLevelTemplate )
    {
      ILayout templateLayout = override == null ? getTemplateLayout() : override;
      templateLayout.footer( buffer );
    }
    ManifoldTemplates.getTracer().trace( this.getClass(), renderTime );
  }

  @SuppressWarnings("unused")
  protected void handleException( Exception e, String fileName, int lineStart, int[] templateLineNumbers )
  {
    StackTraceElement[] currentStack = e.getStackTrace();
    String templateClassName = getClass().getName();

    int elementToRemove = 0;
    while( elementToRemove < currentStack.length )
    {
      StackTraceElement curr = currentStack[elementToRemove];
      if( curr.getClassName().equals( templateClassName ) )
      {
        if( curr.getMethodName().equals( "renderImpl" ) )
        {
          handleTemplateException( e, fileName, lineStart, templateLineNumbers, elementToRemove );
        }
        else if( curr.getMethodName().equals( "footer" ) || curr.getMethodName().equals( "header" ) )
        {
          handleLayoutException( e, fileName, lineStart, templateLineNumbers, elementToRemove );
        }
      }
      elementToRemove++;
    }
  }

  private void handleTemplateException( Exception e, String fileName, int lineStart, int[] templateLineNumbers, int elementToRemove )
  {
    StackTraceElement[] currentStack = e.getStackTrace();
    int lineNumber = currentStack[elementToRemove].getLineNumber();
    int javaLineNum = lineNumber - lineStart;

    String declaringClass = currentStack[elementToRemove + 1].getClassName();
    String methodName = currentStack[elementToRemove + 1].getMethodName();

    StackTraceElement b = new StackTraceElement( declaringClass, methodName, fileName, templateLineNumbers[javaLineNum] );
    currentStack[elementToRemove + 1] = b;

    System.arraycopy( currentStack, elementToRemove + 1, currentStack, elementToRemove, currentStack.length - 1 - elementToRemove );
    throwUnchecked( e, currentStack );
  }

  private void handleLayoutException( Exception e, String fileName, int lineStart, int[] templateLineNumbers, int elementToReplace )
  {
    StackTraceElement[] currentStack = e.getStackTrace();
    int lineNumber = currentStack[elementToReplace].getLineNumber();
    int javaLineNum = lineNumber - lineStart;

    String declaringClass = currentStack[elementToReplace].getClassName();
    String methodName = currentStack[elementToReplace].getMethodName();

    StackTraceElement b = new StackTraceElement( declaringClass, methodName, fileName, templateLineNumbers[javaLineNum] );
    currentStack[elementToReplace] = b;

    throwUnchecked( e, currentStack );
  }

  private void throwUnchecked( Exception e, StackTraceElement[] currentStack )
  {
    e.setStackTrace( currentStack );
    throw ManExceptionUtil.unchecked( e );
  }

  public String toS( Object o )
  {
    return o == null ? "" : o.toString();
  }
}
