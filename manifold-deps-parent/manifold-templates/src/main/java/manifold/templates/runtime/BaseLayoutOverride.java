package manifold.templates.runtime;

public class BaseLayoutOverride {

    private ILayout _override;

    public BaseLayoutOverride(ILayout override) {
        _override = override;
    }

    public ILayout getOverride(){
        return _override;
    }

}
