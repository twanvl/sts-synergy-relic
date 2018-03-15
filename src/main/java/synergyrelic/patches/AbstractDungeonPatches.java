package synergyrelic.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import synergyrelic.SynergyFinder;
import synergyrelic.SynergyRelic;

// Check for Bonsai Tree / Question Card in card reward generator
// synergistic card goes at position 0 for now
class AbstractDungeonPatches {
    @SpirePatch(cls="com.megacrit.cardcrawl.dungeons.AbstractDungeon", method="getBossRewardCards")
    public static class GetBossRewardCards {
        // After "switch statement"
        @SpireInsertPatch(rloc=1752-1715, localvars={"card","rarity","i"})
        public static void Insert(@ByRef(type="cards.AbstractCard") Object[] _card, Object _rarity, int i) {
            maybeReplaceCardReward("boss ", _card, _rarity, i);
        }
    }
    @SpirePatch(cls="com.megacrit.cardcrawl.dungeons.AbstractDungeon", method="getRewardCards")
    public static class GetRewardCards {
        @SpireInsertPatch(rloc=1817-1779, localvars={"card","rarity","i"})
        public static void Insert(@ByRef(type="cards.AbstractCard") Object[] _card, Object _rarity, int i) {
            maybeReplaceCardReward("", _card, _rarity, i);
        }
    }
    public static void maybeReplaceCardReward(String which, Object[] _card, Object _rarity, int i) {
        if (i == 0 && (AbstractDungeon.player.hasRelic("Bonsai Tree")
                    || AbstractDungeon.player.hasRelic("Question Card") && SynergyRelic.combineWithQuestionCard)) {
            SynergyRelic.logger.info("Have synergy relic");
            AbstractCard card = (AbstractCard)_card[0];
            AbstractCard.CardRarity rarity = (AbstractCard.CardRarity)_rarity;
            AbstractCard newCard;
            if (SynergyRelic.offerAnyRarity) {
                newCard = SynergyFinder.getSynergyCardAnyRarity();
            } else {
                newCard = SynergyFinder.getSynergyCard(rarity);
            }
            if (newCard != null) {
                SynergyRelic.logger.info("Replace " + which + "reward: " + card.cardID + " with " + newCard.cardID);
                _card[0] = newCard;
            } else {
                SynergyRelic.logger.info("Tried to replace " + which + "reward: " + card.cardID + ", no replacement");
            }
        }
    }
}
