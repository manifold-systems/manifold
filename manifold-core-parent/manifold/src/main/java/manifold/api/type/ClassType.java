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

package manifold.api.type;

public enum ClassType
{
  Enhancement,
  Program,
  Template,
  Eval,
  Class,
  Interface,
  Structure,
  Annotation,
  Enum,
  JavaClass,
  Unknown;

  public boolean isJava()
  {
    return this == JavaClass;
  }

  public boolean isOther()
  {
    return
      this == Enhancement ||
      this == Program ||
      this == Template ||
      this == Eval ||
      this == Class ||
      this == Interface ||
      this == Structure ||
      this == Annotation ||
      this == Enum;
  }

  public static ClassType getFromFileName( String name )
  {
    if( name.endsWith( ".java" ) )
    {
      return JavaClass;
    }
    if( name.endsWith( ".gsx" ) )
    {
      return Enhancement;
    }
    if( name.endsWith( ".gsp" ) )
    {
      return Program;
    }
    if( name.endsWith( ".gst" ) )
    {
      return Template;
    }
    if( name.endsWith( ".gs" ) || name.endsWith( ".gr" ) || name.endsWith( ".grs" ) )
    {
      return Class;
    }
    return Unknown;
  }

  public String getExt()
  {
    switch( this )
    {
      case Class:
      case Enum:
      case Interface:
      case Structure:
      case Annotation:
        return ".gs";
      case Program:
        return ".gsp";
      case Enhancement:
        return ".gsx";
      case Template:
        return "*.gst";
      case JavaClass:
        return ".java";
      default:
        return "";
    }
  }

  public String keyword()
  {
    switch( this )
    {
      case Enhancement:
        return "enhancement";
      case Interface:
        return "interface";
      case Structure:
        return "structure";
      case Annotation:
        return "annotation";
      case Enum:
        return "enum";
      case Class:
      case Program:
      case Template:
      case Eval:
      case JavaClass:
        return "class";
      default:
        return "<unknown>";
    }
  }

}
