package games.rednblack.talos.runtime.modules;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.talos.runtime.ParticleDrawable;
import games.rednblack.talos.runtime.ParticleEmitterDescriptor;
import games.rednblack.talos.runtime.assets.AssetProvider;
import games.rednblack.talos.runtime.render.drawables.ShadedDrawable;
import games.rednblack.talos.runtime.utils.ShaderDescriptor;
import games.rednblack.talos.runtime.values.DrawableValue;

public class ShadedSpriteModule extends AbstractModule {

    public static final int OUTPUT = 0;

    public ShaderDescriptor shaderDescriptor;

    private DrawableValue outputValue;
    public String shdrFileName;

    public ObjectMap<String, TextureRegion> textureMap = new ObjectMap<>();

    @Override
    protected void defineSlots () {
        outputValue = (DrawableValue) createOutputSlot(OUTPUT, new DrawableValue());

        outputValue.setDrawable(new ShadedDrawable());
    }

    @Override
    public void processValues () {
        ShadedDrawable drawable = (ShadedDrawable) outputValue.getDrawable();
    }

    @Override
    public void setModuleGraph(ParticleEmitterDescriptor graph) {
        super.setModuleGraph(graph);
        final AssetProvider assetProvider = graph.getEffectDescriptor().getAssetProvider();

        if(shdrFileName != null && !shdrFileName.isEmpty()) {
            setShaderData(assetProvider.findAsset(shdrFileName, ShaderDescriptor.class), shdrFileName);
        }

        updateShader(shaderDescriptor);
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("shdrAssetName", shdrFileName);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        shdrFileName = jsonData.getString("shdrAssetName", "");
    }

    public void setShaderData(ShaderDescriptor shaderDescriptor, String fileName) {
        shdrFileName = fileName;
        updateShader(shaderDescriptor);
    }

    private void updateShader(ShaderDescriptor shaderDescriptor) {
        this.shaderDescriptor = shaderDescriptor;

        if(shaderDescriptor != null) {
            ShadedDrawable drawable = (ShadedDrawable) outputValue.getDrawable();
            drawable.setShader(shaderDescriptor.getFragCode());

            for (String uniformName : shaderDescriptor.getUniformMap().keys()) {
                ShaderDescriptor.UniformData data = shaderDescriptor.getUniformMap().get(uniformName);

                if (data.type == ShaderDescriptor.Type.TEXTURE) {
                    TextureRegion textureRegion = graph.getEffectDescriptor().getAssetProvider().findAsset(data.payload, TextureRegion.class);
                    textureMap.put(uniformName, textureRegion);
                }
            }

            drawable.setTextures(textureMap);
        }
    }

    public ShaderDescriptor getShaderDescriptor() {
        return shaderDescriptor;
    }
}
