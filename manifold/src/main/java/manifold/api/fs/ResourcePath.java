/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import manifold.util.DynamicArray;
import manifold.util.GosuObjectUtil;
import manifold.util.GosuStringUtil;

public class ResourcePath {

  public static final String WINDOWS_NETWORK_ROOT = "\\\\";

  protected final ResourcePath _parent;
  protected final String _name;

  protected ResourcePath(ResourcePath parent, String name) {
    if (name == null) {
      throw new IllegalArgumentException("The name argument to the ResourcePath constructor cannot be null");
    }
    _parent = parent;
    _name = name;
  }

  public static ResourcePath parse(String pathString) {
    String rootElement;
    int lastIndex;
    if (pathString.startsWith(WINDOWS_NETWORK_ROOT)) {
      rootElement = WINDOWS_NETWORK_ROOT;
      lastIndex = 2;
    } else if (pathString.startsWith("/")) {
      rootElement = "";
      lastIndex = 1;
    } else {
      char first = pathString.charAt(0);
      if (pathString.length() > 1 && pathString.charAt(1) == ':' && Character.isLetter(first)) {
        rootElement = Character.toUpperCase(first) + ":";
        lastIndex = 2;
        if (pathString.length() > 2 && (pathString.charAt(2) == '/' || pathString.charAt(2) == '\\')) {
          lastIndex = 3;
        }
      } else {
        throw new IllegalArgumentException("The path string [" + pathString + "] is not an absolute path starting at a drive root");
      }
    }

    DynamicArray<String> results = tokenizePathFragment( pathString, lastIndex);

    results = normalizePath(results);

    return construct(rootElement, results, results.size() - 1);
  }

  private static DynamicArray<String> tokenizePathFragment(String pathString, int lastIndex) {
    DynamicArray<String> results = new DynamicArray<String>();
    for (int i = lastIndex; i < pathString.length(); i++) {
      char c = pathString.charAt(i);
      if (c == '/' || c == '\\') {
        results.add(pathString.substring(lastIndex, i));
        lastIndex = i + 1;
      }
    }

    pathString = pathString.substring(lastIndex);
    results.add(pathString);
    return results;
  }

  private static DynamicArray<String> normalizePath(DynamicArray<String> pathElements) {
    DynamicArray<String> results = new DynamicArray<String>(pathElements.size());
    for (int i = 0; i < pathElements.size; i++) {
      String s = (String) pathElements.data[i];
      if (s.equals(".") || s.equals("/") || s.equals("\\") || s.equals("")) {
        // no-op
      } else if (s.equals("..")) {
        // TODO - Throw if no more elements
        results.remove(results.size() - 1);
      } else {
        results.add(s);
      }
    }

    return results;
  }

  private static ResourcePath construct(String headElement, List<String> fullPath, int nameIndex) {
    if (nameIndex == -1) {
      if (headElement == null) {
        return null;
      } else {
        return new ResourcePathHead(headElement);
      }
    } else {
      return new ResourcePath(construct(headElement, fullPath, nameIndex - 1), fullPath.get(nameIndex));
    }
  }

  /**
   * Returns the leaf name of this resource path.  If this path object represents the path "/usr/local/bin", then
   * the getName() method will return the String "bin".  In the case of the root path element, this method will return
   * the empty string for the unix filesystem root and a windows drive letter, normalized to upper case, with no
   * trailing path separator for a windows path root.
   *
   * This method will never return a null value.
   *
   * @return the name of the last element of the path.
   */
  public String getName() {
    return _name;
  }

  /**
   * Returns the parent of this resource path.  If this path object represents the root of the filesystem,
   * this method will return null.
   *
   * @return the parent of this ResourcePath
   */
  public ResourcePath getParent() {
    return _parent;
  }

  /**
   * Returns the path string for this path using the / path separator.
   *
   * @return the path string for this path using the / path separator.
   * @see #getPathString(String)
   */
  public String getPathString() {
    return getPathString("/");
  }

  /**
   * Returns the path string for this path using the default file system separator,
   * as defined by the File.separator property.
   *
   * @return the path string for this path using the default file system separator
   * @see #getPathString(String)
   */
  public String getFileSystemPathString() {
    return getPathString(File.separator);
  }

  /**
   * Returns the path string for this path using the specified path separator.  The path
   * constructed will begin with the root of the path, which will be one of:
   * <ul>
   *   <li>A windows drive letter, normalized to upper case, followed by : and the separator</li>
   *   <li>The windows network path start \\</li>
   *   <li>Just the separator, in the case of the unix root</li>
   * </ul>
   * After the path root will follow all path components, separated with the given separator.  The
   * separator will not be appended to the end of the path.  Some example paths returned by this
   * method are:
   * <ul>
   *   <li>C:\temp\downloads</li>
   *   <li>/</li>
   *   <li>\\files\documents</li>
   * <ul>
   *
   * @param separator the separator to use when constructing the path
   * @return the path string for this path using the specified path separator
   */
  public String getPathString(String separator) {
    StringBuilder sb = new StringBuilder();
    constructPathString(sb, separator);
    return sb.toString();
  }

  private void constructPathString(StringBuilder sb, String separator) {
    if (_parent != null) {
      _parent.constructPathString(sb, separator);
      // We don't want to add the separator character after the windows network root; otherwise
      // we want to add the separator in before adding in our name
      if (!(_parent instanceof ResourcePathHead && _parent.getName().equals(WINDOWS_NETWORK_ROOT))) {
        sb.append(separator);
      }
    }
    sb.append(_name);
  }

  /**
   * Takes the specified path fragment and joins it with this ResourcePath to create a new ResourcePath.
   * The specified path can use either \ or / separator characters (or a mix of the two), and it can include
   * . or .. elements in the path, which will be traversed appropriately.  The path can start and/or end with
   * a separator character, but it should not start with a windows drive letter or network root.  The resulting
   * path will have the same root as this path.  Some examples:
   * <ul>
   *   <li>"/usr/local".join("lib/java") -> "/usr/local/lib/java"</li>
   *   <li>"/usr/local".join("/..") -> "/usr"</li>
   *   <li>"/usr/local".join("/.") -> "/usr/local</li>
   * </ul>
   * If .. path elements would lead to traversing backwards past the root element, an IllegalArgumentException will
   * be thrown.  An IllegalArgumentException will also be thrown if otherPath is null.
   *
   * @param otherPath the path to join with this one
   * @return a ResourcePath that results from appending otherPath to this path and then normalizing the result
   */
  public ResourcePath join(String otherPath) {
    if (otherPath == null) {
      throw new IllegalArgumentException("The join(String) method cannot be called with a null argument");
    }

    DynamicArray<String> components = tokenizePathFragment(otherPath, 0);
    ResourcePath result = this;
    for (int i = 0; i < components.size; i++) {
      String s = (String) components.data[i];
      if (s.equals(".") || s.equals("/") || s.equals("\\") || s.equals("")) {
        // no-op
      } else if (s.equals("..")) {
        result = result.getParent();
        if (result == null) {
          throw new IllegalArgumentException("Joining the path [" + otherPath + "] to the base path [" + getPathString() + "] resulted in traversing backwards past the root path element");
        }
      } else {
        result = new ResourcePath(result, s);
      }
    }

    return result;
  }

  /**
   * Two ResourcePath objects are considered to be equal if they represent the same leaf path and if their parents
   * are equal.  Note that currently the name matching is case-sensitive, even when this is being called on
   * a case-insensitive file system.
   *
   * @param obj the other object
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ResourcePath) {
      ResourcePath otherPath = (ResourcePath) obj;
      return otherPath.getName().equals(getName()) && GosuObjectUtil.equals( getParent(), otherPath.getParent());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = _parent != null ? _parent.hashCode() : 0;
    result = 31 * result + (_name != null ? _name.hashCode() : 0);
    return result;
  }

  public boolean isChild(ResourcePath path) {
    return GosuObjectUtil.equals(this, path.getParent());
  }

  public boolean isDescendant(ResourcePath path) {
    ResourcePath pathToTest = path;
    while (pathToTest != null) {
      if (pathToTest.equals(this)) {
        return true;
      }
      pathToTest = pathToTest.getParent();
    }

    return false;
  }

  public String relativePath(ResourcePath other) {
    return relativePath(other, File.separator);
  }

  public String relativePath(ResourcePath other, String separator) {
    List<String> pathComponents = new ArrayList<String>();
    ResourcePath pathToTest = other;
    boolean success = false;
    while (pathToTest != null) {
      if (pathToTest.equals(this)) {
        success = true;
        break;
      }
      pathComponents.add(0, pathToTest.getName());
      pathToTest = pathToTest.getParent();
    }

    if (!success || pathComponents.isEmpty()) {
      return null;
    } else {
      return GosuStringUtil.join( pathComponents, separator);
    }
  }

  private static class ResourcePathHead extends ResourcePath {
    protected ResourcePathHead(String name) {
      super(null, name);
    }

    @Override
    public String getPathString(String separator) {
      // Special hack:  the path string for the root element needs to
      // include the separator unless it's the windows network root symbol
      if (WINDOWS_NETWORK_ROOT.equals(_name)) {
        return _name;
      } else {
        return _name + separator;
      }
    }
  }
}
