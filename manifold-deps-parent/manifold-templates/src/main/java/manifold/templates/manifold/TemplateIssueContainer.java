package manifold.templates.manifold;

import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;

import java.util.Collections;
import java.util.List;

public class TemplateIssueContainer implements IIssueContainer {
    private final List<TemplateIssue> _issues;

    public TemplateIssueContainer() {
        _issues = Collections.emptyList();
    }

    public TemplateIssueContainer(List<TemplateIssue> issues) {
        _issues = issues;
    }

    @Override
    public List<IIssue> getIssues() {
        return (List) _issues;
    }

    @Override
    public List<IIssue> getWarnings() {
        return Collections.emptyList();
    }

    @Override
    public List<IIssue> getErrors() {
        return getIssues();
    }

    @Override
    public boolean isEmpty() {
        return _issues.isEmpty();
    }
}
