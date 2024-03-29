package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class PumpAllEffect extends SpellAbilityEffect {
    private static void applyPumpAll(final SpellAbility sa,
            final List<Card> list, final int a, final int d,
            final List<String> keywords, final List<ZoneType> affectedZones) {
        
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();
        final List<String> kws = Lists.newArrayList();
        final List<String> hiddenkws = Lists.newArrayList();
        
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                hiddenkws.add(kw);
            } else {
                kws.add(kw);
            }
        }
        
        for (final Card tgtC : list) {
            // only pump things in the affected zones.
            boolean found = false;
            for (final ZoneType z : affectedZones) {
                if (tgtC.isInZone(z)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                continue;
            }

            boolean redrawPT = false;

            if (a != 0 || d != 0) {
                tgtC.addPTBoost(a, d, timestamp, 0);
                redrawPT = true;
            }

            tgtC.addChangedCardKeywords(kws, null, false, false, timestamp);
            if (redrawPT) {
                tgtC.updatePowerToughnessForView();
            }

            for (String kw : hiddenkws) {
                tgtC.addHiddenExtrinsicKeyword(kw);
            }

            if (sa.hasParam("RememberAllPumped")) {
                sa.getHostCard().addRemembered(tgtC);
            }
        
            if (!sa.hasParam("Permanent")) {
                // If not Permanent, remove Pumped at EOT
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 5415795460189457660L;

                    @Override
                    public void run() {
                        tgtC.removePTBoost(timestamp, 0);
                        tgtC.removeChangedCardKeywords(timestamp);

                        for (String kw : hiddenkws) {
                            tgtC.removeHiddenExtrinsicKeyword(kw);
                        }
                        tgtC.updatePowerToughnessForView();

                        game.fireEvent(new GameEventCardStatsChanged(tgtC));
                    }
                };
                if (sa.hasParam("UntilUntaps")) {
                    sa.getHostCard().addUntapCommand(untilEOT);
                } else if (sa.hasParam("UntilEndOfCombat")) {
                    game.getEndOfCombat().addUntil(untilEOT);
                } else if (sa.hasParam("UntilYourNextTurn")) {
                    game.getCleanup().addUntil(sa.getActivatingPlayer(), untilEOT);
                } else if (sa.hasParam("UntilLoseControl")) {
                    tgtC.addLeavesPlayCommand(untilEOT);
                    tgtC.addChangeControllerCommand(untilEOT);
                } else {
                    game.getEndOfTurn().addUntil(untilEOT);
                }
            }

            game.fireEvent(new GameEventCardStatsChanged(tgtC));
        }
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (sa.hasParam("SpellDescription")) {
            desc = TextUtil.fastReplace(sa.getParam("SpellDescription"), "CARDNAME", sa.getHostCard().getName());
        }

        sb.append(desc);

        return sb.toString();
    } // pumpAllStackDescription()

    @Override
    public void resolve(final SpellAbility sa) {
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final List<ZoneType> affectedZones = Lists.newArrayList();
        final Game game = sa.getActivatingPlayer().getGame();

        if (sa.hasParam("PumpZone")) {
            affectedZones.addAll(ZoneType.listValueOf(sa.getParam("PumpZone")));
        } else {
            affectedZones.add(ZoneType.Battlefield);
        }

        CardCollection list = new CardCollection();
        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            for (final ZoneType zone : affectedZones) {
                list.addAll(game.getCardsIn(zone));
            }
        } else {
            for (final ZoneType zone : affectedZones) {
                for (final Player p : tgtPlayers) {
                    list.addAll(p.getCardsIn(zone));
                }
            }
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        list = (CardCollection)AbilityUtils.filterListByType(list, valid, sa);

        List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("KW")) {
            keywords.addAll(Arrays.asList(sa.getParam("KW").split(" & ")));
        }
        final int a = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumAtt"), sa, true);
        final int d = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDef"), sa, true);
        
        if (sa.hasParam("SharedKeywordsZone")) {
            List<ZoneType> zones = ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
            String[] restrictions = new String[] {"Card"};
            if (sa.hasParam("SharedRestrictions"))
                restrictions = sa.getParam("SharedRestrictions").split(",");
            keywords = CardFactoryUtil.sharedKeywords(keywords, restrictions, zones, sa.getHostCard());
        }
        applyPumpAll(sa, list, a, d, keywords, affectedZones);

        replaceDying(sa);
    } // pumpAllResolve()

}
