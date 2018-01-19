package mr;

import manifold.api.fs.cache.PathCache;
import manifold.internal.host.ManifoldHost;
import manifold.templates.ManifoldTemplates;
import manifold.templates.runtime.ILayout;
import manifold.util.StreamUtil;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.lesscss.LessCompiler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class Hyde {

  static Parser MD_PARSER = Parser.builder().build();
  static HtmlRenderer MD_RENDERER = HtmlRenderer.builder().escapeHtml(false).build();

  public static void main(String[] args) throws Exception {

    log("Generating Site...");
    File outputDir = makeOutputDir();
    File wwwDir = fileFromResourcePath("/www");

    // Set default layout
    ManifoldTemplates.setDefaultLayout(www.layout.main.asLayout());

    //==========================================================================================
    //  Generate Templates
    //==========================================================================================
    PathCache pathCache = ManifoldHost.getCurrentModule().getPathCache();
    log("Generating Templates...");
    for (String fqn : pathCache.getExtensionCache("mtf").getFqns()) {
      // ignore layouts and non-www stuff
      if (fqn.contains(".layout.")) {
        continue;
      }

      if (fqn.endsWith("_html")) {
        File f = new File(outputDir, getNameFromFile(fqn));
        writeTo(f, renderTemplate(fqn, "_html"));
      }

      //TODO better MD processing (detect header/footer?)
      if (fqn.endsWith("_md")) {
        File f = new File(outputDir, getNameFromFile(fqn));
        writeTo(f, mdToHtml(renderTemplate(fqn, "_md")));
      }
    }

    //==========================================================================================
    //  External project docs non-template Resources
    //==========================================================================================
    log("Generating External Docs...");
    renderExternalMarkdownTo(wwwDir, "manifold-templates.html", "manifold-deps-parent/manifold-templates/README.md");
    renderExternalMarkdownTo(wwwDir, "manifold-js.html", "manifold-deps-parent/manifold-js/README.md");

    //==========================================================================================
    //  Copy non-template Resources
    //==========================================================================================
    log("Copying Resources...");
    copyDirInto(wwwDir, outputDir, file -> !file.getName().endsWith(".mtf") && !file.getName().endsWith(".less"));

    log("Generating CSS from LESS...");
    generateCSS(fileFromResourcePath("/www/css/site.less"), new File(outputDir, "css/site.css"));

    log("Done!");
  }

  private static void renderExternalMarkdownTo(File wwwDir, String targetFileName, String inputFile) throws IOException {
    File output = new File(wwwDir, targetFileName);
    ILayout layout = www.layout.main.asLayout();
    StringBuffer content = new StringBuffer();
    layout.header(content);
    content.append(mdToHtml(new String(Files.readAllBytes(new File(inputFile).toPath()))));
    layout.footer(content);
    Files.write(output.toPath(), content.toString().getBytes());
  }

  private static void generateCSS(File lessFile, File outputFile) throws Exception {
    new LessCompiler().compile(lessFile, outputFile);
  }

  private static void log(String s) {
    System.out.println("Mr. Hyde says: " + s);
  }

  private static File fileFromResourcePath(String name) throws URISyntaxException {
    return new File(Hyde.class.getResource(name).toURI());
  }

  private static void copyDirInto(File in, File out, Predicate<File> filter) {
    for (File file : in.listFiles()) {
      StreamUtil.copy(file, out, filter);
    }
  }

  private static String mdToHtml(String markdown) {
    Node doc = MD_PARSER.parse(markdown);
    return MD_RENDERER.render(doc);
  }

  private static void writeTo(File f, String html) throws IOException {
    Files.write(Paths.get(f.getPath()), html.getBytes(StandardCharsets.UTF_8));
  }

  private static String renderTemplate(String fqn, String ext) throws Exception {
    CharSequence className = fqn.subSequence(0, fqn.length() - ext.length());
    Class<?> aClass = Class.forName(className.toString());
    return (String) aClass.getMethod("render").invoke(null);
  }

  private static String getNameFromFile(String fqn) {
    int endIndex = fqn.lastIndexOf("_");
    String stripPackage = fqn.substring(4, endIndex);
    String noDots = stripPackage.replace(".", "" + File.separatorChar);
    return noDots + ".html";
  }

  private static File makeOutputDir() {
    File site = new File("manifold-docs/www");
    site.mkdirs();
    return site;
  }

  // HTML utils
  public static class BaseTemplate extends manifold.templates.runtime.BaseTemplate {

    public String anchor(String content) {
      return "<a class=\"toc_anchor\" name=\"" + anchorId(content) + "\">&nbsp;</a>" + content;
    }

    public String linkTo(String name) {
      return "<a href=\"#" + anchorId(name) + "\">" + name + "</a>";
    }

    private String anchorId(String content) {
      String strValue = content.replaceAll("[^A-Za-z0-9 ]", "").trim().toLowerCase();
      return strValue.replace(" ", "_");
    }

  }

}
