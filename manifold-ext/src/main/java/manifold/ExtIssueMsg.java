package manifold;

import manifold.util.IssueMsg;

/**
 */
public class ExtIssueMsg
{
  public static final IssueMsg MSG_THIS_FIRST = new IssueMsg( "@This must target only the first parameter of an extension method" );
  public static final IssueMsg MSG_EXPECTING_TYPE_FOR_THIS = new IssueMsg( "Expecting type '{0}' for @This parameter" );
  public static final IssueMsg MSG_MAYBE_MISSING_THIS = new IssueMsg( "Maybe missing @This to declare an instance extension method?" );
  public static final IssueMsg MSG_MUST_BE_STATIC = new IssueMsg( "Extension method {0} must be declared 'static'" );
  public static final IssueMsg MSG_MUST_NOT_BE_PRIVATE = new IssueMsg( "Extension method {0} must not be declared 'private'" );
  public static final IssueMsg MSG_EXTENSION_DUPLICATION = new IssueMsg( "Illegal extension method. {0} from {1} duplicates another extension method from {2}" );
  public static final IssueMsg MSG_EXTENSION_SHADOWS = new IssueMsg( "Illegal extension method. {0} from {1} duplicates a method in the extended class {2}" );
}
