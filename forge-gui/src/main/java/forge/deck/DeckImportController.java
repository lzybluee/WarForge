package forge.deck;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.interfaces.ICheckBox;
import forge.interfaces.IComboBox;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.gui.SOptionPane;

public class DeckImportController {
    private final boolean replacingDeck;
    private final ICheckBox newEditionCheck, dateTimeCheck, onlyCoreExpCheck;
    private final IComboBox<String> monthDropdown;
    private final IComboBox<Integer> yearDropdown;
    private final List<DeckRecognizer.Token> tokens = new ArrayList<>();

    public DeckImportController(boolean replacingDeck0, ICheckBox newEditionCheck0, ICheckBox dateTimeCheck0, ICheckBox onlyCoreExpCheck0, IComboBox<String> monthDropdown0, IComboBox<Integer> yearDropdown0) {
        replacingDeck = replacingDeck0;
        newEditionCheck = newEditionCheck0;
        dateTimeCheck = dateTimeCheck0;
        onlyCoreExpCheck = onlyCoreExpCheck0;
        monthDropdown = monthDropdown0;
        yearDropdown = yearDropdown0;

        fillDateDropdowns();
    }

    private void fillDateDropdowns() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        monthDropdown.removeAllItems();
        String[] months = dfs.getMonths();
        for (String monthName : months) {
            if (!StringUtils.isBlank(monthName)) {
                monthDropdown.addItem(monthName);
            }
        }
        int yearNow = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = yearNow; i >= 1993; i--) {
            yearDropdown.addItem(Integer.valueOf(i));
        }
    }

    public List<DeckRecognizer.Token> parseInput(String input) {
        tokens.clear();
        input = input.replaceAll("Æ", "Ae");

        DeckRecognizer recognizer = new DeckRecognizer(newEditionCheck.isSelected(),  onlyCoreExpCheck.isSelected(), FModel.getMagicDb().getCommonCards());
        if (dateTimeCheck.isSelected()) {
            recognizer.setDateConstraint(monthDropdown.getSelectedIndex(), yearDropdown.getSelectedItem());
        }
        String[] lines = null;
        if(input.contains("\n")) {
        	lines = input.split("\n");
        } else {
        	lines = input.split("\r");
        }
        for (String line : lines) {
            tokens.add(recognizer.recognizeLine(line));
        }

        boolean autoSideboard = true;
        int cardNum = 0;
        for (final DeckRecognizer.Token t : tokens) {
            final DeckRecognizer.TokenType type = t.getType();
            if ((type == DeckRecognizer.TokenType.SectionName) && t.getText().toLowerCase().contains("side")) {
                autoSideboard = false;
                break;
            }
            if (type == DeckRecognizer.TokenType.KnownCard) {
                cardNum += t.getNumber();
            }
        }
        autoSideboard &= (cardNum == 75);

        if(autoSideboard) {
            cardNum = 0;
            List<DeckRecognizer.Token> newTokens = new ArrayList<DeckRecognizer.Token>();
            for (final DeckRecognizer.Token t : tokens) {
                final DeckRecognizer.TokenType type = t.getType();
                newTokens.add(t);
                if (type == DeckRecognizer.TokenType.KnownCard) {
                    cardNum += t.getNumber();
                    if(cardNum == 60) {
                        newTokens.add(new DeckRecognizer.Token(DeckRecognizer.TokenType.SectionName, 0, "side"));
                    }
                }
            }
            tokens.clear();
            tokens.addAll(newTokens);
        }
        return tokens;
    }

    public Deck accept() {
    	if (tokens.isEmpty()) { return null; }

        if (replacingDeck) {
            final String warning = "This will replace the contents of your current deck with these cards.\n\nProceed?";
            if (!SOptionPane.showConfirmDialog(warning, "Replace Current Deck", "Replace", "Cancel")) {
                return null;
            }
        }

        final Deck result = new Deck();
        boolean isMain = true;
        boolean isCommander = false;
        for (final DeckRecognizer.Token t : tokens) {
            final DeckRecognizer.TokenType type = t.getType();
            if (type == DeckRecognizer.TokenType.SectionName) {
            	if(t.getText().toLowerCase().contains("commander")) {
            		isCommander = true;
            	} else {
            		isCommander = false;
            		if(t.getText().toLowerCase().contains("side")) {
                		isMain = false;
                	}
            	}
            	continue;
            }
            if (type != DeckRecognizer.TokenType.KnownCard) {
            	if(!t.getText().trim().isEmpty()) {
            		isCommander = false;
            	}
                continue;
            }
            final PaperCard crd = t.getCard();
            if(isCommander) {
            	result.getOrCreate(DeckSection.Commander).add(crd, t.getNumber());
            }
            else if (isMain) {
                result.getMain().add(crd, t.getNumber());
            }
            else {
                result.getOrCreate(DeckSection.Sideboard).add(crd, t.getNumber());
            }
        }
        return result;
    }
}
