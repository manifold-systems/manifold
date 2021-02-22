/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.props;

import manifold.api.util.DynamicArray;
import manifold.api.util.IssueMsg;

public class PropIssueMsg
{
  public static final IssueMsg MSG_CANNOT_ASSIGN_READONLY_PROPERTY = new IssueMsg( "Cannot assign read-only property '{0}'" );
  public static final IssueMsg MSG_CANNOT_ACCESS_WRITEONLY_PROPERTY = new IssueMsg( "Cannot access write-only property '{0}'" );
  public static final IssueMsg MSG_PROPERTY_IS_ABSTRACT = new IssueMsg( "Cannot reference property '{0}' in default interface accessor" );
  public static final IssueMsg MSG_SETTER_TYPE_CONFLICT = new IssueMsg( "Setter has parameter of type '{0}' but property '{1}' is of type '{2}'" );
  public static final IssueMsg MSG_PROPERTY_METHOD_CONFLICT = new IssueMsg( "Property accessor conflict: Property '{0}' method '{1}': '{2}'" );
  public static final IssueMsg MSG_FINAL_NOT_ALLOWED_ON_ABSTRACT = new IssueMsg( "'Final' accessors not allowed on abstract property" );
  public static final IssueMsg MSG_FINAL_NOT_ALLOWED_ON_STATIC = new IssueMsg( "'Final' accessors not allowed on static property" );
  public static final IssueMsg MSG_SETTER_DEFINED_FOR_READONLY = new IssueMsg( "Setter method '{0}' defined for read-only property '{1}'" );
  public static final IssueMsg MSG_GETTER_DEFINED_FOR_WRITEONLY = new IssueMsg( "Getter method '{0}' defined for write-only property '{1}'" );
  public static final IssueMsg MSG_PROPERTY_NOT_ACCESSIBLE = new IssueMsg( "{0} access to property '{1}' is '{2}'" );
  public static final IssueMsg MSG_ACCESSOR_WEAKER = new IssueMsg( "'{0}' attempting to assign weaker access privileges; was '{1}'" );
  public static final IssueMsg MSG_STATIC_MISMATCH = new IssueMsg( "Static method '{0}' conflicts with non-static property '{1}'" );
  public static final IssueMsg MSG_NONSTATIC_MISMATCH = new IssueMsg( "Non-static method '{0}' conflicts with static property '{1}'" );
  public static final IssueMsg MSG_SET_WITH_FINAL = new IssueMsg( "Cannot use @set with final property" );
  public static final IssueMsg MSG_MISSING_INTERFACE_STATIC_PROPERTY_ACCESSOR = new IssueMsg( "Interface '{0}' must provide method '{1}' for static non-final property '{2}'" );
  public static final IssueMsg MSG_INTERFACE_FIELD_BACKED_PROPERTY_NOT_SUPPORTED = new IssueMsg( "Static interface properties must provide user-defined accessor[s] which do not reference the property field" );
  public static final IssueMsg MSG_DOES_NOT_OVERRIDE_ANYTHING = new IssueMsg( "Property '{0}' does not override anything" );
  public static final IssueMsg MSG_MISSING_OVERRIDE = new IssueMsg( "Property '{0}' should be annotated with '@override'" );
  public static final IssueMsg MSG_READONLY_CANNOT_OVERRIDE_WRITABLE = new IssueMsg( "Read-only property '{0}' cannot override writable property" );
  public static final IssueMsg MSG_WRITEONLY_CANNOT_OVERRIDE_READABLE = new IssueMsg( "Write-only property '{0}' cannot override readable property" );
  public static final IssueMsg MSG_WRITABLE_ABSTRACT_PROPERTY_CANNOT_HAVE_INITIALIZER = new IssueMsg( "Writable abstract property '{0}' cannot have an initializer" );
  public static final IssueMsg MSG_ABSTRACT_PROPERTY_IN_NONABSTRACT_CLASS = new IssueMsg( "Abstract property in non-abstract class" );
}