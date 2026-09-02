package manifold.ext.parts.parts.multirooted;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class MultiEntryPointCompositesExample extends TestCase
{
  public void testMultiEntryPointComposites() {
    BasicHero basicHero = new BasicHero();
    PaidSubscriber subscriber = new PaidSubscriber(basicHero);
    SuperSoldier superSoldier = new SuperSoldier(basicHero);

    String result = subscriber.takeAction(); // reaches SuperSoldier's strengthLevel()
    assertEquals( "Attack with strength: 80", result );

    basicHero.elapsedTime = 101;
    int strengthLevel = superSoldier.strengthLevel(); // reaches PaidSubscriber's timeLimit()
    assertEquals( 0, strengthLevel );
  }

  interface Actor {
    String takeAction();
    int timeLimit();
  }
  interface Combatant {
    int strengthLevel();
  }

  @part class BasicHero implements Actor, Combatant {
    int elapsedTime = 11;
    BasicHero() {}
    public String takeAction() { return "Attack with strength: " + strengthLevel(); }
    public int timeLimit() { return 10; }
    public int strengthLevel() { return outOfTime() ? 0 : 8; }
    private boolean outOfTime() { return elapsedTime > timeLimit(); }
  }

  class PaidSubscriber implements Actor {
    @link Actor actor;
    PaidSubscriber(Actor actor) { this.actor = actor; }

    public int timeLimit() { return 100; }
  }

  class SuperSoldier implements Combatant {
    @link Combatant combatant;
    SuperSoldier(Combatant combatant) { this.combatant = combatant; }

    public int strengthLevel() { return combatant.strengthLevel() * 10; }
  }
}
