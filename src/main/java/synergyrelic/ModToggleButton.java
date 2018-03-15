package synergyrelic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.InputHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import basemod.ModButton;
import basemod.ModPanel;

// Note: extending ModButton is a minor hack
public class ModToggleButton extends ModButton {
    public boolean enabled;
    private Hitbox hb;
    private Consumer<ModToggleButton> onToggle;
    private float x;
    private float y;
	public static final Logger logger = LogManager.getLogger(ModToggleButton.class.getName());
    
    public ModToggleButton(float xPos, float yPos, ModPanel p, boolean enabled, Consumer<ModToggleButton> c) {
        super(xPos, yPos, ImageMaster.OPTION_TOGGLE, p, (me)->{});
        this.x = xPos * Settings.scale;
        this.y = yPos * Settings.scale;
        //this.hb = new Hitbox(200.0f * Settings.scale, 32.0f * Settings.scale);
        //this.hb.move(x, y);
        this.hb = new Hitbox(x, y-8, 200.0f, 32.0f);
        this.onToggle = c;
        this.enabled = enabled;
    }

    @Override
    public void update() { 
        this.hb.update();
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            logger.info("Update mod button " + this.hb.hovered +" "+ InputHelper.justClickedLeft);
            InputHelper.justClickedLeft = false;
            this.toggle();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        //logger.info("Render mod button" + this.x + " " + this.y);
        if (this.hb.hovered) {
            sb.setColor(Color.CYAN);
        } else if (this.enabled) {
            sb.setColor(Color.LIGHT_GRAY);
        } else {
            sb.setColor(Color.WHITE);
        }
        sb.draw(ImageMaster.OPTION_TOGGLE, x, y, 32*Settings.scale, 32*Settings.scale);
        if (this.enabled) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.OPTION_TOGGLE_ON, x, y, 32*Settings.scale, 32*Settings.scale);
        }
        this.hb.render(sb);
    }

    public void toggle() {
        logger.info("Toggle button");
        enabled = !enabled;
        onToggle.accept(this);
    }
}
