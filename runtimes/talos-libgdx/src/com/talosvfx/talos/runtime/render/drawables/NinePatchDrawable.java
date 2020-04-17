package com.talosvfx.talos.runtime.render.drawables;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.talosvfx.talos.runtime.Particle;
import com.talosvfx.talos.runtime.ParticleDrawable;

public class NinePatchDrawable implements ParticleDrawable {

    TextureRegion region;

    NinePatch ninePatch;

    public NinePatchDrawable() {

    }

    @Override
    public void draw (Batch batch, Particle particle, Color color) {
        float rotation = particle.rotation;
        float width = particle.size.x;
        float height = particle.size.y;
        float y = particle.getY();
        float x = particle.getX();

        if(region == null) return;
        if(ninePatch != null) {
            ninePatch.setColor(color);
            draw(batch, x, y, width, height, rotation);

        }
    }

    @Override
    public void draw (Batch batch, float x, float y, float width, float height, float rotation) {
        ninePatch.draw(batch, x-width/2f, y-height/2f, width/2f, height/2f, width, height, 1f, 1f, rotation);
    }

    @Override
    public float getAspectRatio () {
        return 1;
    }

    @Override
    public void setCurrentParticle (Particle particle) {

    }

    @Override
    public TextureRegion getTextureRegion () {
        return null;
    }

    public void setRegion (TextureRegion region, int[] splits) {
        if(this.region != region) {
            ninePatch =  new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
        }
        this.region = region;
    }
}