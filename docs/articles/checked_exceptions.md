# Java's Checked Exception Dilemma

We've had a couple of decades now to form a collective opinion on the utility of checked exceptions. It has become one of
those highly charged, _us vs. them_ issues. From my own experience I have to admit checked exceptions have been more of
a hassle than not, mostly because I think they are misunderstood and, therefore, overused. According to the internet, I
fall on the heavier, more vocal side of the ledger disapproving checked exceptions.

One has to admit, though, it is pretty clear why so many developers have formed an unfavorable opinion.
The negative consequences of checked exceptions are rather serious and range from reduced readability to unintentional
exception swallowing, which is notorious as a root cause of extremely critical bugs. Lambda expressions add yet another
dimension to the problem with awkward hacks to escape forced exception handling. So although they address a legitimate
use-case, I'm not so sure it remains compelling enough to warrant a feature that can be abused so readily and that commonly
results in negative, potentially critical, consequences. 

# A Way Out

Perhaps the Java language designers should consider addressing the mounting pressure to "fix" this? It's a tough spot,
though, due to the mountain of existing tooling and tests that rely on the current behavior. So how about this:  Since checked
exceptions are enforced exclusively by the compiler, what if it were to offer _levels_ of checked exception enforcement
reflecting the preferences of project designers? 
```
 javac -ce:[error,warning,none] ...
```
`-ce:error`: the default setting, enforces checked exceptions with compiler errors as Java currently behaves.

`-ce:warning`: enforces checked exceptions with compiler warnings, allowing code to circumvent the try/catch ceremony,
but with a configurable warning indicating a potentially unhandled exception. 

`-ce:none`: the compiler does not distinguish between checked and unchecked exceptions, behaves like most other
languages as well as the [Manifold compiler plugin](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions).

Enforcement levels have the potential to nullify the checked exception debate by allowing project designers to choose
the more suitable option according to their needs and standards. As such build files specify the checked exception  
treatment used by all project contributors.
 
# Concerns

One downside involves the case where project designers later wish to switch from `ce:warning` or
`ce:none` to `ce:error`. If the codebase contains a lot of existing checked exception circumvention, those will have to
be converted to explicit try/catch/wrap/rethrow boilerplate, a potentially error-prone process. But considering modern
Java IDEs, such as IntelliJ IDEA, already support "quick fixes" that automatically generate the code for you, perhaps
this is not a valid concern? Indeed, it can go the other way as well -- quick fixes can _remove_ boilerplate code when
switching to `ce:warning` or `ce:none`.

Perhaps another downside is the option itself. In the best of worlds Java would treat checked exceptions in
one way; choice adds complexity. Whether or not the complexity is justified depends the weight of the benefits
it provides. In this particular case my view is that the benefits offer a pretty good trade-off.      

# Upshot

We've had twenty-five years to vet the Java language experiment that is checked exceptions. The verdict is still
out, however there is mounting opposition that deserves attention from the Java language designers. Here I offer a
solution that is compatible with Java's existing behavior and provides both sides of the debate with options to best
suit their needs. What are your thoughts?