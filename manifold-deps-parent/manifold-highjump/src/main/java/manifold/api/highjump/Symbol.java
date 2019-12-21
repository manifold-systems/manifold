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

package manifold.api.highjump;

/**
 * Use {@code Symbol} to manage external symbols that can be directly referenced in dynamic scripts and expressions.
 */
public class Symbol
{
  private final String _uid;
  private final String _name;
  private final String _type;
  private final boolean _readOnly;
  private final Object _initialValue;
  private volatile Object _value;

  /**
   * Create a {@code Symbol} that can be directly referenced in a dynamic expression or script. Use with
   * {@link Highjump#evaluate(Options)}.
   *
   * @param uid Qualifies the {@code name} to uniquely identify this symbol. {@code uid} must strictly follow Java
   *            identifier naming rules (alphanumeric and underscore chars). Can be the empty string if no further
   *            qualification is necessary.
   * @param name The name of the symbol that will be directly referenced in the script or expression. {@code name} must
   *             strictly follow Java identifier naming rules (alphanumeric and underscore chars).
   * @param type The fully qualified name of the symbol's type, such as: {@code java.lang.String},
   *             {@code java.math.BigDecimal[]}, and {@code java.util.List<java.lang.String>}. Note the type must be
   *             non-primitive.
   * @param readOnly If true, indicates the symbol is read-only and the {@code initialValue} is constant.
   * @param initialValue The initial value of the symbol, may be {@code null).
   */
  public Symbol( String uid, String name, String type, boolean readOnly, Object initialValue )
  {
    _uid = uid;
    _name = name;
    _type = type;
    _readOnly = readOnly;
    _initialValue = initialValue;
  }

  /**
   * Same as: {@code new Symbol(uid, name, type, false, null)}
   * <p/>
   * {@link #Symbol(String, String, String, boolean, Object)}
   */
  public Symbol( String uid, String name, String type )
  {
    this( uid, name, type, false, null );
  }

  public String getUid()
  {
    return _uid;
  }

  public String getName()
  {
    return _name;
  }

  public String getType()
  {
    return _type;
  }

  public boolean isReadOnly()
  {
    return _readOnly;
  }

  public Object getInitialValue()
  {
    return _initialValue;
  }
  public Object getValue()
  {
    return _value;
  }
  public void setValue( Object value )
  {
    _value = value;
  }
}
