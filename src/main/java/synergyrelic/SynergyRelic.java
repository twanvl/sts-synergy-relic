package synergyrelic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.localization.RelicStrings;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import basemod.BaseMod;
import basemod.ModPanel;
import basemod.ModButton;
import basemod.interfaces.*;
import synergyrelic.relics.*;
import synergyrelic.ModToggleButton;

@SpireInitializer
public class SynergyRelic implements PostInitializeSubscriber, PostCreateIroncladStartingRelicsSubscriber, PostCreateSilentStartingRelicsSubscriber {
    private static final String MODNAME = "Synergy Relic";
    private static final String AUTHOR = "twanvl";
    private static final String DESCRIPTION = "v1.0.1 NL Adds synergistic card selection.";

    public static boolean startWithRelic = true;
    public static boolean combineWithQuestionCard = false;
    public static boolean offerAnyRarity = false;
    private static boolean relicAdded = false;

	public static final Logger logger = LogManager.getLogger(SynergyRelic.class.getName());

    public SynergyRelic() {
        BaseMod.subscribeToPostInitialize(this);
        BaseMod.subscribeToPostCreateStartingRelics(this);
    }
    
    public static void initialize() {
        new SynergyRelic();
    }
    
    public void receivePostInitialize() {
        // Mod badge
        Texture badgeTexture = new Texture(Gdx.files.internal("img/SynergyRelicBadge.png"));
        BaseMod.registerModBadge(badgeTexture, MODNAME, AUTHOR, DESCRIPTION, settingsPanel());
        
        // RelicStrings
        String jsonString = Gdx.files.internal("localization/SynergyRelic-RelicStrings.json").readString(String.valueOf(StandardCharsets.UTF_8));
        BaseMod.loadCustomStrings(RelicStrings.class, jsonString);
        
        // Add relics
        //RelicLibrary.add(new BonsaiTree());
        addOrRemoveRelic();
    }
    private ModPanel settingsPanel() {
        return new ModPanel((panel) -> {
            panel.addLabel("Start with synergy relic", 430.0f, 720.0f, (me) -> {});
            panel.addButton(new ModToggleButton(380.0f, 720.0f-5, panel, startWithRelic, (me) -> {startWithRelic = me.enabled;}));
            panel.addLabel("Combine with question card", 430.0f, 650.0f, (me) -> {});
            panel.addButton(new ModToggleButton(380.0f, 650.0f-5, panel, combineWithQuestionCard, (me) -> {
                combineWithQuestionCard = me.enabled;
                addOrRemoveRelic();
            }));
            panel.addLabel("Offer cards of any rarity for synergy pick", 430.0f, 580.0f, (me) -> {});
            panel.addButton(new ModToggleButton(380.0f, 580.0f-5, panel, offerAnyRarity, (me) -> {offerAnyRarity = me.enabled;}));
        });
    }

    public void addOrRemoveRelic() {
        // remove/add bonsai tree to relic pool
        if (!relicAdded && !combineWithQuestionCard) {
            relicAdded = true;
            logger.info("Adding Bonsai Tree");
            RelicLibrary.add(new BonsaiTree());
        } else if (relicAdded && combineWithQuestionCard) {
            relicAdded = false;
            logger.info("Removing Bonsai Tree");
            BaseMod.removeRelic(new BonsaiTree());
        }
    }
    
	public boolean receivePostCreateStartingRelics(ArrayList<String> relics) {
        if (startWithRelic) {
            if (combineWithQuestionCard) {
                logger.info("Starting with Question Card");
                relics.add("Question Card");
            } else {
                logger.info("Starting with Bonsai Tree");
                relics.add("Bonsai Tree");
            }
        }
        return false; // keep starting relics
    }
}
