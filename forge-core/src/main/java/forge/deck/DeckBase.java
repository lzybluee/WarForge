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
package forge.deck;

import forge.item.InventoryItem;
import java.io.Serializable;

public abstract class DeckBase implements Serializable, Comparable<DeckBase>, InventoryItem {
    private static final long serialVersionUID = -7538150536939660052L;
    // gameType is from Constant.GameType, like GameType.Regular

    private final String name;
    private transient String directory;
    private String comment = null;
    private String path;

    /**
     * Instantiates a new deck base.
     *
     * @param name0 the name0
     */
    public DeckBase(final String name0) {
        name = name0.replace('/', '_');
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final DeckBase d) {
        return name.compareTo(d.name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof DeckBase) {
            final DeckBase d = (DeckBase) o;
            return name.equals(d.name);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (name.hashCode() * 17) + name.hashCode();
    }

    public String getName() {
        return name;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory0) {
        directory = directory0;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path0) {
    	path = path0;
    }

    public String getUniqueKey() {
        if (directory == null) { return name; }
        return directory + "/" + name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Sets the comment.
     *
     * @param comment0 the new comment
     */
    public void setComment(final String comment0) {
        comment = comment0;
    }

    /**
     * <p>
     * getComment.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getComment() {
        return comment;
    }

    /**
     * New instance.
     *
     * @param name0 the name0
     * @return the deck base
     */
    protected abstract DeckBase newInstance(String name0);

    /**
     * Clone fields to.
     *
     * @param clone the clone
     */
    protected void cloneFieldsTo(final DeckBase clone) {
        clone.directory = directory;
        clone.comment = comment;
    }

    /**
     * Copy to.
     *
     * @param name0 the name0
     * @return the deck base
     */
    public DeckBase copyTo(final String name0) {
        final DeckBase obj = newInstance(name0);
        cloneFieldsTo(obj);
        return obj;
    }

    /**
     * Gets the best file name.
     *
     * @return the best file name
     */
    public final String getBestFileName() {
        return name.replaceAll("[^-_$#@.,{[()]} a-zA-Z0-9]", "");
    }

    public abstract boolean isEmpty();

    public abstract Deck getHumanDeck();
}
