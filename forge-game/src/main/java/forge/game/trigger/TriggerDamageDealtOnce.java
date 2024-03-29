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

import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

import java.util.Set;

/**
 * <p>
 * Trigger_DamageDone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerDamageDone.java 21390 2013-05-08 07:44:50Z Max mtg $
 */
public class TriggerDamageDealtOnce extends Trigger {

    /**
     * <p>
     * Constructor for TriggerDamageDealtOnce.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerDamageDealtOnce(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card srcs = (Card) runParams2.get("DamageSource");
        final Set<GameEntity> tgt = (Set<GameEntity>) runParams2.get("DamageTargets");

        if (this.mapParams.containsKey("CombatDamage")) {
            if (this.mapParams.get("CombatDamage").equals("True")) {
                if (!((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            } else if (this.mapParams.get("CombatDamage").equals("False")) {
                if (((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            }
        }
        
        if (this.mapParams.containsKey("ValidTarget")) {
            boolean valid = false;
            for (GameEntity c : tgt) {
                if (c.isValid(this.mapParams.get("ValidTarget").split(","), this.getHostCard().getController(),this.getHostCard(), null)) {
                    valid = true;
                }
            }
            if (!valid) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidSource")) {
            if (!matchesValid(srcs, this.mapParams.get("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("DamageAmount")) {
            final String fullParam = this.mapParams.get("DamageAmount");

            final String operator = fullParam.substring(0, 2);
            final int operand = Integer.parseInt(fullParam.substring(2));
            final int actualAmount = (Integer) runParams2.get("DamageAmount");

            if (!Expressions.compare(actualAmount, operator, operand)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObjectsFrom(this, AbilityKey.DamageAmount);
        sa.setTriggeringObject(AbilityKey.Source, getFromRunParams(AbilityKey.DamageSource));
        sa.setTriggeringObject(AbilityKey.Targets, getFromRunParams(AbilityKey.DamageTargets));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Damage Source: ").append(sa.getTriggeringObject(AbilityKey.Source)).append(", ");
        sb.append("Damaged: ").append(sa.getTriggeringObject(AbilityKey.Targets)).append(", ");
        sb.append("Amount: ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }
}
