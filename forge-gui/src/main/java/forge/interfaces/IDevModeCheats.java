package forge.interfaces;

public interface IDevModeCheats {

    void setCanPlayUnlimited(boolean canPlayUnlimited);

    void setViewAllCards(boolean canViewAll);

    void generateMana(boolean empty);

    void dumpGameState(boolean quick);

    void setupGameState(boolean lastState);

    void tutorForCard(boolean sideboard);

    void addCountersToPermanent(boolean player);

    void removeCountersFromPermanent(boolean player);

    void tapPermanents(boolean all);

    void untapPermanents(boolean all);

    void setPlayerLife(boolean maxLife);

    void winGame(boolean lose);

    void addCardToHand(boolean mostCommon);

    void addCardToBattlefield(boolean mostCommon);

    void addCardToLibrary(boolean mostCommon);

    void addCardToGraveyard(boolean mostCommon);

    void addCardToExile(boolean mostCommon);

    void castASpell(boolean mostCommon);

    void repeatLastAddition();

    /*
     * Exiles cards from specified player's hand. Will prompt user for player and cards.
     */
    void exileCardsFromHand();

    /*
     * Exiles cards from play. Will prompt user for player and cards.
     */
    void exileCardsFromBattlefield();

    void removeCardsFromGame();


    void riggedPlanarRoll();

    void planeswalkTo();

    /**
     * Implementation of {@link IDevModeCheats} that disallows cheating by
     * performing no action whatsoever when any of its methods is called.
     */
    IDevModeCheats NO_CHEAT = new IDevModeCheats() {
        @Override
        public void winGame(boolean lose) {
        }
        @Override
        public void untapPermanents(boolean all) {
        }
        @Override
        public void tutorForCard(boolean sideboard) {
        }
        @Override
        public void tapPermanents(boolean all) {
        }
        @Override
        public void setupGameState(boolean lastState) {
        }
        @Override
        public void setViewAllCards(final boolean canViewAll) {
        }
        @Override
        public void setPlayerLife(boolean maxLife) {
        }
        @Override
        public void setCanPlayUnlimited(final boolean canPlayUnlimited) {
        }
        @Override
        public void riggedPlanarRoll() {
        }
        @Override
        public void planeswalkTo() {
        }
        @Override
        public void generateMana(boolean empty) {
        }
        @Override
        public void dumpGameState(boolean quick) {
        }
        @Override
        public void addCountersToPermanent(boolean player) {
        }
        @Override
        public void removeCountersFromPermanent(boolean player) {
        }
        @Override
        public void addCardToHand(boolean mostCommon) {
        }
        @Override
        public void exileCardsFromHand() {
        }
        @Override
        public void exileCardsFromBattlefield() {
        }
        @Override
        public void addCardToBattlefield(boolean mostCommon) {
        }
        @Override
        public void addCardToLibrary(boolean mostCommon) {
        }
        @Override
        public void addCardToGraveyard(boolean mostCommon) {
        }
        @Override
        public void addCardToExile(boolean mostCommon) {
        }
        @Override
        public void castASpell(boolean mostCommon) {
        }
        @Override
        public void repeatLastAddition() {
        }
        @Override
        public void removeCardsFromGame() {
        }
    };

}