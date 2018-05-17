package manifold.templates.runtime;

import manifold.templates.ManifoldTemplates;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;

public class BaseTemplate {

    private static Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException|IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
    private ILayout _explicitLayout = null;

    public String toS(Object o) {
        return o == null ? "" : o.toString();
    }

    protected void setLayout(ILayout layout) {
        _explicitLayout = layout;
    }

    protected ILayout getTemplateLayout() {
        if (_explicitLayout != null) {
            return _explicitLayout;
        } else {
            return ManifoldTemplates.getDefaultLayout(this.getClass().getName());
        }
    }

    protected ILayout getExplicitLayout() {
        return _explicitLayout;
    }

    protected void beforeRender(Appendable buffer, ILayout override, boolean topLevelTemplate) throws IOException {
        if (topLevelTemplate) {
            ILayout templateLayout = override == null ? getTemplateLayout() : override;
            templateLayout.header(buffer);
        }
    }

    protected void afterRender(Appendable buffer, ILayout override, boolean topLevelTemplate, long renderTime) throws IOException {
        if (topLevelTemplate) {
            ILayout templateLayout = override == null ? getTemplateLayout() : override;
            templateLayout.footer(buffer);
        }
        ManifoldTemplates.getTracer().trace(this.getClass(), renderTime);
    }

    protected void handleException(Exception e, String fileName, int lineStart, int[] bbLineNumbers) {
        if (e.getClass().equals(TemplateRuntimeException.class)) {
            unsafe.throwException(e);
        }
        StackTraceElement[] currentStack = e.getStackTrace();
        String templateClassName = getClass().getName();

        int elementToRemove = 0;
        while (elementToRemove < currentStack.length) {
            StackTraceElement curr = currentStack[elementToRemove];
            if (curr.getClassName().equals(templateClassName)) {
                if (curr.getMethodName().equals("renderImpl")) {
                    handleTemplateException(e, fileName, lineStart, bbLineNumbers, elementToRemove);
                } else if (curr.getMethodName().equals("footer") || curr.getMethodName().equals("header")) {
                    handleLayoutException(e, fileName, lineStart, bbLineNumbers, elementToRemove);
                }
            }
            elementToRemove++;
        }
    }

    private void handleTemplateException(Exception e, String fileName, int lineStart, int[] bbLineNumbers, int elementToRemove) {
        StackTraceElement[] currentStack = e.getStackTrace();
        int lineNumber = currentStack[elementToRemove].getLineNumber();
        int javaLineNum = lineNumber - lineStart;

        String declaringClass = currentStack[elementToRemove + 1].getClassName();
        String methodName = currentStack[elementToRemove + 1].getMethodName();

        StackTraceElement b = new StackTraceElement(declaringClass, methodName, fileName, bbLineNumbers[javaLineNum]);
        currentStack[elementToRemove + 1] = b;

        System.arraycopy(currentStack, elementToRemove + 1, currentStack, elementToRemove, currentStack.length-1-elementToRemove);
        throwBBException(e, currentStack);
    }

    private void handleLayoutException(Exception e, String fileName, int lineStart, int[] bbLineNumbers, int elementToReplace) {
        StackTraceElement[] currentStack = e.getStackTrace();
        int lineNumber = currentStack[elementToReplace].getLineNumber();
        int javaLineNum = lineNumber - lineStart;

        String declaringClass = currentStack[elementToReplace].getClassName();
        String methodName = currentStack[elementToReplace].getMethodName();

        StackTraceElement b = new StackTraceElement(declaringClass, methodName, fileName, bbLineNumbers[javaLineNum]);
        currentStack[elementToReplace] = b;

        throwBBException(e, currentStack);
    }

    private void throwBBException(Exception e, StackTraceElement[] currentStack) {
        e.setStackTrace(currentStack);
        TemplateRuntimeException exceptionToThrow = new TemplateRuntimeException(e);
        exceptionToThrow.setStackTrace(currentStack);
        unsafe.throwException(exceptionToThrow);
    }


}
