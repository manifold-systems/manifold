package manifold.js.parser.tree;

import manifold.js.parser.Tokenizer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by carson on 6/15/16.
 */
public class Node
{
  private String _name;
  Tokenizer.Token _start;
  Tokenizer.Token _end;
  List<Node> _children;

  public Node( String name)
  {
    _name = name;
    _children = new ArrayList<>();
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
  }

  public Node withChild(Node n) {
    _children.add( n );
    return this;
  }

  public String getName()
  {
    return _name;
  }

  public void setTokens( Tokenizer.Token start, Tokenizer.Token end )
  {
    _start = start;
    _end = end;
  }

  public Tokenizer.Token getStart()
  {
    return _start;
  }

  public Tokenizer.Token getEnd()
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
