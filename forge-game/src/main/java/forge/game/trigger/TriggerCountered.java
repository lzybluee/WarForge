/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Countered class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerCountered.java 17802 2012-10-31 08:05:14Z Max mtg $
 */
public class TriggerCountered extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Countered.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerCountered(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Card"), this.mapParams.get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), this.mapParams.get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidCause")) {
            if (runParams2.get("Cause") == null) {
                return false;
            }
            if (!matchesValid(runParams2.get("Cause"), this.mapParams.get("ValidCause").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        
        if (this.mapParams.containsKey("ValidType")) {
            // TODO: if necessary, expand the syntax to account for multiple valid types (e.g. Spell,Ability)
            SpellAbility ctrdSA = (SpellAbility)runParams2.get("CounteredSA");
            String validType = this.mapParams.get("ValidType");
            if (ctrdSA != null) {
                if (validType.equals("Spell") && !ctrdSA.isSpell()) {
                    return false;
                } else if (validType.equals("Ability") && !ctrdSA.isAbility()) {
                    return false;
                }
            }
            
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObjectsFrom(
            this,
            AbilityKey.Card,
            AbilityKey.Cause,
            AbilityKey.Player,
            AbilityKey.CounteredSA
        );
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Countered: ").append(sa.getTriggeringObject(AbilityKey.Card)).append(", ");
        sb.append("Cause: ").append(sa.getTriggeringObject(AbilityKey.Cause));
        return sb.toString();
    }
}
