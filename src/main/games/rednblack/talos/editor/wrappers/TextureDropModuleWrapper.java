/*******************************************************************************
 * Copyright 2019 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package games.rednblack.talos.editor.wrappers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import games.rednblack.talos.TalosMain;
import games.rednblack.talos.editor.project.FileTracker;
import games.rednblack.talos.editor.assets.TalosAssetProvider;
import games.rednblack.talos.editor.dialogs.SettingsDialog;
import games.rednblack.talos.editor.widgets.TextureDropWidget;
import games.rednblack.talos.runtime.modules.AbstractModule;

import java.io.File;

public abstract class TextureDropModuleWrapper<T extends AbstractModule> extends ModuleWrapper<T> {

    protected TextureDropWidget<AbstractModule> dropWidget;
    protected TextureRegion defaultRegion;

    protected String regionName = "fire";
    protected String filePath = null;

    @Override
    protected void configureSlots() {
        final TalosAssetProvider projectAssetProvider = TalosMain.Instance().TalosProject().getProjectAssetProvider();
        defaultRegion = projectAssetProvider.findAsset("fire", TextureRegion.class);
        dropWidget = new TextureDropWidget<AbstractModule>(defaultRegion, getSkin());
    }

    public abstract void setModuleRegion(String name, Sprite region);

    @Override
    public void fileDrop(String[] paths, float x, float y) {
        if(paths.length == 1) {

            String resourcePath = paths[0];
            FileHandle fileHandle = Gdx.files.absolute(resourcePath);

            final String extension = fileHandle.extension();

            if (extension.endsWith("png") || extension.endsWith("jpg")) {
                final Texture texture = new Texture(fileHandle);
                final Sprite region = new Sprite(texture);
                TalosMain.Instance().TalosProject().getProjectAssetProvider().addToAtlas(fileHandle.nameWithoutExtension(), region);
                setModuleRegion(fileHandle.nameWithoutExtension(), region);
                dropWidget.setDrawable(new TextureRegionDrawable(region));

                regionName = fileHandle.nameWithoutExtension();
                filePath = fileHandle.path();

                TalosMain.Instance().ProjectController().setDirty();

                TalosMain.Instance().FileTracker().trackFile(fileHandle, handle -> {

                    try {
                        final TalosAssetProvider projectAssetProvider = TalosMain.Instance().TalosProject().getProjectAssetProvider();
                        Sprite region1 = projectAssetProvider.replaceRegion(handle);

                        setModuleRegion(handle.nameWithoutExtension(), region1);
                        dropWidget.setDrawable(new TextureRegionDrawable(region1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);

        filePath = jsonData.getString("filePath", null);
        regionName = jsonData.getString("regionName", null);

        // hack for older version to patch broken files (we should do version transition logic and move it there later)
        if(jsonData.has("fileName")) {
            filePath = jsonData.getString("fileName");
            regionName = filePath;
            if(filePath.contains(".")) {
                regionName =  regionName.substring(0, regionName.lastIndexOf("."));
            } else {
                filePath = filePath + ".png";
            }
        }
    
        final TalosAssetProvider assetProvider = TalosMain.Instance().TalosProject().getProjectAssetProvider();
        final Sprite textureRegion = assetProvider.findAsset(regionName, Sprite.class);

        if (textureRegion != null) {
            setModuleRegion(regionName, textureRegion);
            dropWidget.setDrawable(new TextureRegionDrawable(textureRegion));
        } else {
            //TalosMain.Instance().errorReporting.reportException(new GdxRuntimeException("Texture is missing on loading in all paths: " + regionName));
        }
    }

    @Override
    public void draw (Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
    }

    @Override
    public void write (Json json) {
        super.write(json);
        json.writeValue("filePath", filePath);
        json.writeValue("regionName", regionName);
    }

    private FileHandle tryAndFindTexture(String path) {
        FileHandle fileHandle = Gdx.files.absolute(path);
        String fileName = fileHandle.name();
        if(!fileHandle.exists()) {
            if(TalosMain.Instance().ProjectController().getCurrentProjectPath() != null) {
                FileHandle parent = Gdx.files.absolute(TalosMain.Instance().ProjectController().getCurrentProjectPath()).parent();
                fileHandle = Gdx.files.absolute(parent.path() + "/" + fileName);
            }

            if(!fileHandle.exists()) {
                fileHandle = Gdx.files.absolute(TalosMain.Instance().Prefs().getString(SettingsDialog.ASSET_PATH) + File.separator + fileName);
            }
        }

        return fileHandle;
    }

    public void setTexture(String path) {
        FileHandle fileHandle = tryAndFindTexture(path);
        if(fileHandle.exists()) {
            Sprite region = new Sprite(new Texture(fileHandle));
            setModuleRegion(fileHandle.nameWithoutExtension(), region);
            dropWidget.setDrawable(new TextureRegionDrawable(region));
        }
        filePath = fileHandle.path();
        regionName = fileHandle.nameWithoutExtension();
    }
}
