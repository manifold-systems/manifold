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
  public static final IssueMsg MSG_FINAL_NOT_ALLOWED_IN_INTERFACE = new IssueMsg( "'Final' property not allowed in interface" );
  public static final IssueMsg MSG_SETTER_DEFINED_FOR_READONLY = new IssueMsg( "Setter method '{0}' defined for read-only property '{1}'" );
  public static final IssueMsg MSG_GETTER_DEFINED_FOR_WRITEONLY = new IssueMsg( "Getter method '{0}' defined for write-only property '{1}'" );
  public static final IssueMsg MSG_SETTER_DEFINED_FOR_FINAL_PROPERTY = new IssueMsg( "Setter method '{0}' defined for final property '{1}'" );
  public static final IssueMsg MSG_PROPERTY_NOT_ACCESSIBLE = new IssueMsg( "{0} access to property '{1}' is '{2}'" );
  public static final IssueMsg MSG_ACCESSOR_WEAKER = new IssueMsg( "'{0}' attempting to assign weaker access privileges; was '{1}'" );
  public static final IssueMsg MSG_STATIC_MISMATCH = new IssueMsg( "Static method '{0}' conflicts with non-static property '{1}'" );
  public static final IssueMsg MSG_NONSTATIC_MISMATCH = new IssueMsg( "Non-static method '{0}' conflicts with static property '{1}'" );
}