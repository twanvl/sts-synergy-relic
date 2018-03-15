package synergyrelic;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.red.*;
import com.megacrit.cardcrawl.cards.green.*;
import com.megacrit.cardcrawl.cards.colorless.*;
import com.megacrit.cardcrawl.cards.curses.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SynergyFinder {
    public static final Logger logger = LogManager.getLogger(SynergyRelic.class.getName());
    
    class SynergyEntry {
        float weight = 0.f;
        ArrayList<String> trace = new ArrayList<String>();
        AbstractCard card;
    }
    // Cards that have synergy with the current deck, with a count/weight each.
    // Cards that are not in this map are not allowed for the current player/rarity
    // Note: we use a LinkedHashMap to get a consistent order
    Map<String,SynergyEntry> pool = new LinkedHashMap<String,SynergyEntry>();
    double total_weight;
    // hacky semi-global variable to trace which card there is synergy with, only for debuging
    String trace = null;
    
    // rarer cards in our deck weigh more heavily, starter cards less heavily
    private static final float STARTER_WEIGHT  = 0.2f;
    private static final float COMMON_WEIGHT   = 1.0f;
    private static final float UNCOMMON_WEIGHT = 1.2f;
    private static final float RARE_WEIGHT     = 1.8f;
    private static final float DEFAULT_WEIGHT  = 1.0f;
    private static final float RELIC_WEIGHT    = 1.0f;
    private static final float UPGRADED_MULTIPLIER = 1.5f; // upgraded cards in our deck weigh more heavily
    private static final float EXHAUSTS_MULTIPLIER = 0.5f; // synergies with exhaust cards we have
    private static final float SINGLE_USE_MULTIPLIER = 0.33f; // synergies for picking cards that exhaust themselves
    private static final float ANY_ATTACK_MULTIPLIER = 0.1f; // picking just any attack
    private static final float ANY_SKILL_MULTIPLIER = 0.1f;
    private static final float ANY_POWER_MULTIPLIER = 0.3f;
    private static final float WEAK_MULTIPLIER = 0.2f; // weak synergy: energy <-> x cards

    // initialize the pool of available cards
    public SynergyFinder(ArrayList<AbstractCard> base_pool) {
        total_weight = 0;
        SynergyRelic.logger.info("Init1");
        pool.clear();
        SynergyRelic.logger.info("Init2");
        for (AbstractCard card : base_pool) {
            SynergyEntry entry = new SynergyEntry();
            entry.card = card;
            pool.put(card.cardID, entry);
        }
    }
    
    // Find a card
    public AbstractCard getRandomCard() {
        if (total_weight == 0) return null;
        logger.info("Synergy pool has total weight: " + total_weight);
        double i = AbstractDungeon.cardRng.random() * total_weight;
        // get the card at the given (weighted) position in the pool
        for (Map.Entry<String,SynergyEntry> entry : pool.entrySet()) {
            if (entry.getValue().weight > 0) {
                logger.info("  option: " + entry.getKey() + " weight " + entry.getValue().weight +  ", synergy with: " + String.join(", ",entry.getValue().trace));
            }
        }
        for (Map.Entry<String,SynergyEntry> entry : pool.entrySet()) {
            i -= entry.getValue().weight;
            if (i < 0) {
                logger.info("Picked synergy card: " + entry.getKey());
                logger.info("  it has weight " + entry.getValue().weight +  ", synergy with: " + String.join(", ",entry.getValue().trace));
                return entry.getValue().card;
            }
        }
        logger.info("ERROR: no card found");
        return null;
    }
    
    // Find a card that has synergy with the current deck
    public static AbstractCard getSynergyCard(AbstractCard.CardRarity rarity) {
        switch (rarity) {
            case COMMON:
                return getSynergyCard(AbstractDungeon.player.masterDeck, AbstractDungeon.player.relics, AbstractDungeon.commonCardPool.group);
            case UNCOMMON:
                return getSynergyCard(AbstractDungeon.player.masterDeck, AbstractDungeon.player.relics, AbstractDungeon.uncommonCardPool.group);
            case RARE:
                return getSynergyCard(AbstractDungeon.player.masterDeck, AbstractDungeon.player.relics, AbstractDungeon.rareCardPool.group);
            default:
                return null;
        }
    }
    public static AbstractCard getSynergyCardAnyRarity() {
        ArrayList<AbstractCard> combinedPool = new ArrayList<AbstractCard>();
        combinedPool.addAll(AbstractDungeon.commonCardPool.group);
        combinedPool.addAll(AbstractDungeon.uncommonCardPool.group);
        combinedPool.addAll(AbstractDungeon.rareCardPool.group);
        return getSynergyCard(AbstractDungeon.player.masterDeck, AbstractDungeon.player.relics, combinedPool);
    }
    public static AbstractCard getSynergyCard(CardGroup deck, ArrayList<AbstractRelic> relics, ArrayList<AbstractCard> pool) {
        SynergyFinder s = new SynergyFinder(pool);
        for (AbstractCard c : deck.group) {
            s.findSynergy(c);
        }
        for (AbstractRelic r : relics) {
            s.findSynergy(r);
        }
        return s.getRandomCard();
    }
    
    void add(String card, float weight) {
        add(pool.get(card), weight);
    }
    void add(SynergyEntry e, float weight) {
        if (e != null) {
            // TODO: multiplier for having the appropriate Egg?
            if (trace != null) e.trace.add(trace);
            e.weight += weight;
            total_weight += weight;
        }
    }
    
    // Add cards that have synergy with `card` to the pool
    void findSynergy(AbstractCard card) {
        trace = card.name;
        float weight = DEFAULT_WEIGHT;
        switch (card.rarity) {
            case BASIC:    weight = card.upgraded ? COMMON_WEIGHT : STARTER_WEIGHT; break;
            case COMMON:   weight = COMMON_WEIGHT; break;
            case UNCOMMON: weight = UNCOMMON_WEIGHT; break;
            case RARE:     weight = RARE_WEIGHT; break;
            default:       weight = DEFAULT_WEIGHT;
        }
        if (card.upgraded) weight *= UPGRADED_MULTIPLIER;
        if (card.exhaust)  weight *= EXHAUSTS_MULTIPLIER;
        SynergyRelic.logger.info("Adding synergies with " + card.name + ", weight " + weight);
        switch (card.cardID) {
            // red cards
            case Anger.ID: add_cheapAttack_use(weight); break;
            case Armaments.ID: add_upgrade_use(weight); break;
            case Barricade.ID: add_barricade_use(weight); break;
            case Bash.ID: add_vulnerable_use(weight); break;
            case BattleTrance.ID: add_draw_use(weight); break;
            case Berserk.ID: add_energy_use(weight); break;
            case BloodForBlood.ID: add_selfDamage(weight); break;
            case Bloodletting.ID: add_selfDamage_use(weight); break;
            case Bludgeon.ID: add_bigAttack_use(weight); break;
            case BodySlam.ID: add_cheapAttack_use(weight); add_block(weight); break;
            case Brutality.ID: add_selfDamage_use(weight); break;
            case BurningPact.ID: add_exhaustOther_use(weight); add_draw_use(weight); break;
            case Carnage.ID: add_bigAttack_use(weight); break;
            case Clash.ID: add_cheapAttack_use(weight); break;
            case Cleave.ID: break; //??
            case Clothesline.ID: add_weak_use(weight); add_bigAttack_use(weight); break;
            case Combust.ID: add_selfDamage_use(weight); break;
            case Corruption.ID: add_bigSkill(weight); add_exhaustOther_use(weight); break;
            case DarkEmbrace.ID: add_exhaust(weight); add_draw_use(weight); break;
            case Defend_Red.ID: break; // none
            case DemonForm.ID: add_strength_use(weight); break;
            case Disarm.ID: add_debuf_use(weight); add_exhaust_use(weight); break;
            case DoubleTap.ID: add_doubleTap_use(weight); break;
            case Dropkick.ID: add_vulnerable(weight); break;
            case DualWield.ID: add_cheapAttack(weight); break; // ??
            case Entrench.ID: add_block_use(weight); break;
            case Evolve.ID: add_wound(weight); break;
            case Exhume.ID: add_exhaust(weight); add_exhaust_use(weight); break;
            case Feed.ID: add_exhaust_use(weight); break;
            case FeelNoPain.ID: add_exhaust(weight); break;
            case FiendFire.ID: add_exhaust_use(weight); add_bigAttack_use(weight); break;
            case FlameBarrier.ID: add_bigSkill_use(weight); add_block_use(weight); break;
            case FireBreathing.ID: add_cheapAttack(weight); break;
            case Flex.ID: add_strength_use(weight); break;
            case GhostlyArmor.ID: add_block_use(weight); break;
            case Havoc.ID: add_exhaustOther_use(weight); add_cheapSkill_use(weight); break;
            case Headbutt.ID: add_plan_use(weight); break;
            case HeavyBlade.ID: add_strength(weight); add_bigAttack_use(weight); break;
            case Hemokinesis.ID: add_selfDamage_use(weight); break;
            case Immolate.ID: add_status(weight); add_exhaustOther_use(weight); break;
            case Impervious.ID: add_block_use(weight); add_exhaust_use(weight); break;
            case InfernalBlade.ID: add_exhaust_use(weight); break; // none?
            case Inflame.ID: add_strength_use(weight); break;
            case Intimidate.ID: add_exhaust_use(weight); add_weak_use(weight); break;
            case IronWave.ID: break; // none?
            case Juggernaut.ID: add_block(weight); break;
            case LimitBreak.ID: add_strength(weight); add_strength_use(weight); break;
            case Metallicize.ID: add_block_use(weight); break;
            case Offering.ID: add_selfDamage_use(weight); add_exhaust_use(weight); break;
            case PerfectedStrike.ID: add_strike(weight); break;
            case PommelStrike.ID: add_draw_use(weight); break;
            case PowerThrough.ID: add_wound_use(weight); break;
            case Pummel.ID: add_strength(weight); add_exhaust_use(weight); break;
            case Rage.ID: add_cheapAttack(weight); break;
            case Rampage.ID: add_bigAttack_use(weight); break;
            case Reaper.ID: add_strength(weight); break;
            case RecklessCharge.ID: add_cheapAttack_use(weight); break;
            case Rupture.ID: add_strength_use(weight); add_selfDamage(weight); break;
            case SearingBlow.ID: add_upgrade(weight); break;
            case SecondWind.ID: add_exhaustOther_use(weight);
            case SeeingRed.ID: add_exhaust_use(weight); add_energy_use(weight); break;
            case Sentinel.ID: add_exhaust(weight); add_energy_use(weight); break;
            case SeverSoul.ID: add_exhaustOther_use(weight); break;
            case Shockwave.ID: add_weak_use(weight); add_vulnerable_use(weight); add_exhaust_use(weight); break;
            case ShrugItOff.ID: add_block_use(weight); break;
            case SpotWeakness.ID: add_strength_use(weight); break;
            case Strike_Red.ID: break; // none
            case SwordBoomerang.ID: add_strength(weight); break;
            case ThunderClap.ID: add_vulnerable_use(weight); break;
            case TrueGrit.ID: add_exhaustOther_use(weight); break;
            case TwinStrike.ID: add_strength(weight); break;
            case Uppercut.ID: add_bigAttack_use(weight); add_vulnerable_use(weight); break;
            case Warcry.ID: add_plan_use(weight); break;
            case Whirlwind.ID: add_bigAttack_use(weight); add_strength(weight); add_energy(weight*WEAK_MULTIPLIER); break;
            case WildStrike.ID: add_wound_use(weight); break;

            // green cards
            case Accuracy.ID: add_shiv(weight); break;
            case Acrobatics.ID: add_draw_use(weight); add_discard_use(weight); break;
            case Adrenaline.ID: add_exhaust_use(weight); add_draw_use(weight); break;
            case AfterImage.ID: add_cheapCard(weight); break;
            case Alchemize.ID: add_exhaust_use(weight); break; // none
            case AllOutAttack.ID: add_bigAttack_use(weight); break;
            case AThousandCuts.ID: add_cheapCard(weight); break;
            case Backflip.ID: add_draw_use(weight); add_block_use(weight); break;
            case Backstab.ID: add_cheapCard_use(weight); add_exhaust_use(weight); break;
            case Bane.ID: add_poison(weight); break;
            case BladeDance.ID: add_shiv_use(weight); break;
            case Blur.ID: add_barricade_use(weight); add_block_use(weight); break;
            case BouncingFlask.ID: add_poison_use(weight); break;
            case BulletTime.ID: add_bigCard(weight); break;
            case Burst.ID: add_bigSkill(weight); break;
            case CalculatedGamble.ID: add_cheapSkill_use(weight); add_discard_use(weight); break;
            case Caltrops.ID: break; // maybe block?
            case Catalyst.ID: add_poison(weight); break;
            case Choke.ID: add_cheapCard(weight); add_bigAttack_use(weight); break;
            case CloakAndDagger.ID: add_shiv_use(weight); add_block_use(weight); break;
            case Concentrate.ID: add_energy_use(weight); add_discard_use(weight); break;
            case CorpseExplosion.ID: add_poison(weight); break; // not bigAttack_use
            case CripplingPoison.ID: add_poison_use(weight); break;
            case DaggerSpray.ID: add_strength(weight); break;
            case DaggerThrow.ID: add_discard_use(weight); break;
            case Dash.ID: add_bigAttack_use(weight); add_block_use(weight); break;
            case DeadlyPoison.ID: add_poison_use(weight); break;
            case Defend_Green.ID: break;
            case Deflect.ID: add_cheapSkill_use(weight); add_block_use(weight); break;
            case DieDieDie.ID: add_exhaust_use(weight); add_strength(weight); break;
            case Distraction.ID: break; //??
            case DodgeAndRoll.ID: add_dexterity(weight); break;
            case Doppelganger.ID: add_plan(weight); add_energy_use(weight); break;
            case EndlessAgony.ID: add_cheapAttack_use(weight); break;
            case Envenom.ID: add_cheapAttack(weight); add_poison_use(weight); break;
            case EscapePlan.ID: add_cheapSkill_use(weight); add_block_use(weight); break;
            case Eviscerate.ID: add_discard(weight); break;
            case Expertise.ID: add_draw_use(weight); break;
            case Finisher.ID: add_cheapAttack(weight); break;
            case Flechettes.ID: add_skill(weight); break;
            case FlyingKnee.ID: break;
            case Footwork.ID: add_dexterity_use(weight); break;
            case GlassKnife.ID: add_strength(weight); break;
            case GrandFinale.ID: add_plan(weight); add_draw(weight); break;
            case HeelHook.ID: add_weak(weight); break;
            case InfiniteBlades.ID: add_shiv_use(weight); break;
            case LegSweep.ID: add_weak_use(weight); break;
            case Malaise.ID: add_weak_use(weight); add_energy(weight); break;
            case MasterfulStab.ID: add_discard(weight); add_cheapAttack_use(weight); break;
            case Neutralize.ID: add_weak_use(weight); add_cheapAttack_use(weight); break;
            case Nightmare.ID: break; //??
            case NoxiousFumes.ID: add_poison_use(weight); break;
            case Outmaneuver.ID: add_energy_use(weight); break;
            case PhantasmalKiller.ID: add_plan(weight); break; //?
            case PiercingWail.ID: add_debuf_use(weight); break; //??
            case PoisonedStab.ID: add_poison_use(weight); break;
            case Predator.ID: add_bigAttack_use(weight); break;
            case Prepared.ID: add_cheapSkill_use(weight); add_discard_use(weight); break;
            case QuickSlash.ID:  break;//?
            case Reflex.ID: add_discard(weight); add_retain(weight); break;
            case RiddleWithHoles.ID: add_bigAttack_use(weight); add_strength(weight); break;
            case Setup.ID: add_plan_use(weight); break;
            case Skewer.ID: add_strength(weight); add_bigAttack_use(weight); add_energy(weight); break;
            case Slice.ID: add_cheapAttack_use(weight); break;
            case StormOfSteel.ID: add_shiv_use(weight); break;
            case Strike_Green.ID: break; // none
            case SuckerPunch.ID: add_weak_use(weight); break;
            case Survivor.ID: add_discard_use(weight); break;
            case Tactician.ID: add_discard(weight); add_retain(weight); break;
            case Terror.ID: add_vulnerable_use(weight); break;
            case ToolsOfTheTrade.ID: add_plan_use(weight); add_discard_use(weight); break;
            case UnderhandedStrike.ID: add_discard(weight); add_retain(weight); break;
            case Unload.ID: add_discard_use(weight); break;
            case WellLaidPlans.ID: add_plan_use(weight); break;
            case WraithForm.ID: add_cheapSkill(weight); break;
            
            // colorless cards
            case Apotheosis.ID: add_upgrade_use(weight); break;
            case BandageUp.ID: break;
            case Bite.ID: add_selfDamage(weight*WEAK_MULTIPLIER); break;
            case Blind.ID: add_weak_use(weight); break;
            case DarkShackles.ID: add_exhaust_use(weight); break;
            case DeepBreath.ID: add_cheapSkill_use(weight);  break;
            case DramaticEntrance.ID: break;
            case Finesse.ID: add_cheapSkill_use(weight); break;
            case FlashOfSteel.ID: add_cheapAttack_use(weight); break;
            case GoodInstincts.ID: break;
            case JAX.ID: add_strength_use(weight); add_exhaust_use(weight); add_selfDamage_use(weight); break;
            case Madness.ID: add_cheapCard_use(weight); add_exhaust_use(weight); break;
            case MindBlast.ID: add_cheapAttack_use(weight); break;
            case Panache.ID: add_cheapCard(weight); break;
            case Purity.ID: add_exhaustOther_use(weight); break;
            case SadisticNature.ID: add_debuf(weight); break;
            case SecretTechnique.ID: add_skill(weight); break; //??
            case SecretWeapon.ID: add_attack(weight); break;
            case SwiftStrike.ID: add_cheapAttack_use(weight); break; //??
            case ThinkingAhead.ID: add_plan_use(weight); break;
            case Transmutation.ID: add_exhaust_use(weight); add_energy(WEAK_MULTIPLIER); break;
            case Trip.ID: add_vulnerable_use(weight); break;

            // curses
            case Clumsy.ID:
            case Necronomicurse.ID:
                add_curse_use(weight);
                break;
            case Doubt.ID:
            case Decay.ID:
            case Normality.ID:
            case Pain.ID:
            case Regret.ID:
                add_exhaustOther(weight);
                add_discard(weight);
                add_curse_use(weight);
                break;
            case Injury.ID:
            case Parasite.ID:
            case Writhe.ID:
                add_exhaustOther(weight);
                add_curse_use(weight);
                // discard?
                break;

            default:
                logger.info("Synergy: unkown card: " + card.cardID);
        }
        trace = null;
    }
    // Synergy with relics
    void findSynergy(AbstractRelic relic) {
        trace = relic.relicId;
        float weight = RELIC_WEIGHT;
        switch (relic.relicId) {
            case "Bag of Marbles": add_vulnerable_use(weight); break;
            case "Bird-Faced Urn": add_power(weight); break;
            case "Calipers": add_barricade_use(weight); break;
            case "Charon's Ashes": add_exhaust(weight); break;
            case "Champion Belt": add_vulnerable(weight); break;
            case "Dead Branch": add_exhaust(weight); break;
            case "Runic Dodecahedron": add_energy_use(weight); break;
            case "Ectoplasm": add_energy_use(weight); break;
            case "Gambling Chip": add_discard_use(weight); break;
            case "Girya": add_strength_use(weight); break;
            case "Ice Cream": add_energy_use(weight); add_energy(weight*WEAK_MULTIPLIER); break;
            case "Kunai": add_cheapAttack(weight); break;
            case "Letter Opener": add_cheapSkill(weight); break;
            case "Magic Flower": add_healing(weight); break;
            case "Mark of Pain": add_wound_use(weight); add_energy_use(weight); break;
            case "Medical Kit": add_wound(weight); break;
            case "Mumified Hand": add_power(weight); break;
            case "Necronomicon": add_bigAttack(weight); break;
            case "Ninja Scroll": add_shiv_use(weight); break;
            case "Oddly Smooth Stone": add_dexterity_use(weight); break;
            case "Ornamental Fan": add_cheapAttack(weight); break;
            case "Paper Krane": add_weak(weight); break;
            case "Paper Phrog": add_vulnerable(weight); break;
            case "Philosopher's Stone": add_energy_use(weight); break;
            case "Red Mask": add_weak_use(weight); break;
            case "Runic Cube": add_selfDamage(weight); break;
            case "Runic Dome": add_energy_use(weight); break;
            case "Runic Pyramid": add_discard(weight); add_draw(weight); break;
            case "Shuriken": add_cheapAttack(weight); break;
            case "Snecko Eye": add_bigCard(weight); break;
            case "Snecko Skull": add_poison(weight); break;
            case "Sozu": add_energy_use(weight); break;
            case "Strange Spoon": add_exhaust(weight); break;
            case "The Specimen": add_poison(weight); break;
            case "Tingsha": add_discard(weight); break;
            case "Tough Bandages": add_discard(weight); break;
            case "Vajra": add_strength_use(weight); break;
            case "Velvet Choker": add_energy_use(weight); break;
        }
        trace = null;
    }

    // Synergy pools
    // These are cards that fall into a particular category.
    // The category X_use are cards that benefit from having X cards in the deck
    void add_exhaust(float weight) {
        add(Disarm.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Feed.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Impervious.ID,weight*SINGLE_USE_MULTIPLIER);
        add(InfernalBlade.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Intimidate.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Offering.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Pummel.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Reaper.ID,weight*SINGLE_USE_MULTIPLIER);
        add(SeeingRed.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Shockwave.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Warcry.ID,weight*SINGLE_USE_MULTIPLIER);
        //
        add(Adrenaline.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Alchemize.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Backstab.ID,weight*SINGLE_USE_MULTIPLIER);
        // Catalyst only with poison
        add(DieDieDie.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Distraction.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Doppelganger.ID,weight*SINGLE_USE_MULTIPLIER);
        add(EndlessAgony.ID,weight); // exhausts multiple times
        add(Malaise.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Nightmare.ID,weight*SINGLE_USE_MULTIPLIER);
        add(PiercingWail.ID,weight*SINGLE_USE_MULTIPLIER);
        add(Terror.ID,weight*SINGLE_USE_MULTIPLIER);
        add_exhaustOther(weight);
    }
    void add_exhaust_use(float weight) {
        weight *= SINGLE_USE_MULTIPLIER; // weak synergy with single use
        add(DarkEmbrace.ID,weight);
        add(Exhume.ID,weight);
        add(FeelNoPain.ID,weight);
    }
    void add_exhaustOther(float weight) {
        add(BurningPact.ID,weight);
        //add(Corruption.ID,weight); // only skills
        add(FiendFire.ID,weight);
        add(Havoc.ID,weight);
        add(Immolate.ID,weight);
        add(SecondWind.ID,weight);
        add(SeverSoul.ID,weight);
        add(TrueGrit.ID,weight);
    }
    void add_exhaustOther_use(float weight) {
        add(Sentinel.ID,weight);
        add_exhaust_use(weight / SINGLE_USE_MULTIPLIER);
    }
    void add_upgrade(float weight) {
        add(Armaments.ID,weight);
    }
    void add_upgrade_use(float weight) {
        add(SearingBlow.ID,weight);
    }
    void add_bigAttack(float weight) {
        add(Bludgeon.ID,weight);
        add(Carnage.ID,weight);
        add(Clothesline.ID,weight);
        add(HeavyBlade.ID,weight);
        add(Uppercut.ID,weight);
        //
        add(Choke.ID,weight);
        add(Dash.ID,weight);
        add(Predator.ID,weight);
        add(RiddleWithHoles.ID,weight);
    }
    void add_xAttack(float weight) {
        add(FiendFire.ID,weight);
        add(Whirlwind.ID,weight);
        add(Skewer.ID,weight);
    }
    void add_copyableAttack(float weight) {
        add_bigAttack(weight);
        add_xAttack(weight);
        add(Rampage.ID,weight);
        add(FlyingKnee.ID,weight);
        //add(Predator.ID,weight); // already in bigAttack
    }
    void add_bigAttack_use(float weight) {
        add(DoubleTap.ID,weight);
    }
    void add_doubleTap_use(float weight) {
        add_bigAttack(weight);
        add_xAttack(weight);
        add(Rampage.ID,weight);
        // honestly, any attack
    }
    void add_bigSkill(float weight) {
        add(Entrench.ID,weight);
        add(FlameBarrier.ID,weight);
        add(Impervious.ID,weight);
        add(Shockwave.ID,weight);
        // green
        add(BouncingFlask.ID,weight);
        add(BulletTime.ID,weight);
        add(CripplingPoison.ID,weight);
        add(LegSweep.ID,weight);
        add(Nightmare.ID,weight);
        add(PhantasmalKiller.ID,weight);
    }
    void add_bigSkill_use(float weight) {
        add(Burst.ID,weight);
        add(Corruption.ID,weight);
    }
    void add_bigCard(float weight) {
        add_bigAttack(weight);
        add_bigSkill(weight);
    }
    void add_bigCard_use(float weight) {
        add(BulletTime.ID,weight);
    }
    void add_vulnerable(float weight) {
        add(Bash.ID,weight);
        add(Uppercut.ID,weight);
        add(Shockwave.ID,weight);
        add(Terror.ID,weight);
    }
    void add_vulnerable_use(float weight) {
        add(Dropkick.ID, weight);
        if (weight > STARTER_WEIGHT) add_attack(weight);
    }
    void add_status(float weight) {
        add_wound(weight);
    }
    void add_status_use(float weight) {
        add(Immolate.ID,weight);
        add_exhaustOther(weight);
    }
    void add_curse_use(float weight) {
        add(Immolate.ID,weight);
    }
    void add_wound(float weight) {
        add(PowerThrough.ID,weight);
        add(WildStrike.ID,weight);
    }
    void add_wound_use(float weight) {
        add(Evolve.ID,weight);
        add_status_use(weight);
    }
    void add_selfDamage(float weight) {
        add(Bloodletting.ID,weight);
        add(Brutality.ID,weight);
        add(Combust.ID,weight);
        add(Hemokinesis.ID,weight);
        add(Offering.ID,weight);
    }
    void add_selfDamage_use(float weight) {
        add(BloodForBlood.ID,weight);
        add(Rupture.ID,weight);
    }
    void add_strength(float weight) {
        add(DemonForm.ID,weight);
        add(Flex.ID,weight);
        add(LimitBreak.ID,weight);
        add(SpotWeakness.ID,weight);
        add(JAX.ID,weight); // never actually in the pool
    }
    void add_strength_use(float weight) {
        add(HeavyBlade.ID,weight);
        add(LimitBreak.ID,weight);
        add(Pummel.ID,weight);
        add(Reaper.ID,weight);
        add(SwordBoomerang.ID,weight);
        add(TwinStrike.ID,weight);
        add(Whirlwind.ID,weight);
        // green
        add(DaggerSpray.ID,weight);
        add(DieDieDie.ID,weight);
        add(GlassKnife.ID,weight);
        add(RiddleWithHoles.ID,weight);
        add(Skewer.ID,weight);
    }
    void add_strike(float weight) {
        add(SwiftStrike.ID,weight);
        add(PerfectedStrike.ID,weight);
        add(PommelStrike.ID,weight);
        add(TwinStrike.ID,weight);
        add(WildStrike.ID,weight);
    }
    void add_poison(float weight) {
        add(BouncingFlask.ID,weight);
        add(CripplingPoison.ID,weight);
        add(DeadlyPoison.ID,weight);
        add(Envenom.ID,weight);
        add(PoisonedStab.ID,weight);
        add(NoxiousFumes.ID,weight);
    }
    void add_poison_use(float weight) {
        add(Bane.ID,weight);
        add(Catalyst.ID,weight);
        add(CorpseExplosion.ID,weight);
        add_poison(weight); // poison synnergizes with more poison
    }
    void add_shiv(float weight) {
        add(BladeDance.ID,weight);
        add(CloakAndDagger.ID,weight);
        add(InfiniteBlades.ID,weight);
        add(StormOfSteel.ID,weight);
    }
    void add_shiv_use(float weight) {
        add(Accuracy.ID,weight);
        add_cheapAttack_use(weight);
    }
    void add_cheapAttack(float weight) {
        // red
        add(Anger.ID,weight);
        add(BodySlam.ID,weight);
        add(Clash.ID,weight);
        add(Dropkick.ID,weight);
        add(RecklessCharge.ID,weight);
        // green
        add(Backstab.ID,weight);
        add(EndlessAgony.ID,weight);
        add(HeelHook.ID,weight);
        add(Neutralize.ID,weight);
        add(MasterfulStab.ID,weight);
        add(Slice.ID,weight);
        add_shiv(weight);
    }
    void add_cheapAttack_use(float weight) {
        add(Rage.ID,weight);
        // green
        add(Envenom.ID,weight);
        add(Finisher.ID,weight);
        add_strength(weight);
        add_cheapCard_use(weight);
    }
    void add_cheapSkill(float weight) {
        add(Havoc.ID,weight);
        add(Intimidate.ID,weight);
        add(Offering.ID,weight);
        //
        add(CalculatedGamble.ID,weight);
        add(Deflect.ID,weight);
        add(EscapePlan.ID,weight);
        add(Prepared.ID,weight);
    }
    void add_cheapSkill_use(float weight) {
        add(WraithForm.ID,weight);
        add_cheapCard_use(weight);
    }
    void add_cheapCard(float weight) {
        add_cheapSkill(weight);
        add_cheapAttack(weight);
    }
    void add_cheapCard_use(float weight) {
        add(AfterImage.ID,weight);
        add(AThousandCuts.ID,weight);
        add(Choke.ID,weight);
        add(Panache.ID,weight);
    }
    void add_weak(float weight) {
        add(Clothesline.ID,weight);
        add(LegSweep.ID,weight);
        add(Neutralize.ID,weight);
        add(Malaise.ID,weight);
    }
    void add_weak_use(float weight) {
        add(HeelHook.ID, weight);
    }
    void add_debuf(float weight) {
        add_weak(weight);
        add_vulnerable(weight);
        add(Disarm.ID,weight);
    }
    void add_debuf_use(float weight) {
        add(SadisticNature.ID,weight);
    }
    void add_discard(float weight) {
        add(Adrenaline.ID,weight);
        add(CalculatedGamble.ID,weight);
        add(Concentrate.ID,weight);
        add(DaggerThrow.ID,weight);
        add(Survivor.ID,weight);
        add(ToolsOfTheTrade.ID,weight);
        add(Unload.ID,weight);
    }
    void add_discard_use(float weight) {
        add(Eviscerate.ID,weight);
        add(MasterfulStab.ID,weight);
        add(Reflex.ID,weight);
        add(Tactician.ID,weight);
        add(UnderhandedStrike.ID,weight);
    }
    void add_dexterity(float weight) {
        add(Footwork.ID,weight);
    }
    void add_dexterity_use(float weight) {
        add(DodgeAndRoll.ID,weight);
        add_block(weight * WEAK_MULTIPLIER);
    }
    void add_block(float weight) {
        add(Entrench.ID,weight);
        add(FlameBarrier.ID,weight);
        add(GhostlyArmor.ID,weight);
        add(Impervious.ID,weight);
        add(Metallicize.ID,weight);
        add(ShrugItOff.ID,weight);
        add(TrueGrit.ID,weight);
        // green
        add(Backflip.ID,weight);
        add(Blur.ID,weight);
        add(CloakAndDagger.ID,weight);
        add(Dash.ID,weight);
        add(Deflect.ID,weight);
        add(EscapePlan.ID,weight);
        add(Survivor.ID,weight);
    }
    void add_block_use(float weight) {
        weight *= WEAK_MULTIPLIER;
        add(Barricade.ID,weight);
        add(BodySlam.ID,weight);
        add(Juggernaut.ID,weight);
        add(Blur.ID,weight);
        add_dexterity(weight);
    }
    void add_barricade_use(float weight) {
        add(BodySlam.ID,weight);
        add_block(weight);
    }
    void add_energy(float weight) {
        weight *= WEAK_MULTIPLIER; // wanting more energy is never a strong synergy
        add(SeeingRed.ID,weight);
        add(Offering.ID,weight);
        add(Adrenaline.ID,weight);
        // concentrate: only if we have discard synergy
        add(Outmaneuver.ID,weight);
        add(FlyingKnee.ID,weight);
    }
    void add_energy_use(float weight) {
        weight *= WEAK_MULTIPLIER; // using more energy is never a strong synergy
        // X cards
        add_xAttack(weight);
        add(Malaise.ID,weight);
    }
    void add_plan(float weight) {
        add(Headbutt.ID,weight);
        add(Setup.ID,weight);
        add_retain(weight);
    }
    void add_retain(float weight) {
        add(WellLaidPlans.ID,weight);
    }
    void add_plan_use(float weight) {
        // ?
    }
    void add_draw(float weight) {
        //add(DarkEmbrace.ID,weight); // only exhaust synergy
        add(BattleTrance.ID,weight);
        add(BurningPact.ID,weight);
        add(Offering.ID,weight);
        add(PommelStrike.ID,weight);
        // green
        add(Acrobatics.ID,weight);
        add(Adrenaline.ID,weight);
        add(Backflip.ID,weight);
        add(Expertise.ID,weight);
    }
    void add_draw_use(float weight) {
        // ?
        add(GrandFinale.ID,weight); // ?
    }
    void add_skill_use(float weight) {
        add(Burst.ID,weight);
    }
    void add_attack_use(float weight) {
        add(DoubleTap.ID,weight);
    }
    void add_skill(float weight) {
        add_of_type(AbstractCard.CardType.SKILL, weight*ANY_SKILL_MULTIPLIER);
    }
    void add_attack(float weight) {
        add_of_type(AbstractCard.CardType.ATTACK, weight*ANY_ATTACK_MULTIPLIER);
    }
    void add_power(float weight) {
        add_of_type(AbstractCard.CardType.POWER, weight*ANY_POWER_MULTIPLIER);
    }
    void add_of_type(AbstractCard.CardType type, float weight) {
        // any skill/attack/power? with a much lower weight
        for (SynergyEntry entry : pool.values()) {
            if (entry.card.type == type) {
                add(entry,weight);
            }
        }
    }
    void add_power_use(float weight) {
    }
    void add_healing(float weight) {
        add(Reaper.ID,weight);
    }
}
