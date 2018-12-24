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

package manifold.util.xml.gen;
// Generated from XMLParser.g4 by ANTLR 4.4

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link XMLParser}.
 */
public interface XMLParserListener extends ParseTreeListener
{
  /**
   * Enter a parse tree produced by {@link XMLParser#reference}.
   *
   * @param ctx the parse tree
   */
  void enterReference( @NotNull XMLParser.ReferenceContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#reference}.
   *
   * @param ctx the parse tree
   */
  void exitReference( @NotNull XMLParser.ReferenceContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#document}.
   *
   * @param ctx the parse tree
   */
  void enterDocument( @NotNull XMLParser.DocumentContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#document}.
   *
   * @param ctx the parse tree
   */
  void exitDocument( @NotNull XMLParser.DocumentContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#chardata}.
   *
   * @param ctx the parse tree
   */
  void enterChardata( @NotNull XMLParser.ChardataContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#chardata}.
   *
   * @param ctx the parse tree
   */
  void exitChardata( @NotNull XMLParser.ChardataContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#attribute}.
   *
   * @param ctx the parse tree
   */
  void enterAttribute( @NotNull XMLParser.AttributeContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#attribute}.
   *
   * @param ctx the parse tree
   */
  void exitAttribute( @NotNull XMLParser.AttributeContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#prolog}.
   *
   * @param ctx the parse tree
   */
  void enterProlog( @NotNull XMLParser.PrologContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#prolog}.
   *
   * @param ctx the parse tree
   */
  void exitProlog( @NotNull XMLParser.PrologContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#content}.
   *
   * @param ctx the parse tree
   */
  void enterContent( @NotNull XMLParser.ContentContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#content}.
   *
   * @param ctx the parse tree
   */
  void exitContent( @NotNull XMLParser.ContentContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#element}.
   *
   * @param ctx the parse tree
   */
  void enterElement( @NotNull XMLParser.ElementContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#element}.
   *
   * @param ctx the parse tree
   */
  void exitElement( @NotNull XMLParser.ElementContext ctx );

  /**
   * Enter a parse tree produced by {@link XMLParser#misc}.
   *
   * @param ctx the parse tree
   */
  void enterMisc( @NotNull XMLParser.MiscContext ctx );

  /**
   * Exit a parse tree produced by {@link XMLParser#misc}.
   *
   * @param ctx the parse tree
   */
  void exitMisc( @NotNull XMLParser.MiscContext ctx );
}
