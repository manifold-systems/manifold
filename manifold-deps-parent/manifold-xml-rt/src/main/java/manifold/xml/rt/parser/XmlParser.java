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

package manifold.xml.rt.parser;

import java.io.InputStream;
import java.util.Stack;
import manifold.rt.api.util.Pair;
import manifold.xml.rt.parser.antlr.XMLParser;
import manifold.xml.rt.parser.antlr.XMLParserBaseListener;
import manifold.xml.rt.parser.antlr.gen.XMLLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

public class XmlParser
{
  private XmlElement _root = null;

  public static XmlElement parse( InputStream inputStream )
  {
    return new XmlParser( inputStream )._root;
  }

  @SuppressWarnings("WeakerAccess")
  public static XMLParser.DocumentContext parseRaw( InputStream inputStream )
  {
    try
    {
      XMLLexer lexer = new XMLLexer( CharStreams.fromStream( inputStream ) );
      CommonTokenStream tokens = new CommonTokenStream( lexer );
      XMLParser parser = new XMLParser( tokens );
      return parser.document();
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private XmlParser( InputStream inputStream )
  {
    try
    {
      XMLParser.DocumentContext ctx = parseRaw( inputStream );
      ParseTreeWalker walker = new ParseTreeWalker();
      XmlBuilder builder = new XmlBuilder();
      walker.walk( builder, ctx );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private class XmlBuilder extends XMLParserBaseListener
  {
    private XmlElement _prolog;
    private Stack<Pair<ParserRuleContext, XmlElement>> _elements = new Stack<>();
    private XmlAttribute _attribute;

    @Override
    public void enterProlog( XMLParser.PrologContext ctx )
    {
      _prolog = new XmlElement( ctx, null );
      _elements.push( new Pair<>( ctx, _prolog ) );
    }

    @Override
    public void exitProlog( XMLParser.PrologContext ctx )
    {
      _elements.pop();
    }

    public void enterElement( XMLParser.ElementContext ctx )
    {
      XmlElement parent = _elements.isEmpty() ? null : _elements.peek().getSecond();
      XmlElement xmlElement;
      if( parent == null )
      {
        xmlElement = new XmlRootElement( ctx, _prolog );
        _prolog = null;
      }
      else
      {
        xmlElement = new XmlElement( ctx, parent );
        parent.addChild( xmlElement );
      }
      _elements.push( new Pair<>( ctx, xmlElement ) );
      if( _root == null )
      {
        _root = xmlElement;
      }
    }

    @Override
    public void exitElement( XMLParser.ElementContext ctx )
    {
      Pair<ParserRuleContext, XmlElement> popped = _elements.pop();
      if( popped.getFirst() != ctx )
      {
        throw new IllegalStateException( "Unbalanced elements, expecting '" + ctx.Name( 0 ) +
                                         "' but found '" + popped.getFirst() + "'" );
      }
    }

    @Override
    public void enterAttribute( XMLParser.AttributeContext ctx )
    {
      if( _attribute != null )
      {
        throw new IllegalStateException( "Error processing attribute '" + ctx.Name().getText() + "'," +
                                         " there is already an attribute processing: '" + _attribute.getName().getRawText() );
      }
      XmlElement parent = _elements.peek().getSecond();
      _attribute = new XmlAttribute( ctx, parent );
      parent.addAttribute( _attribute );
    }

    @Override
    public void exitAttribute( XMLParser.AttributeContext ctx )
    {
      if( _attribute == null )
      {
        throw new IllegalStateException( "Expecting non-null attribute during exitAttribute()" );
      }
      _attribute = null;
    }

    @Override
    public void visitTerminal( TerminalNode node )
    {
      Token symbol = node.getSymbol();
      if( _attribute != null )
      {
        if( symbol.getType() == XMLLexer.STRING )
        {
          _attribute.setRawValue( new XmlTerminal( symbol, _attribute ) );
        }
      }
      else if( !_elements.isEmpty() )
      {
        if( symbol.getType() == XMLLexer.TEXT ||
            symbol.getType() == XMLLexer.STRING ||
            symbol.getType() == XMLLexer.CDATA )
        {
          XmlElement second = _elements.peek().getSecond();
          second.setRawContent( new XmlTerminal( symbol, second ) );
        }
      }
    }
  }
}