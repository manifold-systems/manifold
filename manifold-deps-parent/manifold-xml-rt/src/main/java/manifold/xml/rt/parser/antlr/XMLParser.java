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

package manifold.xml.rt.parser.antlr;
// Generated from XMLParser.g4 by ANTLR 4.4

import java.util.List;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class XMLParser extends Parser
{
  static
  {
    RuntimeMetaData.checkVersion( "4.7", RuntimeMetaData.VERSION );
  }

  protected static final DFA[] _decisionToDFA;
  protected static final PredictionContextCache _sharedContextCache =
    new PredictionContextCache();
  public static final int
    COMMENT = 1, CDATA = 2, DTD = 3, EntityRef = 4, CharRef = 5, SEA_WS = 6, OPEN = 7, XMLDeclOpen = 8,
    TEXT = 9, CLOSE = 10, SPECIAL_CLOSE = 11, SLASH_CLOSE = 12, SLASH = 13, EQUALS = 14,
    STRING = 15, Name = 16, S = 17, PI = 18;
  public static final String[] tokenNames = {
    "<INVALID>", "COMMENT", "CDATA", "DTD", "EntityRef", "CharRef", "SEA_WS",
    "'<'", "XMLDeclOpen", "TEXT", "'>'", "SPECIAL_CLOSE", "'/>'", "'/'", "'='",
    "STRING", "Name", "S", "PI"
  };
  public static final int
    RULE_document = 0, RULE_prolog = 1, RULE_content = 2, RULE_element = 3,
    RULE_reference = 4, RULE_attribute = 5, RULE_chardata = 6, RULE_misc = 7;
  public static final String[] ruleNames = {
    "document", "prolog", "content", "element", "reference", "attribute",
    "chardata", "misc"
  };

  @Override
  public String getGrammarFileName()
  {
    return "XMLParser.g4";
  }

  @Override
  public String[] getTokenNames()
  {
    return tokenNames;
  }

  @Override
  public String[] getRuleNames()
  {
    return ruleNames;
  }

  @Override
  public String getSerializedATN()
  {
    return _serializedATN;
  }

  @Override
  public ATN getATN()
  {
    return _ATN;
  }

  public XMLParser( TokenStream input )
  {
    super( input );
    _interp = new ParserATNSimulator( this, _ATN, _decisionToDFA, _sharedContextCache );
  }

  public static class DocumentContext extends ParserRuleContext
  {
    public MiscContext misc( int i )
    {
      return getRuleContext( MiscContext.class, i );
    }

    public ElementContext element()
    {
      return getRuleContext( ElementContext.class, 0 );
    }

    public List<MiscContext> misc()
    {
      return getRuleContexts( MiscContext.class );
    }

    public PrologContext prolog()
    {
      return getRuleContext( PrologContext.class, 0 );
    }

    public DocumentContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_document;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterDocument( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitDocument( this );
      }
    }
  }

  public final DocumentContext document() throws RecognitionException
  {
    DocumentContext _localctx = new DocumentContext( _ctx, getState() );
    enterRule( _localctx, 0, RULE_document );
    int _la;
    try
    {
      enterOuterAlt( _localctx, 1 );
      {
        setState( 17 );
        _la = _input.LA( 1 );
        if( _la == XMLDeclOpen )
        {
          {
            setState( 16 );
            prolog();
          }
        }

        setState( 22 );
        _errHandler.sync( this );
        _la = _input.LA( 1 );
        while( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << COMMENT) | (1L << SEA_WS) | (1L << PI))) != 0) )
        {
          {
            {
              setState( 19 );
              misc();
            }
          }
          setState( 24 );
          _errHandler.sync( this );
          _la = _input.LA( 1 );
        }
        setState( 25 );
        element();
        setState( 29 );
        _errHandler.sync( this );
        _la = _input.LA( 1 );
        while( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << COMMENT) | (1L << SEA_WS) | (1L << PI))) != 0) )
        {
          {
            {
              setState( 26 );
              misc();
            }
          }
          setState( 31 );
          _errHandler.sync( this );
          _la = _input.LA( 1 );
        }
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class PrologContext extends ParserRuleContext
  {
    public List<AttributeContext> attribute()
    {
      return getRuleContexts( AttributeContext.class );
    }

    public TerminalNode SPECIAL_CLOSE()
    {
      return getToken( XMLParser.SPECIAL_CLOSE, 0 );
    }

    public AttributeContext attribute( int i )
    {
      return getRuleContext( AttributeContext.class, i );
    }

    public TerminalNode XMLDeclOpen()
    {
      return getToken( XMLParser.XMLDeclOpen, 0 );
    }

    public PrologContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_prolog;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterProlog( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitProlog( this );
      }
    }
  }

  public final PrologContext prolog() throws RecognitionException
  {
    PrologContext _localctx = new PrologContext( _ctx, getState() );
    enterRule( _localctx, 2, RULE_prolog );
    int _la;
    try
    {
      enterOuterAlt( _localctx, 1 );
      {
        setState( 32 );
        match( XMLDeclOpen );
        setState( 36 );
        _errHandler.sync( this );
        _la = _input.LA( 1 );
        while( _la == Name )
        {
          {
            {
              setState( 33 );
              attribute();
            }
          }
          setState( 38 );
          _errHandler.sync( this );
          _la = _input.LA( 1 );
        }
        setState( 39 );
        match( SPECIAL_CLOSE );
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class ContentContext extends ParserRuleContext
  {
    public TerminalNode CDATA( int i )
    {
      return getToken( XMLParser.CDATA, i );
    }

    public List<TerminalNode> COMMENT()
    {
      return getTokens( XMLParser.COMMENT );
    }

    public List<ElementContext> element()
    {
      return getRuleContexts( ElementContext.class );
    }

    public List<ChardataContext> chardata()
    {
      return getRuleContexts( ChardataContext.class );
    }

    public List<TerminalNode> PI()
    {
      return getTokens( XMLParser.PI );
    }

    public List<ReferenceContext> reference()
    {
      return getRuleContexts( ReferenceContext.class );
    }

    public TerminalNode PI( int i )
    {
      return getToken( XMLParser.PI, i );
    }

    public ChardataContext chardata( int i )
    {
      return getRuleContext( ChardataContext.class, i );
    }

    public ElementContext element( int i )
    {
      return getRuleContext( ElementContext.class, i );
    }

    public ReferenceContext reference( int i )
    {
      return getRuleContext( ReferenceContext.class, i );
    }

    public TerminalNode COMMENT( int i )
    {
      return getToken( XMLParser.COMMENT, i );
    }

    public List<TerminalNode> CDATA()
    {
      return getTokens( XMLParser.CDATA );
    }

    public ContentContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_content;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterContent( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitContent( this );
      }
    }
  }

  public final ContentContext content() throws RecognitionException
  {
    ContentContext _localctx = new ContentContext( _ctx, getState() );
    enterRule( _localctx, 4, RULE_content );
    int _la;
    try
    {
      int _alt;
      enterOuterAlt( _localctx, 1 );
      {
        setState( 42 );
        _la = _input.LA( 1 );
        if( _la == SEA_WS || _la == TEXT )
        {
          {
            setState( 41 );
            chardata();
          }
        }

        setState( 56 );
        _errHandler.sync( this );
        _alt = getInterpreter().adaptivePredict( _input, 7, _ctx );
        while( _alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER )
        {
          if( _alt == 1 )
          {
            {
              {
                setState( 49 );
                switch( _input.LA( 1 ) )
                {
                  case OPEN:
                  {
                    setState( 44 );
                    element();
                  }
                  break;
                  case EntityRef:
                  case CharRef:
                  {
                    setState( 45 );
                    reference();
                  }
                  break;
                  case CDATA:
                  {
                    setState( 46 );
                    match( CDATA );
                  }
                  break;
                  case PI:
                  {
                    setState( 47 );
                    match( PI );
                  }
                  break;
                  case COMMENT:
                  {
                    setState( 48 );
                    match( COMMENT );
                  }
                  break;
                  default:
                    throw new NoViableAltException( this );
                }
                setState( 52 );
                _la = _input.LA( 1 );
                if( _la == SEA_WS || _la == TEXT )
                {
                  {
                    setState( 51 );
                    chardata();
                  }
                }

              }
            }
          }
          setState( 58 );
          _errHandler.sync( this );
          _alt = getInterpreter().adaptivePredict( _input, 7, _ctx );
        }
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class ElementContext extends ParserRuleContext
  {
    public List<AttributeContext> attribute()
    {
      return getRuleContexts( AttributeContext.class );
    }

    public ContentContext content()
    {
      return getRuleContext( ContentContext.class, 0 );
    }

    public AttributeContext attribute( int i )
    {
      return getRuleContext( AttributeContext.class, i );
    }

    public TerminalNode Name( int i )
    {
      return getToken( XMLParser.Name, i );
    }

    public List<TerminalNode> Name()
    {
      return getTokens( XMLParser.Name );
    }

    public ElementContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_element;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterElement( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitElement( this );
      }
    }
  }

  public final ElementContext element() throws RecognitionException
  {
    ElementContext _localctx = new ElementContext( _ctx, getState() );
    enterRule( _localctx, 6, RULE_element );
    int _la;
    try
    {
      setState( 83 );
      switch( getInterpreter().adaptivePredict( _input, 10, _ctx ) )
      {
        case 1:
          enterOuterAlt( _localctx, 1 );
        {
          setState( 59 );
          match( OPEN );
          setState( 60 );
          match( Name );
          setState( 64 );
          _errHandler.sync( this );
          _la = _input.LA( 1 );
          while( _la == Name )
          {
            {
              {
                setState( 61 );
                attribute();
              }
            }
            setState( 66 );
            _errHandler.sync( this );
            _la = _input.LA( 1 );
          }
          setState( 67 );
          match( CLOSE );
          setState( 68 );
          content();
          setState( 69 );
          match( OPEN );
          setState( 70 );
          match( SLASH );
          setState( 71 );
          match( Name );
          setState( 72 );
          match( CLOSE );
        }
        break;
        case 2:
          enterOuterAlt( _localctx, 2 );
        {
          setState( 74 );
          match( OPEN );
          setState( 75 );
          match( Name );
          setState( 79 );
          _errHandler.sync( this );
          _la = _input.LA( 1 );
          while( _la == Name )
          {
            {
              {
                setState( 76 );
                attribute();
              }
            }
            setState( 81 );
            _errHandler.sync( this );
            _la = _input.LA( 1 );
          }
          setState( 82 );
          match( SLASH_CLOSE );
        }
        break;
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class ReferenceContext extends ParserRuleContext
  {
    public TerminalNode CharRef()
    {
      return getToken( XMLParser.CharRef, 0 );
    }

    public TerminalNode EntityRef()
    {
      return getToken( XMLParser.EntityRef, 0 );
    }

    public ReferenceContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_reference;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterReference( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitReference( this );
      }
    }
  }

  public final ReferenceContext reference() throws RecognitionException
  {
    ReferenceContext _localctx = new ReferenceContext( _ctx, getState() );
    enterRule( _localctx, 8, RULE_reference );
    int _la;
    try
    {
      enterOuterAlt( _localctx, 1 );
      {
        setState( 85 );
        _la = _input.LA( 1 );
        if( !(_la == EntityRef || _la == CharRef) )
        {
          _errHandler.recoverInline( this );
        }
        consume();
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class AttributeContext extends ParserRuleContext
  {
    public TerminalNode STRING()
    {
      return getToken( XMLParser.STRING, 0 );
    }

    public TerminalNode Name()
    {
      return getToken( XMLParser.Name, 0 );
    }

    public AttributeContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_attribute;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterAttribute( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitAttribute( this );
      }
    }
  }

  public final AttributeContext attribute() throws RecognitionException
  {
    AttributeContext _localctx = new AttributeContext( _ctx, getState() );
    enterRule( _localctx, 10, RULE_attribute );
    try
    {
      enterOuterAlt( _localctx, 1 );
      {
        setState( 87 );
        match( Name );
        setState( 88 );
        match( EQUALS );
        setState( 89 );
        match( STRING );
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class ChardataContext extends ParserRuleContext
  {
    public TerminalNode TEXT()
    {
      return getToken( XMLParser.TEXT, 0 );
    }

    public TerminalNode SEA_WS()
    {
      return getToken( XMLParser.SEA_WS, 0 );
    }

    public ChardataContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_chardata;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterChardata( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitChardata( this );
      }
    }
  }

  public final ChardataContext chardata() throws RecognitionException
  {
    ChardataContext _localctx = new ChardataContext( _ctx, getState() );
    enterRule( _localctx, 12, RULE_chardata );
    int _la;
    try
    {
      enterOuterAlt( _localctx, 1 );
      {
        setState( 91 );
        _la = _input.LA( 1 );
        if( !(_la == SEA_WS || _la == TEXT) )
        {
          _errHandler.recoverInline( this );
        }
        consume();
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static class MiscContext extends ParserRuleContext
  {
    public TerminalNode COMMENT()
    {
      return getToken( XMLParser.COMMENT, 0 );
    }

    public TerminalNode SEA_WS()
    {
      return getToken( XMLParser.SEA_WS, 0 );
    }

    public TerminalNode PI()
    {
      return getToken( XMLParser.PI, 0 );
    }

    public MiscContext( ParserRuleContext parent, int invokingState )
    {
      super( parent, invokingState );
    }

    @Override
    public int getRuleIndex()
    {
      return RULE_misc;
    }

    @Override
    public void enterRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).enterMisc( this );
      }
    }

    @Override
    public void exitRule( ParseTreeListener listener )
    {
      if( listener instanceof XMLParserListener )
      {
        ((XMLParserListener)listener).exitMisc( this );
      }
    }
  }

  public final MiscContext misc() throws RecognitionException
  {
    MiscContext _localctx = new MiscContext( _ctx, getState() );
    enterRule( _localctx, 14, RULE_misc );
    int _la;
    try
    {
      enterOuterAlt( _localctx, 1 );
      {
        setState( 93 );
        _la = _input.LA( 1 );
        if( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << COMMENT) | (1L << SEA_WS) | (1L << PI))) != 0)) )
        {
          _errHandler.recoverInline( this );
        }
        consume();
      }
    }
    catch( RecognitionException re )
    {
      _localctx.exception = re;
      _errHandler.reportError( this, re );
      _errHandler.recover( this, re );
    }
    finally
    {
      exitRule();
    }
    return _localctx;
  }

  public static final String _serializedATN =
    "\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\24b\4\2\t\2\4\3\t" +
    "\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\5\2\24\n\2\3\2" +
    "\7\2\27\n\2\f\2\16\2\32\13\2\3\2\3\2\7\2\36\n\2\f\2\16\2!\13\2\3\3\3\3" +
    "\7\3%\n\3\f\3\16\3(\13\3\3\3\3\3\3\4\5\4-\n\4\3\4\3\4\3\4\3\4\3\4\5\4" +
    "\64\n\4\3\4\5\4\67\n\4\7\49\n\4\f\4\16\4<\13\4\3\5\3\5\3\5\7\5A\n\5\f" +
    "\5\16\5D\13\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\7\5P\n\5\f\5\16" +
    "\5S\13\5\3\5\5\5V\n\5\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\3\t\3\t\3\t\2\2" +
    "\n\2\4\6\b\n\f\16\20\2\5\3\2\6\7\4\2\b\b\13\13\5\2\3\3\b\b\24\24g\2\23" +
    "\3\2\2\2\4\"\3\2\2\2\6,\3\2\2\2\bU\3\2\2\2\nW\3\2\2\2\fY\3\2\2\2\16]\3" +
    "\2\2\2\20_\3\2\2\2\22\24\5\4\3\2\23\22\3\2\2\2\23\24\3\2\2\2\24\30\3\2" +
    "\2\2\25\27\5\20\t\2\26\25\3\2\2\2\27\32\3\2\2\2\30\26\3\2\2\2\30\31\3" +
    "\2\2\2\31\33\3\2\2\2\32\30\3\2\2\2\33\37\5\b\5\2\34\36\5\20\t\2\35\34" +
    "\3\2\2\2\36!\3\2\2\2\37\35\3\2\2\2\37 \3\2\2\2 \3\3\2\2\2!\37\3\2\2\2" +
    "\"&\7\n\2\2#%\5\f\7\2$#\3\2\2\2%(\3\2\2\2&$\3\2\2\2&\'\3\2\2\2\')\3\2" +
    "\2\2(&\3\2\2\2)*\7\r\2\2*\5\3\2\2\2+-\5\16\b\2,+\3\2\2\2,-\3\2\2\2-:\3" +
    "\2\2\2.\64\5\b\5\2/\64\5\n\6\2\60\64\7\4\2\2\61\64\7\24\2\2\62\64\7\3" +
    "\2\2\63.\3\2\2\2\63/\3\2\2\2\63\60\3\2\2\2\63\61\3\2\2\2\63\62\3\2\2\2" +
    "\64\66\3\2\2\2\65\67\5\16\b\2\66\65\3\2\2\2\66\67\3\2\2\2\679\3\2\2\2" +
    "8\63\3\2\2\29<\3\2\2\2:8\3\2\2\2:;\3\2\2\2;\7\3\2\2\2<:\3\2\2\2=>\7\t" +
    "\2\2>B\7\22\2\2?A\5\f\7\2@?\3\2\2\2AD\3\2\2\2B@\3\2\2\2BC\3\2\2\2CE\3" +
    "\2\2\2DB\3\2\2\2EF\7\f\2\2FG\5\6\4\2GH\7\t\2\2HI\7\17\2\2IJ\7\22\2\2J" +
    "K\7\f\2\2KV\3\2\2\2LM\7\t\2\2MQ\7\22\2\2NP\5\f\7\2ON\3\2\2\2PS\3\2\2\2" +
    "QO\3\2\2\2QR\3\2\2\2RT\3\2\2\2SQ\3\2\2\2TV\7\16\2\2U=\3\2\2\2UL\3\2\2" +
    "\2V\t\3\2\2\2WX\t\2\2\2X\13\3\2\2\2YZ\7\22\2\2Z[\7\20\2\2[\\\7\21\2\2" +
    "\\\r\3\2\2\2]^\t\3\2\2^\17\3\2\2\2_`\t\4\2\2`\21\3\2\2\2\r\23\30\37&," +
    "\63\66:BQU";
  public static final ATN _ATN =
    new ATNDeserializer().deserialize( _serializedATN.toCharArray() );

  static
  {
    _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    for( int i = 0; i < _ATN.getNumberOfDecisions(); i++ )
    {
      _decisionToDFA[i] = new DFA( _ATN.getDecisionState( i ), i );
    }
  }
}
