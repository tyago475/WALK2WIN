package pt.ubi.pdm.quizpenaltis.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.ubi.pdm.quizpenaltis.model.Player.Tier;

public class PlayerTierTest {

    private static Player costing(int cost) {
        return new Player("X", cost, 0, 0);
    }

    @Test
    public void cheapPlayersAreBronze() {
        assertEquals(Tier.BRONZE, costing(100).getTier());
        assertEquals(Tier.BRONZE, costing(300).getTier());
    }

    @Test
    public void midPlayersAreSilver() {
        assertEquals(Tier.SILVER, costing(400).getTier());
        assertEquals(Tier.SILVER, costing(600).getTier());
    }

    @Test
    public void expensivePlayersAreGold() {
        assertEquals(Tier.GOLD, costing(700).getTier());
        assertEquals(Tier.GOLD, costing(1000).getTier());
    }

    @Test
    public void catalogCoversAllThreeTiers() {
        boolean bronze = false, silver = false, gold = false;
        for (Player p : PlayerCatalog.ALL) {
            switch (p.getTier()) {
                case BRONZE: bronze = true; break;
                case SILVER: silver = true; break;
                default: gold = true;
            }
        }
        assertEquals(true, bronze && silver && gold);
    }
}
