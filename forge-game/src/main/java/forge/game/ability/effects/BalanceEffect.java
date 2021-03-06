package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class BalanceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        Card source = sa.getHostCard();
        Game game = activator.getGame();
        String valid = sa.hasParam("Valid") ? sa.getParam("Valid") : "Card";
        ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
        
        int min = Integer.MAX_VALUE;
        
        final FCollectionView<Player> players = game.getPlayersInTurnOrder();
        final List<CardCollection> validCards = new ArrayList<>(players.size());
        
        for(int i = 0; i < players.size(); i++) {
            // Find the minimum of each Valid per player
            validCards.add(CardLists.getValidCards(players.get(i).getCardsIn(zone), valid, activator, source));
            min = Math.min(min, validCards.get(i).size());
        }

        final List<Card> discarded = new ArrayList<Card>();
        final List<Player> discarders = new ArrayList<Player>();

        CardZoneTable table = new CardZoneTable();
        for(int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int numToBalance = validCards.get(i).size() - min;
            if (numToBalance == 0) {
                continue;
            }
            if (zone.equals(ZoneType.Hand)) {
                for (Card card : p.getController().chooseCardsToDiscardFrom(p, sa, validCards.get(i), numToBalance, numToBalance)) {
                    if ( null == card ) continue;
                    p.discard(card, sa, table);
                    discarded.add(card);
                    if(!discarders.contains(p)) {
                    	discarders.add(p);
                    }
                }
            } else { // Battlefield
                // TODO: "can'e be sacrificed"
                for(Card card : p.getController().choosePermanentsToSacrifice(sa, numToBalance, numToBalance,  validCards.get(i), valid)) {
                    if ( null == card ) continue; 
                    game.getAction().sacrifice(card, sa, table);
                }
            }
        }
        table.triggerChangesZoneAll(game);

        HashMap<Player, CardCollection> revealCards = new HashMap<Player, CardCollection>();
        for(Card c : discarded) {
            Player p = c.getOwner();
            if(!revealCards.containsKey(p)) {
                revealCards.put(p, new CardCollection());
            }
            revealCards.get(c.getOwner()).add(c);
        }

        for(Player p : discarders) {
            if(revealCards.containsKey(p)) {
                p.getGame().getAction().reveal(revealCards.get(p), p, true);
            }
        }
    }
}
