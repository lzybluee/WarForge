package forge.match;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import forge.LobbyPlayer;
import forge.interfaces.IGameController;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.GuiBase;
import forge.control.FControlGameEventHandler;
import forge.control.FControlGamePlayback;
import forge.control.WatchLocalGame;
import forge.events.IUiEventVisitor;
import forge.events.UiEvent;
import forge.events.UiEventAttackerDeclared;
import forge.events.UiEventBlockerAssigned;
import forge.events.UiEventNextGameDecision;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IGuiGame;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.trackable.TrackableCollection;
import forge.util.CollectionSuppliers;
import forge.util.collect.FCollectionView;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

public class HostedMatch {
    private Match match;
    private Game game;
    private String title;
    public HashMap<LobbySlot, IGameController> gameControllers = null;
    private Runnable startGameHook = null;
    private final List<PlayerControllerHuman> humanControllers = Lists.newArrayList();
    private Map<RegisteredPlayer, IGuiGame> guis;
    private int humanCount;
    private FControlGamePlayback playbackControl = null;
    private final MatchUiEventVisitor visitor = new MatchUiEventVisitor();
    private final Map<PlayerControllerHuman, NextGameDecision> nextGameDecisions = Maps.newHashMap();
    private boolean isMatchOver = false;

    public HostedMatch() {}

    public void setStartGameHook(Runnable hook) {
        startGameHook = hook;
    }

    private static GameRules getDefaultRules(final GameType gameType) {
        final GameRules gameRules = new GameRules(gameType);
        gameRules.setPlayForAnte(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE));
        gameRules.setMatchAnteRarity(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE_MATCH_RARITY));
        gameRules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        gameRules.setSideboardForAI(FModel.getPreferences().getPrefBoolean(FPref.MATCH_SIDEBOARD_FOR_AI));
        gameRules.setCanCloneUseTargetsImage(FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE));
        return gameRules;
    }

    public void startMatch(final GameType gameType, final Set<GameType> appliedVariants, final List<RegisteredPlayer> players, final RegisteredPlayer human, final IGuiGame gui) {
        startMatch(getDefaultRules(gameType), appliedVariants, players, human, gui);
    }
    public void startMatch(final GameType gameType, final Set<GameType> appliedVariants, final List<RegisteredPlayer> players, final Map<RegisteredPlayer, IGuiGame> guis) {
        startMatch(getDefaultRules(gameType), appliedVariants, players, guis);
    }
    public void startMatch(final GameRules gameRules, final Set<GameType> appliedVariants, final List<RegisteredPlayer> players, final RegisteredPlayer human, final IGuiGame gui) {
        startMatch(gameRules, appliedVariants, players, human == null || gui == null ? null : ImmutableMap.of(human, gui));
    }
    public void startMatch(final GameRules gameRules, final Set<GameType> appliedVariants, final List<RegisteredPlayer> players, final Map<RegisteredPlayer, IGuiGame> guis) {
        if (gameRules == null || gameRules.getGameType() == null || players == null || players.isEmpty()) {
            throw new IllegalArgumentException();
        }

        this.guis = guis == null ? ImmutableMap.of() : guis;
        final boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
        for (final RegisteredPlayer rp : players) {
            rp.setRandomFoil(useRandomFoil);
        }

        if (appliedVariants != null && !appliedVariants.isEmpty()) {
            gameRules.setAppliedVariants(appliedVariants);
        }

        final List<RegisteredPlayer> sortedPlayers = Lists.newArrayList(players);
        Collections.sort(sortedPlayers, new Comparator<RegisteredPlayer>() {
            @Override public final int compare(final RegisteredPlayer p1, final RegisteredPlayer p2) {

                final int v1 = p1.getPlayer() instanceof LobbyPlayerHuman ? 0 : 1;
                final int v2 = p2.getPlayer() instanceof LobbyPlayerHuman ? 0 : 1;
                return Integer.compare(v1, v2);
            }
        });

        if (sortedPlayers.size() == 2) {
            title = TextUtil.concatNoSpace(sortedPlayers.get(0).getPlayer().getName(), " vs ", sortedPlayers.get(1).getPlayer().getName());
        } else {
            title = TextUtil.concatNoSpace("Multiplayer Game (", String.valueOf(sortedPlayers.size()), " players)");
        }
        this.match = new Match(gameRules, sortedPlayers, title);
        startGame();
    }

    public void continueMatch() {
        endCurrentGame();
        startGame();
    }

    public void restartMatch() {
        endCurrentGame();
        this.match = new Match(match.getRules(), match.getPlayers(), this.title);
        startGame();
    }

    public void startGame() {
        nextGameDecisions.clear();
        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MATCH);

        game = match.createGame();

        if (game.getRules().getGameType() == GameType.Quest) {
            final QuestController qc = FModel.getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game.getMatch().getPlayedGames().isEmpty()) {
                qc.getCards().resetNewList();
            }
            game.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }

        game.subscribeToEvents(SoundSystem.instance);
        game.subscribeToEvents(visitor);

        final FCollectionView<Player> players = game.getPlayers();
        final String[] avatarIndices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");
        final GameView gameView = getGameView();

        humanCount = 0;
        final MapOfLists<IGuiGame, PlayerView> playersPerGui = new HashMapOfLists<>(CollectionSuppliers.arrayLists());
        for (int iPlayer = 0; iPlayer < players.size(); iPlayer++) {
            final RegisteredPlayer rp = match.getPlayers().get(iPlayer);
            final Player p = players.get(iPlayer);

            p.getLobbyPlayer().setAvatarIndex(rp.getPlayer().getAvatarIndex());
            if (p.getLobbyPlayer().getAvatarIndex() == -1) {
                if (iPlayer < avatarIndices.length) {
                    p.getLobbyPlayer().setAvatarIndex(Integer.parseInt(avatarIndices[iPlayer]));
                } else {
                    p.getLobbyPlayer().setAvatarIndex(0);
                }
            }
            p.updateAvatar();

            if (p.getController() instanceof PlayerControllerHuman) {
                final PlayerControllerHuman humanController = (PlayerControllerHuman) p.getController();
                final IGuiGame gui = guis.get(p.getRegisteredPlayer());
                humanController.setGui(gui);
                gui.setGameView(null); //clear out game view first so we don't copy into old game view
                gui.setGameView(gameView);
                gui.setOriginalGameController(p.getView(), humanController);

                game.subscribeToEvents(new FControlGameEventHandler(humanController));
                playersPerGui.add(gui, p.getView());

                if (gameControllers != null ) {
                    LobbySlot lobbySlot = getLobbySlot(p.getLobbyPlayer());
                    gameControllers.put(lobbySlot, humanController);
                }

                humanControllers.add(humanController);
                humanCount++;
            }
        }

        for (final Entry<IGuiGame, Collection<PlayerView>> e : playersPerGui.entrySet()) {
            e.getKey().openView(new TrackableCollection<>(e.getValue()));
        }

        if (humanCount == 0) { //watch game but do not participate
            final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
            gui.setGameView(null); //clear the view so when the game restarts again, it updates correctly
            gui.setGameView(gameView);

            final PlayerControllerHuman humanController = new WatchLocalGame(game, new LobbyPlayerHuman("Spectator"), gui);
            game.subscribeToEvents(new FControlGameEventHandler(humanController));
            humanControllers.add(humanController);
            gui.setSpectator(humanController);

            gui.openView(null);
        }

        //prompt user for player one name if needed
        if (StringUtils.isBlank(FModel.getPreferences().getPref(FPref.PLAYER_NAME)) && humanCount == 1) {
            GamePlayerUtil.setPlayerName();
        }

        //ensure opponents set properly
        for (final Player p : players) {
            p.updateOpponentsForView();
        }

        // It's important to run match in a different thread to allow GUI inputs to be invoked from inside game. 
        // Game is set on pause while gui player takes decisions
        game.getAction().invoke(new Runnable() {
            @Override public final void run() {
                if (humanCount == 0) {
                    // Create FControlGamePlayback in game thread to allow pausing
                    playbackControl = new FControlGamePlayback(humanControllers.get(0));
                    playbackControl.setGame(game);
                    game.subscribeToEvents(playbackControl);
                }
                // Actually start the game!
                match.startGame(game, startGameHook, FModel.getPreferences().getPref(FPref.UI_START_PLAYER),
                		FModel.getPreferences().getPrefBoolean(FPref.UI_SKIP_RESTORE_DECK),
                		FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_MTGA_SHUFFLE));

                // After game is over...
                isMatchOver = match.isMatchOver();
                if (humanCount == 0) {
                    // ... if no human players, let AI decide next game
                    if (isMatchOver) {
                        addNextGameDecision(null, NextGameDecision.QUIT);
                    } else {
                        addNextGameDecision(null, NextGameDecision.CONTINUE);
                    }
                }
            }
        });
    }

    private LobbySlot getLobbySlot(LobbyPlayer lobbyPlayer) {
        for (LobbySlot key: gameControllers.keySet()) {
            IGameController value = gameControllers.get(key);
            if (value instanceof PlayerControllerHuman) {
                if (lobbyPlayer == ((PlayerControllerHuman) value).getLobbyPlayer()) {
                    return key;
                }
            }
        }
        return null;
    }

    public void registerSpectator(final IGuiGame gui) {
        final PlayerControllerHuman humanController = new WatchLocalGame(game, null, gui);
        gui.setSpectator(humanController);
        gui.openView(null);

        game.subscribeToEvents(new FControlGameEventHandler(humanController));
        humanControllers.add(humanController);
    }

    public Game getGame() {
        return game;
    }
    public GameView getGameView() {
        return game == null ? null : game.getView();
    }

    public void endCurrentGame() {
        if (game == null) { return; }

        game = null;

        for (final PlayerControllerHuman humanController : humanControllers) {
            if (FModel.getPreferences().getPref(FPref.UI_AUTO_YIELD_MODE).equals(ForgeConstants.AUTO_YIELD_PER_CARD) || isMatchOver()) {
                // when autoyielding per card, we need to clear auto yields between games since card IDs change
                humanController.getGui().clearAutoYields();
            }

            humanController.getGui().afterGameEnd();
        }
        humanControllers.clear();
    }

    public void pause() {
        final ForgePreferences prefs = FModel.getPreferences();
        if (prefs == null) { return; } //do nothing if prefs haven't been initialized yet

        //pause playback if needed
        if (prefs.getPrefBoolean(FPref.UI_PAUSE_WHILE_MINIMIZED) && playbackControl != null) {
            playbackControl.getInput().pause();
        }
    }

    public void resume() {
        // Doesn't need to do anything right now
    }

    public boolean isMatchOver() {
        return isMatchOver;
    }

    private final class MatchUiEventVisitor implements IUiEventVisitor<Void> {
        @Override
        public Void visit(final UiEventBlockerAssigned event) {
            for (final PlayerControllerHuman humanController : humanControllers) {
                humanController.getGui().updateSingleCard(event.blocker);
                final PlayerView p = humanController.getPlayer().getView();
                if (event.attackerBeingBlocked != null && event.attackerBeingBlocked.getController().equals(p)) {
                    humanController.getGui().autoPassCancel(p);
                }
            }
            return null;
        }

        @Override
        public Void visit(final UiEventAttackerDeclared event) {
            for (final PlayerControllerHuman humanController : humanControllers) {
                humanController.getGui().updateSingleCard(event.attacker);
            }
            return null;
        }

        @Override
        public Void visit(final UiEventNextGameDecision event) {
            addNextGameDecision(event.getController(), event.getDecision());
            return null;
        }

        @Subscribe
        public void receiveEvent(final UiEvent evt) {
            try {
                evt.visit(this);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addNextGameDecision(final PlayerControllerHuman controller, final NextGameDecision decision) {
        if (decision == NextGameDecision.QUIT) {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override public void run() {
                    endCurrentGame();
                    isMatchOver = true;
                }
            });
            return; // if any player chooses quit, quit the match
        }

        nextGameDecisions.put(controller, decision);
        if (nextGameDecisions.size() < humanControllers.size()) {
            return;
        }

        int newMatch = 0, continueMatch = 0;
        for (final NextGameDecision dec : nextGameDecisions.values()) {
            switch (dec) {
            case CONTINUE:
                continueMatch++;
                break;
            case NEW:
                newMatch++;
                break;
            default:
            }
        }

        if (continueMatch >= newMatch) {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override public void run() {
                    continueMatch();
                }
            });
        } else {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override public void run() {
                    restartMatch();
                }
            });
        }
    }
}
