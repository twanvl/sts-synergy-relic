package synergyrelic.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BonsaiTree extends CustomRelic {
    public static final String ID = "Bonsai Tree";
    private static final String IMG = "img/relics/BonsaiTree.png";
    private static final String OUTLINE = "img/relics/outline/BonsaiTree.png";

    public BonsaiTree() {
        super(ID, new Texture(Gdx.files.internal(IMG)), new Texture(Gdx.files.internal(OUTLINE)), RelicTier.BOSS, LandingSound.MAGICAL);
    }
    
    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }
    
    @Override
    public AbstractRelic makeCopy() {
        return new BonsaiTree();
    }
}
