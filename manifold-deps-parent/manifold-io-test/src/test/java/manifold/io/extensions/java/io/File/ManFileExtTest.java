package manifold.io.extensions.java.io.File;

import manifold.io.FileTreeWalk;
import manifold.rt.api.util.Stack;
import manifold.test.api.ExtensionManifoldTest;

import java.io.File;
import java.io.IOException;

/**
 */
public class ManFileExtTest extends ExtensionManifoldTest {
  public void testCoverage() {
//    testCoverage(ManFileExt.class);
  }

  public void testCopyRecursively() throws IOException {
    File tree = createTempFileTree(
      "top\n" +
      "--subDir0\n" +
      "----File0.txt\n" +
      "----File1.txt\n" +
      "--subDir1\n" +
      "----subDir0\n" +
      "------File0.txt\n" +
      "----File0.txt\n" +
      "----File1.txt\n"
    );
    FileTreeWalk walk = (FileTreeWalk) tree.walkTopDown();
    for( File s: walk )
    {
      System.out.println( s );
    }
  }

  private File createTempFileTree(String treeDesc) throws IOException {
    Stack<File> parents = new Stack<>();
    File tempDir = File.createTempDir();
    tempDir.deleteOnExit();
    parents.push(tempDir);
    int iCurrentDepth = -1;
    for (String line : treeDesc.split("\n")) {
      int i = 0;
      while (line.indexOf("--", i) >= 0) {
        i += 2;
      }
      int iDepth = i / 2;
      String fileName = line.substring(i);
      assert !fileName.contains('-') : "Odd number of dashes (-) for line: " + line;
      File child;
      if (iDepth == iCurrentDepth) {
        parents.pop();
        child = new File(parents.peek(), fileName);
      }
      else if (iDepth > iCurrentDepth) {
        assert iDepth - iCurrentDepth == 1 : "too many dashes or line: " + line;
        child = new File(parents.peek(), fileName);
      }
      else {
        parents.pop();
        int iDelta = iCurrentDepth - iDepth;
        assert iDelta >= 0 : "depth < 0";
        while (iDelta-- > 0) {
          parents.pop();
        }
        child = new File(parents.peek(), fileName);
      }
      parents.push(child);
      if (fileName.contains('.')) {
        if (!child.createNewFile()) {
          throw new IllegalStateException("test file should not have existed");
        }
      }
      else {
        if (!child.mkdir()) {
          throw new IllegalStateException("test dir should not have existed");
        }
      }
      iCurrentDepth = iDepth;
    }
    return tempDir;
  }

}