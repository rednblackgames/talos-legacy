package games.rednblack.talos.runtime;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import games.rednblack.talos.runtime.modules.EmitterModule;
import games.rednblack.talos.runtime.modules.ParticleModule;


public interface IEmitter {

    void init();

    float getAlpha();
    ParticleModule getParticleModule();
    EmitterModule getEmitterModule();
    Vector2 getEffectPosition();
    ScopePayload getScope();
    Color getTint();

    void setScope(ScopePayload scope);
    int getActiveParticleCount();
    boolean isContinuous();
    boolean isComplete();
    boolean isStopped();
    void stop();
    void pause();
    void resume();
    void restart();
    void reset();
    float getDelayRemaining();
    void update(float delta);
    ParticleEmitterDescriptor getEmitterGraph();
    void setVisible(boolean isVisible);
    boolean isVisible();
    boolean isAdditive();
    boolean isBlendAdd();
    Array<Particle> getActiveParticles();
}
