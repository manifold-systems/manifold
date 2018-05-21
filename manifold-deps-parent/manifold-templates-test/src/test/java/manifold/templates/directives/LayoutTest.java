package manifold.templates.directives;

import manifold.templates.ManifoldTemplates;
import manifold.templates.runtime.ILayout;
import org.junit.Test;
import directives.layouts.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by hkalidhindi on 7/17/2017.
 */
public class LayoutTest {

    @Test
    public void layoutTestWithoutContent() {
        assertEquals("HeaderFooter", HasLayout.render());
    }

    @Test
    public void layoutTestWithContent() {
        assertEquals("HeaderContentFooter", HasLayoutAndContent1.render());
        assertEquals("HeaderContentFooter", HasLayoutAndContent2.render());
    }

    @Test
    public void withoutLayoutTestWithContent() {
        assertEquals("Content", HasLayoutAndContent1.withoutLayout().render());
    }

    @Test
    public void overrideLayoutTest() {
        assertEquals("HeaderPlainFooter", PlainFile.withLayout(IsLayout.asLayout()).render());
    }

    @Test
    public void NestedLayoutTestWithContent() {
        assertEquals("HeaderH2ContentF2Footer", HasNestedLayout.render());
    }

    @Test
    public void PlainDefaultLayoutTest() {
        ILayout lo = IsLayout.asLayout();
        ManifoldTemplates.setDefaultLayout(lo);
        assertEquals("HeaderPlainFooter", PlainFile.render());
        ManifoldTemplates.setDefaultLayout("directives", IsLayout3.asLayout());
        assertEquals("3Plain4", PlainFile.render());
        ManifoldTemplates.resetDefaultLayout();
    }

    @Test
    public void LayoutPrecedenceTest() {
        assertEquals("HeaderH2ContentF2Footer", HasNestedLayout.render());
    }
}
