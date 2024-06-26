package games.rednblack.talos.editor.addons.shader.nodes;

import games.rednblack.talos.runtime.shaders.ShaderBuilder;

public class NumberNode extends AbstractShaderNode {

    public final String OUTPUT = "outputValue";

    @Override
    public void prepareDeclarations (ShaderBuilder shaderBuilder) {

    }

    @Override
    public String writeOutputCode (String slotId) {
        return getExpression(OUTPUT, null);
    }
}
