package manifold.ext.params.rt;

// This file exists so that the manifold-params-rt module builds, which is necessary
// to bring in manifold-tuple-rt, which is necessary to call methods with named args / optional params.
// Basically, rather than requiring manifold-rt as a runtime dependency, it's more
// consistent to use manifold-params-rt for runtime when you use manifold-params for compile time,
// as is conventional with manifold modules.
public class dummy_placeholder
{
}
