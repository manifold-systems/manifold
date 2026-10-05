package manifold.ext.parts.parts.multirooted;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class OverlappingMultiEntryPointCompositesExample extends TestCase
{
  public void testMultiEntryPointComposites() {
    BasicHero basicHero = new BasicHero();
    PaidSubscriber subscriber = new PaidSubscriber(basicHero);
    SuperSoldier superSoldier = new SuperSoldier(basicHero);

    String result = subscriber.takeAction(); // reaches SuperSoldier's strengthLevel()
    assertEquals( subscriber.name() +
                  " (call sign: " + superSoldier.name() +
                  ") attacks with strength: 80", result );

    basicHero.elapsedTime = 101;
    int strengthLevel = superSoldier.strengthLevel(); // reaches PaidSubscriber's timeLimit()
    assertEquals( 0, strengthLevel );
  }

  public void testMultiEntryPointComposites_Both() {
    BasicHeroBoth basicHero = new BasicHeroBoth();
    PaidSubscriber subscriber = new PaidSubscriber(basicHero);
    SuperSoldier superSoldier = new SuperSoldier(basicHero);

    String result = subscriber.takeAction(); // reaches SuperSoldier's strengthLevel()
    assertEquals( subscriber.name() +
                  " (call sign: " + superSoldier.name() +
                  ") attacks with strength: 80", result );

    basicHero.elapsedTime = 101;
    int strengthLevel = superSoldier.strengthLevel(); // reaches PaidSubscriber's timeLimit()
    assertEquals( 0, strengthLevel );
  }

  interface Actor {
    String takeAction();
    int timeLimit();
    String name();
  }
  interface Combatant {
    int strengthLevel();
    String name();
  }

  @part class BasicHero implements Actor, Combatant {
    int elapsedTime = 11;

    public String takeAction() {
      return ((Actor)this).name() +
             " (call sign: " + ((Combatant)this).name() +
             ") attacks with strength: " + strengthLevel();
    }

    public int timeLimit() { return 10; }
    public int strengthLevel() { return outOfTime() ? 0 : 8; }
    private boolean outOfTime() { return elapsedTime > timeLimit(); }

    @Override
    public String name() { return ((Actor)this).name(); }
  }
  class PaidSubscriber implements Actor {

    @link Actor actor;
    PaidSubscriber(Actor actor) { this.actor = actor; }
    public int timeLimit() { return 100; }

    public String name() { return "PaidSubscriber"; }

  }
  class SuperSoldier implements Combatant {

    @link Combatant combatant;
    SuperSoldier(Combatant combatant) { this.combatant = combatant; }
    public int strengthLevel() { return combatant.strengthLevel() * 10; }

    public String name() { return "SuperSoldier"; }

  }

  interface Both extends Actor, Combatant
  {
  }

  @part class BasicHeroBoth implements Both {
    int elapsedTime = 11;

    public String takeAction() {
      return ((Actor)this).name() +
             " (call sign: " + ((Combatant)this).name() +
             ") attacks with strength: " + strengthLevel();
    }

    public int timeLimit() { return 10; }
    public int strengthLevel() { return outOfTime() ? 0 : 8; }
    private boolean outOfTime() { return elapsedTime > timeLimit(); }

    @Override
    public String name() { return ((Actor)this).name(); }
  }
}
