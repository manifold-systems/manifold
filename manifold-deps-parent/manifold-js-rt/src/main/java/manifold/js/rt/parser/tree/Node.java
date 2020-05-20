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

package manifold.js.rt.parser.tree;

import manifold.js.rt.parser.Token;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Node
{
  private String _name;
  private Token _start;
  private Token _end;
  private List<Node> _children;
  private Node _parent;

  public Node( String name)
  {
    _name = name;
    _children = new ArrayList<>();
  }

  public Node getParent()
  {
    return _parent;
  }

  public ProgramNode getProgramNode()
  {
    return _parent.getProgramNode();
  }

  public List<Node> getChildren() {
    return _children;
  }

  public <T> List<T> getChildren(Class<T> clazz) {
  LinkedList<T> lst = new LinkedList<>();
  for( Node child : _children )
  {
    if(child.getClass().equals(clazz))
    {
      lst.add((T) child);
    }
  }
  return lst;
}

  public <T extends Node> T getFirstChild (Class<T> clazz) {
    for( Node child : _children )
    {
      if(child.getClass().equals(clazz))
      {
        return (T) child;
      }
    }
    return null;
  }

  public void addChild(Node n) {
    _children.add( n );
    n._parent = this;
  }

  public Node withChild(Node n) {
    _children.add( n );
    n._parent = this;
    return this;
  }

  public String getName()
  {
    return _name;
  }

  public void setTokens( Token start, Token end )
  {
    _start = start;
    _end = end;
  }

  public Token getStart()
  {
    return _start;
  }

  public Token getEnd()
  {
    return _end;
  }

  /* Generates ES5 code */
  public String genCode() {
    StringBuilder childCode = new StringBuilder();
    for (Node node : this.getChildren()) {
      childCode.append(node.genCode());
    }
    return childCode.toString();
  }


  @Override
  public String toString() {
    return getName();
  }

}
