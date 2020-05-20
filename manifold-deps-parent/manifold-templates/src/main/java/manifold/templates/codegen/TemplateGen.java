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

package manifold.templates.codegen;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.fs.def.FileFragmentImpl;
import manifold.api.DisableStringLiteralTemplates;
import manifold.internal.javac.IIssue;
import manifold.templates.manifold.TemplateIssue;
import manifold.templates.manifold.TemplateIssueContainer;
import manifold.templates.tokenizer.Token;
import manifold.templates.tokenizer.Tokenizer;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.ManEscapeUtil;


import static manifold.templates.codegen.TemplateGen.DirType.*;

public class TemplateGen
{
  private static final String BASE_CLASS_NAME = "BaseTemplate";
  private static final String LAYOUT_INTERFACE = "ILayout";

  private List<TemplateIssue> _issues = new ArrayList<>();

  public String generateCode( String fullyQualifiedName, String source, IFile file, URI fileUri, String fileName )
  {
    FileGenerator generator = new FileGenerator( fullyQualifiedName, file, fileUri, fileName, source );
    return generator.getFileContents();
  }

  private void addError( String message, int line )
  {
    TemplateIssue error = new TemplateIssue( IIssue.Kind.Error, 0, line, 0, message );
    _issues.add( error );
  }

  public TemplateIssueContainer getIssues()
  {
    return new TemplateIssueContainer( _issues );
  }

  class ClassInfo
  {
    final String fqn;
    private final ClassInfo parent;
    Map<Integer, ClassInfo> nestedClasses = new HashMap<>();
    String params = null;
    String[][] paramsList = null;
    String name;
    URI fileUri;
    String fileName;
    String superClass = BASE_CLASS_NAME;
    int startTokenPos;
    Integer endTokenPos;
    int depth;
    boolean isLayout = false;
    boolean hasLayout = false;
    Directive layoutDir;
    int contentPos;
    String testSource;
    private IFile _file;

    //only for the outermost class
    private ClassInfo( Iterator<Directive> dirIterator, String fqn, String name, IFile file, URI fileUri, String fileName, Integer endTokenPos )
    {
      parent = null;
      this.fqn = fqn;
      this.name = name;
      this.fileUri = fileUri;
      this.fileName = fileName;
      this.startTokenPos = 0;
      this.endTokenPos = endTokenPos;
      this.depth = 0;
      _file = file;

      fillClassInfo( dirIterator );
    }

    ClassInfo( Iterator<Directive> dirIterator, ClassInfo parent, String name, String params, String[][] paramList, int startTokenPos, int depth, String superClass )
    {
      this.parent = parent;
      this.fqn = parent.fqn;
      this.name = name;
      this.fileUri = parent.fileUri;
      this.fileName = parent.fileName;
      this.params = params;
      this.paramsList = paramList;
      this.startTokenPos = startTokenPos;
      this.depth = depth;
      this.superClass = superClass;

      fillClassInfo( dirIterator );
    }

    void fillClassInfo( Iterator<Directive> dirIterator )
    {
      boolean endSec = false;

      outerLoop:
      while( dirIterator.hasNext() )
      {
        Directive dir = dirIterator.next();
        switch( dir._dirType )
        {
          case IMPORT:
            break;
          case NEST:
          case INCLUDE:
            break;
          case EXTENDS:
            if( depth == 0 )
            {
              if( superClass.equals( BASE_CLASS_NAME ) )
              {
                superClass = dir.className;
              }
              else
              {
                addError( "Invalid Extends Directive: class cannot extend 2 classes", dir.token.getLine() );
              }
            }
            else
            {
              addError( "Invalid Extends Directive: class cannot extend within section", dir.token.getLine() );
            }

            break;
          case PARAMS:
            if( depth == 0 )
            {
              if( params == null )
              {
                params = dir.params;
                paramsList = dir.paramsList;
              }
              else
              {
                addError( "Invalid Params Directive: class cannot have 2 params directives", dir.token.getLine() );
              }
            }
            else
            {
              addError( "Invalid Params Directive: class cannot have param directive within section", dir.token.getLine() );
            }
            break;
          case SECTION:
            addNestedClass( new ClassInfo( dirIterator, this, dir.className, dir.params, dir.paramsList, dir.tokenPos + 1, depth + 1, superClass ) );
            break;
          case END_SECTION:
            if( endTokenPos == null )
            {
              endTokenPos = dir.tokenPos;
            }
            else
            {
              addError( "Invalid End Section Directive: section declaration does not exist", dir.token.getLine() );
            }
            endSec = true;
            break outerLoop;
          case CONTENT:
            if( isLayout )
            {
              addError( "Invalid Layout Instantiation: cannot have two layout instantiations", dir.token.getLine() );
            }
            else if( depth > 0 )
            {
              addError( "Invalid Layout Instantiation: cannot instantiate layout within section", dir.token.getLine() );
            }
            else
            {
              isLayout = true;
              contentPos = dir.tokenPos;
            }
            break;
          case LAYOUT:
            if( hasLayout )
            {
              addError( "Invalid Layout Declaration: cannot have two layout declarations", dir.token.getLine() );
            }
            else if( depth > 0 )
            {
              addError( "Invalid Layout Declaration: cannot declare layout within section", dir.token.getLine() );
            }
            else
            {
              hasLayout = true;
              layoutDir = dir;
            }
            break;
          case ERRANT:
            //continue;

        }
      }
      if( !endSec )
      {
        if( depth == 0 )
        {
          assert (startTokenPos == 0);
        }
        else
        {
          addError( "Reached end of file before parsing section: " + name, 0 ); //TODO: Fix this to get the correct line number (of the end of the file?)
        }
      }
    }

    void addNestedClass( ClassInfo nestedClass )
    {
      nestedClasses.put( nestedClass.startTokenPos, nestedClass );
    }

    public boolean isFragment()
    {
      return _file instanceof IFileFragment;
    }

    public String getFragmentText()
    {
      return ((FileFragmentImpl)_file).getContent();
    }
  }

  protected enum DirType
  {
    IMPORT( "import" ),     //className
    EXTENDS( "extends" ),    //className
    PARAMS( "params" ),     //           params, paramsList
    INCLUDE( "include" ),    //className, params,            conditional
    NEST( "nest" ),      //className, params,            conditional
    SECTION( "section" ),    //className, params, paramsList
    END_SECTION( "end" ),//
    CONTENT( "content" ),    //
    LAYOUT( "layout" ),      //className
    ERRANT( "#errant" )      //the directive is invalid
    ;

    private String _keyword;

    DirType( String keyword )
    {
      _keyword = keyword;
    }

    public String keyword()
    {
      return _keyword;
    }
  }

  class Directive
  {
    int tokenPos;

    Token token;

    DirType _dirType;

    //imports "[static] [class_name]"
    //extends "[class_name]"
    //params ([paramType paramName], [paramType paramName],...)                  <---nothing stored for params or end section
    //include "[templateName]"([paramVal], [paramVal],...) (conditional)
    //nest "[templateName]"([paramVal], [paramVal],...) (conditional)
    //section "[sectionName]"([paramType paramName], [paramType paramName],...)
    //end section
    String className;

    //iff section, params, nest, and include (empty string if params not given for include/nest)
    String params;

    //iff section and params only (include/nest doesn't need it broken down bc types aren't given)
    String[][] paramsList;

    //iff include/nest
    String conditional;

    Directive( int tokenPos, Token token, List<Token> tokens )
    {
      assert (token.getType() == Token.TokenType.DIRECTIVE);
      this.tokenPos = tokenPos;
      this.token = token;

      identifyType();
      fillVars( tokens );
    }

    private void identifyType()
    {
      String text = token.getText().trim();
      Optional<DirType> dirType = Arrays.stream( DirType.values() ).filter( dt -> text.startsWith( dt.keyword() ) ).findFirst();
      _dirType = dirType.orElse( ERRANT );
      if( _dirType == ERRANT )
      {
        addError( "Unsupported Directive Type", token.getLine() );
      }
    }

    private void fillVars( List<Token> tokens )
    {
      String text = token.getText();
      text = text.trim();
      switch( _dirType )
      {
        case IMPORT:
          break;
        case EXTENDS:
          className = text.substring( EXTENDS.keyword().length() ).trim();
          break;
        case PARAMS:
          String content = text.substring( PARAMS.keyword().length() ).trim();
          if( content.length() > 1 )
          {
            params = content.substring( 1, content.length() - 1 );
            paramsList = splitParamsList( params );
          }
          break;
        case INCLUDE:
        case NEST:
          fillIncludeVars( _dirType );
          break;
        case SECTION:
          String[] temp = text.substring( SECTION.keyword().length() ).trim().split( "\\(", 2 );
          className = temp[0].trim();
          if( temp.length == 2 && !temp[1].equals( ")" ) && !temp[1].trim().isEmpty() )
          {
            params = temp[1].substring( 0, temp[1].length() - 1 ).trim();
            paramsList = splitParamsList( params );
            findParamTypes( paramsList, tokenPos, tokens );
            params = makeParamsString( paramsList );
          }
          break;
        case END_SECTION:
          break;
        case CONTENT:
          break;
        case LAYOUT:
          className = text.substring( LAYOUT.keyword().length() ).trim();
          break;
        case ERRANT:
          break;
      }
    }

    /**
     * Helper method: Given that the type of token is INCLUDE/NEST, will parse through the content of the token
     * and accordingly set className, params, and conditional. The format of an include/nest statement is as follows:
     * <%@ include templateNameHere[(optional-params)][if(optional conditional)] %>
     * Note that in the if statement, parentheses around the conditional are optional.
     * @param dirType
     */
    private void fillIncludeVars( DirType dirType )
    {
      String text = token.getText();
      text = text.trim();
      String content = text.substring( dirType == INCLUDE ? INCLUDE.keyword().length() : NEST.keyword().length() ).trim();
      int index = 0;
      while( index < content.length() )
      {
        if( content.charAt( index ) == '(' )
        {
          this.className = content.substring( 0, index ).trim();
          this.params = null;
          if( content.length() > index+1 && content.indexOf( ')', index+1 ) >= 0 )
          {
            this.params = content.substring( index + 1, content.indexOf( ')' ) );
            fillConditional( content.substring( content.indexOf( ')' ) + 1 ).trim() );
          }
          else
          {
            addError( "')' expected", token.getLine() );
          }
          return;
        }
        else if( index < content.length() - 2 && content.charAt( index ) == ' ' && content.charAt( index + 1 ) == 'i' && content.charAt( index + 2 ) == 'f' )
        {
          this.className = content.substring( 0, index ).trim();
          this.params = null;
          if( content.length() > index+1 )
          {
            fillConditional( content.substring( index + 1 ).trim() );
          }
          else
          {
            addError( "'if' condition expected", token.getLine() );
          }
          return;
        }
        index += 1;
      }
      this.className = content;
    }

    /**
     * Helper Method: Takes in a conditional, properly parses it, and sets this.conditional accordingly.
     *
     * @param conditional A statement as follows: if([INSERT CONDITIONAL HERE]), where parentheses are
     *                    optional.
     */
    // todo: this needs grownup attention (it's using hard-coded offsets, instead it should be tokenized etc.)
    private void fillConditional( String conditional )
    {
      if( conditional.length() < 2 )
      {
        return;
      }
      String conditionalWithoutIf = conditional.substring( 2 );
      if( conditionalWithoutIf.length() > 0 && conditionalWithoutIf.charAt( 0 ) == '(' )
      {
        if( conditionalWithoutIf.length() > 2 )
        {
          this.conditional = conditionalWithoutIf.substring( 1, conditionalWithoutIf.length() - 1 );
        }
        else
        {
          addError( "Expecting a condition expression", token.getLine() );
        }
      }
      else
      {
        this.conditional = conditionalWithoutIf;
      }
    }

    /**
     * given a trimmed string of variables,
     * returns a list with a string list per variable with the type and variable name (when both are given)
     * or just the name if both aren't given
     */
    private String[][] splitParamsList( String params )
    {
      params = params.trim();
      params = params.replaceAll( " ,", "," ).replace( ", ", "," );
      String[] parameters = params.split( "," );
      String[][] paramsList = new String[parameters.length][2];
      for( int i = 0; i < parameters.length; i++ )
      {
        paramsList[i] = parameters[i].split( " ", 2 );
      }
      return paramsList;
    }

    //given a list of 2 element String lists (0th elem is type and 1st elem is value), returns the string form
    //ex. [[String, str],[int,5]] returns "String str, int 5"
    private String makeParamsString( String[][] paramsList )
    {
      StringBuilder params = new StringBuilder().append( paramsList[0][0] ).append( " " ).append( paramsList[0][1] );
      for( int i = 1; i < paramsList.length; i++ )
      {
        params.append( ", " ).append( paramsList[i][0] ).append( " " ).append( paramsList[i][1] );
      }
      return params.toString();
    }

    private void findParamTypes( String[][] params, int tokenPos, List<Token> tokens )
    {
      for( int i = 0; i < params.length; i++ )
      {
        if( params[i].length == 1 )
        {
          String name = params[i][0];
          params[i] = new String[2];
          params[i][0] = inferSingleArgumentType( name, tokenPos, tokens );
          params[i][1] = name;
        }
      }
    }

    private String makeParamsStringWithoutTypes( String[][] paramsList )
    {
      StringBuilder params = new StringBuilder( paramsList[0][1] );
      for( int i = 1; i < paramsList.length; i++ )
      {
        params.append( ", " ).append( paramsList[i][1] );
      }
      return params.toString();
    }

    private String inferSingleArgumentType( String name, int tokenPos, List<Token> tokens )
    {
      String pattern = "([a-zA-Z_$][a-zA-Z_$0-9]* " + //First Group: Matches Type arg format
                       name + ")|(\".*[a-zA-Z_$][a-zA-Z_$0-9]* " + //Second & Third Group: Deals with matching within strings
                       name + ".*\")|('.*[a-zA-Z_$][a-zA-Z_$0-9]* " +
                       name + ".*')";
      Pattern argumentRegex = Pattern.compile( pattern );
      for( int i = tokenPos - 1; i >= 0; i -= 1 )
      {
        Token currentToken = tokens.get( i );
        if( currentToken.getType() == Token.TokenType.STMT )
        {
          String text = currentToken.getText();
          text = text.trim();
          Matcher argumentMatcher = argumentRegex.matcher( text );
          String toReturn = null;
          while( argumentMatcher.find() )
          {
            if( argumentMatcher.group( 1 ) != null )
            {
              toReturn = argumentMatcher.group( 1 );
            }
          }
          if( toReturn != null )
          {
            return toReturn.split( " " )[0];
          }
        }
        else if( currentToken.getType() == Token.TokenType.DIRECTIVE )
        {
          Directive cur = new Directive( i, currentToken, tokens );
          if( cur._dirType == PARAMS )
          {
            String[][] outerClassParameters = cur.paramsList;
            for( String[] currentParams: outerClassParameters )
            {
              String parameter = currentParams[1];
              if( name.equals( parameter ) )
              {
                return currentParams[0];
              }
            }
          }
        }
      }
      addError( "Type for argument can not be inferred: " + name, token.getLine() );
      return "";
    }

  }

  class FileGenerator
  {
    private TemplateStringBuilder _sb = new TemplateStringBuilder();
    private ClassInfo _currClass;
    private List<Token> _tokens;
    private Map<Integer, Directive> _dirMap;

    FileGenerator( String fqn, IFile file, URI fileUri, String fileName, String source )
    {
      String className = ManClassUtil.getShortClassName( fqn );
      String packageName = ManClassUtil.getPackage( fqn );
      Tokenizer tokenizer = new Tokenizer();
      _tokens = tokenizer.tokenize( source );

      List<Directive> dirList = getDirectivesList( _tokens );
      _dirMap = getDirectivesMap( dirList );
      _currClass = new ClassInfo( dirList.iterator(), fqn, className, file, fileUri, fileName, _tokens.size() - 1 );
      if( fileUri == null )
      {
        // for tests to avoid files
        _currClass.testSource = source;
      }
      buildFile( packageName, dirList );
    }

    String getFileContents()
    {
      return _sb.toString();
    }

    private void buildFile( String packageName, List<Directive> dirList )
    {
      _sb.append( "package " ).append( packageName + ";\n" )
        .newLine( "import java.io.IOException;" )
        .newLine( "import manifold.templates.rt.ManifoldTemplates;" )
        .newLine( "import manifold.templates.rt.runtime.*;\n" );
      addImports( dirList );
      makeClassContent();
    }

    private boolean containsStringContentOrExpr( List<Token> tokens, Integer start, Integer end )
    {
      if( end == null )
      {
        end = tokens.size() - 1;
      }
      for( int i = start; i <= end; i++ )
      {
        Token token = tokens.get( i );
        Token.TokenType tokenType = token.getType();
        if( tokenType == Token.TokenType.CONTENT || tokenType == Token.TokenType.EXPR )
        {
          return true;
        }
      }
      return false;
    }

    private void addRenderImpl()
    {
      _sb.newLine( "    public void renderImpl(Appendable appendable, ILayout overrideLayout" ).append( safeTrailingString( _currClass.params ) ).append( ") {\n" );
      _sb.append("        renderImpl(appendable, \"\", overrideLayout" );
      appendArgs().append( ");\n" );
      _sb.newLine( "    }" );
      _sb.newLine( "    public void renderImpl(Appendable appendable, String indentation, ILayout overrideLayout" ).append( safeTrailingString( _currClass.params ) ).append( ") {" );
      _sb.newLine( "      WrapAppendable buffer = new WrapAppendable(appendable, indentation);" );
      boolean needsToCatchIO = _currClass.depth == 0;

      if( !needsToCatchIO )
      {
        needsToCatchIO = containsStringContentOrExpr( _tokens, _currClass.startTokenPos - 1, _currClass.endTokenPos );
      }

      if( needsToCatchIO )
      {
        _sb.newLine( "        try {" );
      }

      if( _currClass.isLayout )
      {
        _sb.newLine( "            header(buffer);" )
          .newLine( "            footer(buffer);" );
      }
      else
      {
        String isOuterTemplate = String.valueOf( _currClass.depth == 0 );
        _sb.newLine( "            beforeRender(buffer, overrideLayout, " ).append( isOuterTemplate ).append( ");\n" );
        _sb.newLine( "            long startTime = System.nanoTime();\n" );
        makeFuncContent( _currClass.startTokenPos, _currClass.endTokenPos );
        _sb.newLine( "            long endTime = System.nanoTime();\n" );
        _sb.newLine( "            long duration = (endTime - startTime)/1000000;\n" );
        _sb.newLine( "            afterRender(buffer, overrideLayout, " ).append( isOuterTemplate ).append( ", duration);\n" );
      }

      if( needsToCatchIO )
      {
        _sb.newLine( "        } catch (IOException e) {\n" )
          .newLine( "            throw new RuntimeException(e);\n" )
          .newLine( "        }\n" );
      }

      _sb.newLine( "    }\n\n" );
    }

    private void addRender()
    {
      _sb.newLine( "" )
        .newLine( "    public static String render(" ).append( safeString( _currClass.params ) ).append( ") {" )
        .newLine( "      StringBuilder sb = new StringBuilder();" )
        .newLine( "      renderInto(sb" );
      for( String[] param: safeParamsList() )
      {
        String arg = param.length >= 2 ? param[1] : "err"; // eg. can happen during editing in IJ
        _sb.append( ", " ).append( arg );
      }
      _sb.append( ");" )
        .newLine( "        return sb.toString();" )
        .newLine( "    }\n" );
    }

    private void addRenderInto()
    {
      _sb.newLine( "    public static void renderInto(Appendable buffer" ).append( safeTrailingString( _currClass.params ) ).append( ") {\n" )
        .newLine( "      " + _currClass.name + " instance = new " + _currClass.name + "();" )
        .newLine( "      instance.renderImpl(buffer, null" );
      appendArgs();
      _sb.append( ");\n" )
        .newLine( "    }\n\n" );
    }

    private void addNestInto()
    {
      _sb.newLine( "    public static void nestInto(Appendable buffer, String indentation" ).append( safeTrailingString( _currClass.params ) ).append( ") {\n" )
        .newLine( "      " + _currClass.name + " instance = new " + _currClass.name + "();" )
        .newLine( "      instance.renderImpl(buffer, indentation, null" );
      appendArgs();
      _sb.append( ");\n" )
        .newLine( "    }\n\n" );
    }

    private void addWithoutLayout()
    {
      _sb.newLine( "    public static LayoutOverride withoutLayout() {" )
        .newLine( "        return withLayout(ILayout.EMPTY);" )
        .newLine( "    }\n\n" );
    }

    private void addWithLayout()
    {
      _sb.newLine( "    public static LayoutOverride withLayout(ILayout layout) {" )
        .newLine( "      " + _currClass.name + " instance = new " + _currClass.name + "();" )
        .newLine( "        return instance.new LayoutOverride(layout);" )
        .newLine( "    }\n\n" );
    }


    private void addLayoutOverrideClass()
    {
      _sb.newLine( "    public class LayoutOverride extends BaseLayoutOverride {" )
        // constructor
        .newLine( "       public LayoutOverride(ILayout override) {" )
        .newLine( "         super(override);" )
        .newLine( "       }\n" )
        .newLine( "" )
        // render
        .newLine( "    public String render(" ).append( safeString( _currClass.params ) ).append( ") {" )
        .newLine( "      StringBuilder sb = new StringBuilder();" )
        .newLine( "      renderImpl(sb, getOverride()" );
      appendArgs();
      _sb.append( ");" )
        .newLine( "        return sb.toString();" )
        .newLine( "    }\n" )
        // renderInto
        .newLine( "    public void renderInto(Appendable sb" ).append( safeTrailingString( _currClass.params ) ).append( ") {" )
        .newLine( "      renderImpl(sb, getOverride()" );
      appendArgs();
      _sb.append( ");" )
        .newLine( "    }\n" )
        // nestInto
        .newLine( "    public void nestInto(Appendable sb, String indentation" ).append( safeTrailingString( _currClass.params ) ).append( ") {" )
        .newLine( "      renderImpl(sb, indentation, getOverride()" );
      appendArgs();
      _sb.append( ");" )
        .newLine( "    }\n" )
        // close class
        .newLine( "    }\n" );
    }

    private TemplateStringBuilder appendArgs()
    {
      for( String[] param: safeParamsList() )
      {
        if( param.length > 1 )
        {
          _sb.append( ", " ).append( param[1] );
        }
      }
      return _sb;
    }

    private String safeTrailingString( String string )
    {
      if( string != null && string.length() > 0 )
      {
        return ", " + string;
      }
      else
      {
        return "";
      }
    }

    private String safeString( String string )
    {
      if( string != null && string.length() > 0 )
      {
        return string;
      }
      else
      {
        return "";
      }
    }

    private String[][] safeParamsList()
    {
      String[][] paramsList = _currClass.paramsList;
      if( paramsList == null )
      {
        paramsList = new String[0][0];
      }
      return paramsList;
    }

    private void addFileHeader()
    {
      _sb.newLine( "\n" );
      if( _currClass.depth == 0 )
      {
        //       sb.newLine( '@'+DisableStringLiteralTemplates.class.getTypeName() );
        if( _currClass.isLayout )
        {
          _sb.newLine( "public class " ).append( _currClass.name ).append( " extends " ).append( _currClass.superClass ).append( " implements " ).append( LAYOUT_INTERFACE ).append( " {" );
        }
        else
        {
          _sb.newLine( "public class " ).append( _currClass.name ).append( " extends " ).append( _currClass.superClass ).append( " {" );
        }
      }
      else
      {
        _sb.newLine( "public static class " ).append( _currClass.name ).append( " extends " ).append( _currClass.superClass ).append( " {" );
      }
      _sb.newLine( "    private " ).append( _currClass.name ).append( "(){" );
      if( _currClass.hasLayout )
      {
        _sb.newLine( "        setLayout(" ).append( _currClass.layoutDir.className ).append( ".asLayout());" );
      }
      _sb.newLine( "    }\n" );
    }


    private void makeClassContent()
    {
      addFileHeader();
      addGetTemplateResourceAsStream();
      addRender();
      addLayoutOverrideClass();
      addWithoutLayout();
      addWithLayout();
      addRenderInto();
      addNestInto();
      addRenderImpl();

      if( _currClass.isLayout )
      {
        addHeaderAndFooter();
      }
      for( ClassInfo nested: _currClass.nestedClasses.values() )
      {
        _currClass = nested;
        makeClassContent();
      }
      //close class
      _sb.newLine( "}\n" );
    }

    private void addGetTemplateResourceAsStream()
    {
      if( _currClass.isFragment() )
      {
        _sb.newLine( "    @" + DisableStringLiteralTemplates.class.getTypeName() );
        _sb.newLine( "    protected java.io.InputStream getTemplateResourceAsStream() {" )
          .newLine( "        return new java.io.ByteArrayInputStream(\"" +
                    ManEscapeUtil.escapeForJavaStringLiteral( _currClass.getFragmentText() ) + "\".getBytes());" )
          .newLine( "    }" );
      }
      else
      {
        _sb.newLine( "    protected java.io.InputStream getTemplateResourceAsStream() {" )
          .newLine( "        return " + (_currClass.fileUri == null ? "null" : "getClass().getResourceAsStream(\"" + getTemplateFilePath() + "\");") )
          .newLine( "    }" );
      }

      if( _currClass.testSource != null )
      {
        //!! only for tests
        _sb.newLine( "    @" + DisableStringLiteralTemplates.class.getTypeName() );
        _sb.newLine( "    protected String getTemplateText() {" )
          .newLine( "        return \"" + _currClass.testSource.replace( "\"", "\\\\\"" ).replace( "\n", "\\\\n" ) + "\";" )
          .newLine( "    }" );
      }
    }

    private String getTemplateFilePath()
    {
      String className = ManClassUtil.getShortClassName( _currClass.fqn );
      String uri = _currClass.fileUri.toString();
      int nameIndex = uri.lastIndexOf( className );
      String fileExt = uri.substring( nameIndex + className.length() );
      return '/' + _currClass.fqn.replace( '.', '/' ) + fileExt;
    }

    private void addHeaderAndFooter()
    {
      _sb.newLine( "    public static " ).append( LAYOUT_INTERFACE ).append( " asLayout() {" )
        .newLine( "        return new " + _currClass.name + "();" )
        .newLine( "    }\n" )
        .newLine( "    @Override" )
        .newLine( "    public void header(Appendable buffer) throws IOException {" )
        .newLine( "        if (getExplicitLayout() != null) {" )
        .newLine( "            getExplicitLayout().header(buffer);" )
        .newLine( "        }" );
      assert (_currClass.depth == 0);
      makeFuncContent( _currClass.startTokenPos, _currClass.contentPos );
      _sb.newLine( "    }" )
        .newLine( "    @Override" )
        .newLine( "    public void footer(Appendable buffer) throws IOException {" );
      makeFuncContent( _currClass.contentPos, _currClass.endTokenPos );
      _sb.newLine( "        if (getExplicitLayout() != null) {" )
        .newLine( "            getExplicitLayout().footer(buffer);" )
        .newLine( "    }\n}" );
    }

    private List<Directive> getDirectivesList( List<Token> tokens )
    {
      ArrayList<Directive> dirList = new ArrayList<>();

      for( int i = 0; i < tokens.size(); i++ )
      {
        Token token = tokens.get( i );
        if( token.getType() == Token.TokenType.DIRECTIVE )
        {
          dirList.add( new Directive( i, token, tokens ) );
        }
      }
      return dirList;
    }

    private Map<Integer, Directive> getDirectivesMap( List<Directive> dirList )
    {
      Map<Integer, Directive> dirMap = new HashMap<>();
      for( Directive dir: dirList )
      {
        dirMap.put( dir.tokenPos, dir );
      }
      return dirMap;
    }

    private void addImports( List<Directive> dirList )
    {
      for( Directive dir: dirList )
      {
        if( dir._dirType == IMPORT )
        {
          // verbatim
          _sb.newLine( dir.token.getText().trim() + ";" );
        }
      }
    }

    private void makeFuncContent( Integer startPos, Integer endPos )
    {
      ArrayList<Integer> templateLineNumbers = new ArrayList<>();
      if( endPos == null )
      {
        endPos = _tokens.size() - 1;
      }
      _sb.newLine( "            int lineStart = Thread.currentThread().getStackTrace()[1].getLineNumber() + 1;" );
      _sb.newLine( "            try {" );
      int lastTokenIndex = -1;
      outerLoop:
      for( int i = startPos; i <= endPos; i++ )
      {
        Token token = _tokens.get( i );
        switch( token.getType() )
        {
          case CONTENT:
          {
            int[] loc = makeText( lastTokenIndex, nextTokenType( i + 1, endPos ), token );
            if( loc != null )
            {
              _sb.newLine( "                buffer.append(getTemplateText(), " + loc[0] + ", " + loc[1] + ");" );
              // sb.newLine( "                buffer.append(\"" ).append( text.replaceAll( "\"", "\\\\\"" ).replaceAll( "\r", "" ).replaceAll( "\n", "\\\\n" ) + "\");" );
              templateLineNumbers.add( token.getLine() );
            }
            break;
          }
          case STMT:
          {
            String[] statementList = token.getText().split( "\n" );
            for( int j = 0; j < statementList.length; j++ )
            {
              String statement = statementList[j].trim();
              _sb.append( "                " ).append( statement ).append( "\n" );
              templateLineNumbers.add( token.getLine() + j );
            }
            break;
          }
          case EXPR:
          {
            _sb.newLine( "                buffer.append(toS(" ).append( token.getText() ).append( "));" );
            templateLineNumbers.add( token.getLine() );
            break;
          }
          case COMMENT:
          {
            break;
          }
          case DIRECTIVE:
          {
            Directive dir = _dirMap.get( i );
            if( dir._dirType == SECTION )
            {
              ClassInfo classToSkipOver = _currClass.nestedClasses.get( i + 1 );
              if( classToSkipOver.endTokenPos == null )
              {
                i = endPos;
              }
              else
              {
                i = classToSkipOver.endTokenPos;
              }
              addSection( dir );
            }
            else if( dir._dirType == END_SECTION )
            {
              break outerLoop;
            }
            else if( dir._dirType == INCLUDE )
            {
              addInclude( dir );
            }
            else if( dir._dirType == NEST )
            {
              addNest( dir, i );
            }
            else if( dir._dirType == CONTENT )
            {
              break;
            }
            break;
          }
          default:
            continue;
        }
        lastTokenIndex = i;
      }
      String nums = templateLineNumbers.toString().substring( 1, templateLineNumbers.toString().length() - 1 );

      _sb.newLine( "            } catch (RuntimeException e) {" );
      _sb.newLine( "                int[] templateLineNumbers = new int[]{" ).append( nums ).append( "};" );
      _sb.newLine( "                handleException(e, \"" ).append( _currClass.fileName ).append( "\", lineStart, templateLineNumbers);\n            }" );
    }

    private Token.TokenType nextTokenType( int index, Integer endPos )
    {
      if( index <= endPos )
      {
        return _tokens.get( index ).getType();
      }
      return null;
    }

    private int[] makeText( int prevTokenIndex, Token.TokenType nextTokenType, Token token )
    {
      int[] loc = null;
      String text = token.getText();
      Token prevToken = prevTokenIndex < 0 ? null :_tokens.get( prevTokenIndex );
      Token.TokenType prevTokenType = prevToken == null ? null : prevToken.getType();
      if( text != null && text.length() > 0 )
      {
        int offset = token.getOffset();
        if( prevTokenType != Token.TokenType.CONTENT &&
            prevTokenType != Token.TokenType.EXPR &&
            !(prevTokenType == Token.TokenType.DIRECTIVE && _dirMap.get( prevTokenIndex )._dirType == NEST) )
        {
          if( text.charAt( 0 ) == '\n' )
          {
            // remove leading new line (which follows the preceding non-content token)
            offset++;
          }
          else if( text.length() > 1 && text.charAt( 0 ) == '\r' && text.charAt( 1 ) == '\n' )
          {
            // remove leading new line (which follows the preceding non-content token)
            offset += 2;
          }
        }
        // remove trailing indentation (which precedes the following non-content token)
        int length = removeTrailingIndentation( text, nextTokenType );
        if( length > 0 )
        {
          length -= (offset - token.getOffset());
          if( length > 0 )
          {
            loc = new int[]{offset, offset + length};
          }
        }
      }
      return loc;
    }

    private int removeTrailingIndentation( String text, Token.TokenType nextTokenType )
    {
      int length = text.length();
      if( text.length() > 0 &&
          nextTokenType != Token.TokenType.CONTENT &&
          nextTokenType != Token.TokenType.EXPR_ANGLE_BEGIN &&
          nextTokenType != Token.TokenType.EXPR_BRACE_BEGIN )
      {
        if( isSpaces( text ) )
        {
          length = 0;
        }
        else
        {
          int iEol = text.lastIndexOf( '\n' );
          if( iEol >= 0 )
          {
            for( int i = text.length() - 1; i >= iEol; i-- )
            {
              char c = text.charAt( i );
              if( c != ' ' && c != '\t' )
              {
                length = i + 1;
                break;
              }
            }
          }
        }
      }
      return length;
    }

    private boolean isSpaces( String text )
    {
      int length = text.length();
      for( int i = 0; i < length; i++ )
      {
        char c = text.charAt( i );
        if( c != ' ' && c != '\t' )
        {
          return false;
        }
      }
      return true;
    }

    private void addInclude( Directive dir )
    {
      assert (dir._dirType == INCLUDE);
      if( dir.conditional != null )
      {
        _sb.newLine( "            if(" ).append( dir.conditional ).append( "){" );
      }
      _sb.newLine( "            " ).append( dir.className ).append( ".withoutLayout().renderInto(buffer" ).append( safeTrailingString( dir.params ) ).append( ");" );
      if( dir.conditional != null )
      {
        _sb.newLine( "            " ).append( "}" );
      }
    }

    private void addNest( Directive dir, int index )
    {
      assert (dir._dirType == NEST);

      //noinspection unused
      String indentation = getIndentation( index - 2 );

      if( dir.conditional != null )
      {
        _sb.newLine( "            if(" ).append( dir.conditional ).append( "){" );
      }
      _sb.newLine( "            " ).append( dir.className ).append( ".withoutLayout().nestInto(buffer, \"" + indentation + "\"" ).append( safeTrailingString( dir.params ) ).append( ");" );
      if( dir.conditional != null )
      {
        _sb.newLine( "            " ).append( "}" );
      }
    }

    private String getIndentation( int index )
    {
      if( index < 0 )
      {
        return "";
      }
      Token token = _tokens.get( index );
      if( token.getType() == Token.TokenType.CONTENT )
      {
        StringBuilder indent = new StringBuilder();
        String text = token.getText();
        int len = text.length();
        for( int i = 0; i < len; i++ )
        {
          char c = text.charAt( len - i - 1 );
          if( c == ' ' || c == '\t' )
          {
            indent.insert( 0, c );
          }
          else
          {
            break;
          }
        }
        return indent.toString();
      }
      return "";
    }

    private void addSection( Directive dir )
    {
      assert (dir._dirType == SECTION);
      if( dir.params != null )
      {
        String paramsWithoutTypes = dir.makeParamsStringWithoutTypes( dir.paramsList );
        _sb.newLine( "            " ).append( dir.className ).append( ".renderInto(buffer, " ).append( paramsWithoutTypes ).append( ");" );
      }
      else
      {
        _sb.newLine( "            " ).append( dir.className ).append( ".renderInto(buffer);" );
      }
    }

    private class TemplateStringBuilder
    {
      private final String INDENT = "    ";
      private StringBuilder sb = new StringBuilder();

      TemplateStringBuilder newLine( String content )
      {
        sb.append( "\n" );
        for( int i = 0; i < _currClass.depth; i++ )
        {
          sb.append( INDENT );
        }
        sb.append( content );
        return this;
      }

      TemplateStringBuilder append( String content )
      {
        sb.append( content );
        return this;
      }

      public String toString()
      {
        return sb.toString();
      }
    }
  }
}