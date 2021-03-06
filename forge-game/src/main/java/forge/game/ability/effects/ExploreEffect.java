package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class ExploreEffect extends SpellAbilityEffect {

    

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        List<Card> tgt = getTargetCards(sa);

        sb.append(Lang.joinHomogenous(tgt));
        sb.append(" ");
        sb.append(tgt.size() > 1 ? "explore" : "explores");
        sb.append(". ");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        // check if only the activating player counts
        final Player pl = sa.getActivatingPlayer();
        final PlayerController pc = pl.getController();
        final Game game = pl.getGame();

        GameEntityCounterTable table = new GameEntityCounterTable();
        for (final Card c : getTargetCards(sa)) {
            // revealed land card
            boolean revealedLand = false;
            CardCollection top = pl.getTopXCardsFromLibrary(1);
            if (!top.isEmpty()) {
                game.getAction().reveal(top, pl, false, "Revealed for Explore - ");
                final Card r = top.getFirst();
                if (r.isLand()) {
                    game.getAction().moveTo(ZoneType.Hand, r, sa);
                    revealedLand = true;
                } else {
                	CardCollection explored = new CardCollection();
                	explored.add(r);
                	final ImmutablePair<CardCollection, CardCollection> lists = pc.arrangeForSurveil(explored);
                    final CardCollection toGrave = lists.getRight();
                    if (toGrave != null) {
                        for (Card card : toGrave) {
                        	game.getAction().moveTo(ZoneType.Graveyard, card, sa);
                        }
                    }
                }
            }
            if (!revealedLand) {
                // need to get newest game state to check
                // if it is still on the battlefield
                // and the timestamp didnt chamge
                Card gamec = game.getCardState(c);
                // if the card is not more in the game anymore
                // this might still return true but its no problem
                if (game.getZoneOf(gamec).is(ZoneType.Battlefield) && gamec.equalsWithTimestamp(c)) {
                    c.addCounter(CounterType.P1P1, 1, pl, true, table);
                }
            }

            // a creature does explore even if it isn't on the battlefield anymore
            game.getTriggerHandler().runTrigger(TriggerType.Explores, AbilityKey.mapFromCard(c), false);
        }
        table.triggerCountersPutAll(game);
    }

}
