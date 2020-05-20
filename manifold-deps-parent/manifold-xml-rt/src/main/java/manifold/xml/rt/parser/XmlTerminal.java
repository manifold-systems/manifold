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

import org.antlr.v4.runtime.Token;

public class XmlTerminal extends XmlPart
{
  private final String _text;

  XmlTerminal( Token t, XmlPart parent )
  {
    super( parent, t.getStartIndex(), t.getStopIndex() - t.getStartIndex() + 1, t.getLine() );
    _text = t.getText();
  }

  public String getRawText()
  {
    return _text;
  }
}
