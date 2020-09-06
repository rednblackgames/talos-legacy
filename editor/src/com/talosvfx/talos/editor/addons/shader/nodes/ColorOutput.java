package com.talosvfx.talos.editor.addons.shader.nodes;

import com.badlogic.gdx.utils.Align;
import com.talosvfx.talos.editor.addons.shader.ShaderBuilder;
import com.talosvfx.talos.editor.addons.shader.widgets.ShaderBox;
import com.talosvfx.talos.editor.nodes.NodeWidget;

public class ColorOutput extends AbstractShaderNode {

    public final String INPUT_RGBA = "inputColor";

    public void prepareDeclarations (ShaderBuilder shaderBuilder) {

    }

    public void buildFragmentShader(ShaderBuilder shaderBuilder) {
        shaderBuilder.reset();

        resetProcessingTree();
        processTree(shaderBuilder);

        String color = getExpression(INPUT_RGBA);

        if(color == null) {
            color = "vec4(0.0, 0.0, 0.0, 1.0)";
        }

        shaderBuilder.addLine("gl_FragColor = " + color + "");
    }

    @Override
    public void graphUpdated () {
        buildFragmentShader(previewBuilder);
        super.graphUpdated();
    }


    @Override
    protected void updatePreview () {
        shaderBox.setShader(previewBuilder);
    }

    @Override
    protected String getPreviewOutputName () {
        return null;
    }

    @Override
    public String writeOutputCode(String slotId) {
        return null;
    }
}