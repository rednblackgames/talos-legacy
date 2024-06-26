package games.rednblack.talos.editor.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ClippedNinePatch {
    public static final int TOP_LEFT = 0;
    public static final int TOP_CENTER = 1;
    public static final int TOP_RIGHT = 2;
    public static final int MIDDLE_LEFT = 3;
    public static final int MIDDLE_CENTER = 4;
    public static final int MIDDLE_RIGHT = 5;
    public static final int BOTTOM_LEFT = 6;

    public static final int BOTTOM_CENTER = 7;
    public static final int BOTTOM_RIGHT = 8;

    private static final Color tmpDrawColor = new Color();

    private Texture texture;
    private int bottomLeft = -1, bottomCenter = -1, bottomRight = -1;
    private int middleLeft = -1, middleCenter = -1, middleRight = -1;
    private int topLeft = -1, topCenter = -1, topRight = -1;
    private float leftWidth, rightWidth, middleWidth, middleHeight, topHeight, bottomHeight;
    private float[] vertices = new float[9 * 4 * 5];
    private int idx;
    private final Color color = new Color(Color.WHITE);
    private float padLeft = -1, padRight = -1, padTop = -1, padBottom = -1;
    private TextureRegion[] patches;
    private float maskScaleX = 1;
    private float maskScaleY = 1;

    /** Create a ninepatch by cutting up the given texture into nine patches. The subsequent parameters define the 4 lines that
     * will cut the texture region into 9 pieces.
     *
     * @param left Pixels from left edge.
     * @param right Pixels from right edge.
     * @param top Pixels from top edge.
     * @param bottom Pixels from bottom edge. */
    public ClippedNinePatch (Texture texture, int left, int right, int top, int bottom) {
        this(new TextureRegion(texture), left, right, top, bottom);
    }

    /** Create a ninepatch by cutting up the given texture region into nine patches. The subsequent parameters define the 4 lines
     * that will cut the texture region into 9 pieces.
     *
     * @param left Pixels from left edge.
     * @param right Pixels from right edge.
     * @param top Pixels from top edge.
     * @param bottom Pixels from bottom edge. */
    public ClippedNinePatch (TextureRegion region, int left, int right, int top, int bottom) {
        if (region == null) throw new IllegalArgumentException("region cannot be null.");
        final int middleWidth = region.getRegionWidth() - left - right;
        final int middleHeight = region.getRegionHeight() - top - bottom;

        patches = new TextureRegion[9];
        if (top > 0) {
            if (left > 0) patches[TOP_LEFT] = new TextureRegion(region, 0, 0, left, top);
            if (middleWidth > 0) patches[TOP_CENTER] = new TextureRegion(region, left, 0, middleWidth, top);
            if (right > 0) patches[TOP_RIGHT] = new TextureRegion(region, left + middleWidth, 0, right, top);
        }
        if (middleHeight > 0) {
            if (left > 0) patches[MIDDLE_LEFT] = new TextureRegion(region, 0, top, left, middleHeight);
            if (middleWidth > 0) patches[MIDDLE_CENTER] = new TextureRegion(region, left, top, middleWidth, middleHeight);
            if (right > 0) patches[MIDDLE_RIGHT] = new TextureRegion(region, left + middleWidth, top, right, middleHeight);
        }
        if (bottom > 0) {
            if (left > 0) patches[BOTTOM_LEFT] = new TextureRegion(region, 0, top + middleHeight, left, bottom);
            if (middleWidth > 0) patches[BOTTOM_CENTER] = new TextureRegion(region, left, top + middleHeight, middleWidth, bottom);
            if (right > 0) patches[BOTTOM_RIGHT] = new TextureRegion(region, left + middleWidth, top + middleHeight, right, bottom);
        }

        // If split only vertical, move splits from right to center.
        if (left == 0 && middleWidth == 0) {
            patches[TOP_CENTER] = patches[TOP_RIGHT];
            patches[MIDDLE_CENTER] = patches[MIDDLE_RIGHT];
            patches[BOTTOM_CENTER] = patches[BOTTOM_RIGHT];
            patches[TOP_RIGHT] = null;
            patches[MIDDLE_RIGHT] = null;
            patches[BOTTOM_RIGHT] = null;
        }
        // If split only horizontal, move splits from bottom to center.
        if (top == 0 && middleHeight == 0) {
            patches[MIDDLE_LEFT] = patches[BOTTOM_LEFT];
            patches[MIDDLE_CENTER] = patches[BOTTOM_CENTER];
            patches[MIDDLE_RIGHT] = patches[BOTTOM_RIGHT];
            patches[BOTTOM_LEFT] = null;
            patches[BOTTOM_CENTER] = null;
            patches[BOTTOM_RIGHT] = null;
        }

        load(patches);
    }

    /** Construct a degenerate "nine" patch with only a center component. */
    public ClippedNinePatch (Texture texture, Color color) {
        this(texture);
        setColor(color);
    }

    /** Construct a degenerate "nine" patch with only a center component. */
    public ClippedNinePatch (Texture texture) {
        this(new TextureRegion(texture));
    }

    /** Construct a degenerate "nine" patch with only a center component. */
    public ClippedNinePatch (TextureRegion region, Color color) {
        this(region);
        setColor(color);
    }

    /** Construct a degenerate "nine" patch with only a center component. */
    public ClippedNinePatch (TextureRegion region) {
        load(new TextureRegion[] {
                //
                null, null, null, //
                null, region, null, //
                null, null, null //
        });
    }

    /** Construct a nine patch from the given nine texture regions. The provided patches must be consistently sized (e.g., any left
     * edge textures must have the same width, etc). Patches may be <code>null</code>. Patch indices are specified via the public
     * members {@link #TOP_LEFT}, {@link #TOP_CENTER}, etc. */
    public ClippedNinePatch (TextureRegion... patches) {
        if (patches == null || patches.length != 9) throw new IllegalArgumentException("NinePatch needs nine TextureRegions");

        load(patches);

        float leftWidth = getLeftWidth();
        if ((patches[TOP_LEFT] != null && patches[TOP_LEFT].getRegionWidth() != leftWidth)
                || (patches[MIDDLE_LEFT] != null && patches[MIDDLE_LEFT].getRegionWidth() != leftWidth)
                || (patches[BOTTOM_LEFT] != null && patches[BOTTOM_LEFT].getRegionWidth() != leftWidth)) {
            throw new GdxRuntimeException("Left side patches must have the same width");
        }

        float rightWidth = getRightWidth();
        if ((patches[TOP_RIGHT] != null && patches[TOP_RIGHT].getRegionWidth() != rightWidth)
                || (patches[MIDDLE_RIGHT] != null && patches[MIDDLE_RIGHT].getRegionWidth() != rightWidth)
                || (patches[BOTTOM_RIGHT] != null && patches[BOTTOM_RIGHT].getRegionWidth() != rightWidth)) {
            throw new GdxRuntimeException("Right side patches must have the same width");
        }

        float bottomHeight = getBottomHeight();
        if ((patches[BOTTOM_LEFT] != null && patches[BOTTOM_LEFT].getRegionHeight() != bottomHeight)
                || (patches[BOTTOM_CENTER] != null && patches[BOTTOM_CENTER].getRegionHeight() != bottomHeight)
                || (patches[BOTTOM_RIGHT] != null && patches[BOTTOM_RIGHT].getRegionHeight() != bottomHeight)) {
            throw new GdxRuntimeException("Bottom side patches must have the same height");
        }

        float topHeight = getTopHeight();
        if ((patches[TOP_LEFT] != null && patches[TOP_LEFT].getRegionHeight() != topHeight)
                || (patches[TOP_CENTER] != null && patches[TOP_CENTER].getRegionHeight() != topHeight)
                || (patches[TOP_RIGHT] != null && patches[TOP_RIGHT].getRegionHeight() != topHeight)) {
            throw new GdxRuntimeException("Top side patches must have the same height");
        }
    }

    public ClippedNinePatch (ClippedNinePatch ninePatch) {
        this(ninePatch, ninePatch.color);
    }

    public ClippedNinePatch (ClippedNinePatch ninePatch, Color color) {
        texture = ninePatch.texture;

        bottomLeft = ninePatch.bottomLeft;
        bottomCenter = ninePatch.bottomCenter;
        bottomRight = ninePatch.bottomRight;
        middleLeft = ninePatch.middleLeft;
        middleCenter = ninePatch.middleCenter;
        middleRight = ninePatch.middleRight;
        topLeft = ninePatch.topLeft;
        topCenter = ninePatch.topCenter;
        topRight = ninePatch.topRight;

        leftWidth = ninePatch.leftWidth;
        rightWidth = ninePatch.rightWidth;
        middleWidth = ninePatch.middleWidth;
        middleHeight = ninePatch.middleHeight;
        topHeight = ninePatch.topHeight;
        bottomHeight = ninePatch.bottomHeight;

        padLeft = ninePatch.padLeft;
        padTop = ninePatch.padTop;
        padBottom = ninePatch.padBottom;
        padRight = ninePatch.padRight;

        vertices = new float[ninePatch.vertices.length];
        System.arraycopy(ninePatch.vertices, 0, vertices, 0, ninePatch.vertices.length);
        idx = ninePatch.idx;
        this.color.set(color);
    }

    private void load (TextureRegion[] patches) {
        idx = 0;
        final float color = Color.WHITE.toFloatBits(); // placeholder color, overwritten at draw time

        if (patches[BOTTOM_LEFT] != null) {
            bottomLeft = add(patches[BOTTOM_LEFT], color, false, false);
            leftWidth = patches[BOTTOM_LEFT].getRegionWidth();
            bottomHeight = patches[BOTTOM_LEFT].getRegionHeight();
        }
        if (patches[BOTTOM_CENTER] != null) {
            bottomCenter = add(patches[BOTTOM_CENTER], color, true, false);
            middleWidth = Math.max(middleWidth, patches[BOTTOM_CENTER].getRegionWidth());
            bottomHeight = Math.max(bottomHeight, patches[BOTTOM_CENTER].getRegionHeight());
        }
        if (patches[BOTTOM_RIGHT] != null) {
            bottomRight = add(patches[BOTTOM_RIGHT], color, false, false);
            rightWidth = Math.max(rightWidth, patches[BOTTOM_RIGHT].getRegionWidth());
            bottomHeight = Math.max(bottomHeight, patches[BOTTOM_RIGHT].getRegionHeight());
        }
        if (patches[MIDDLE_LEFT] != null) {
            middleLeft = add(patches[MIDDLE_LEFT], color, false, true);
            leftWidth = Math.max(leftWidth, patches[MIDDLE_LEFT].getRegionWidth());
            middleHeight = Math.max(middleHeight, patches[MIDDLE_LEFT].getRegionHeight());
        }
        if (patches[MIDDLE_CENTER] != null) {
            middleCenter = add(patches[MIDDLE_CENTER], color, true, true);
            middleWidth = Math.max(middleWidth, patches[MIDDLE_CENTER].getRegionWidth());
            middleHeight = Math.max(middleHeight, patches[MIDDLE_CENTER].getRegionHeight());
        }
        if (patches[MIDDLE_RIGHT] != null) {
            middleRight = add(patches[MIDDLE_RIGHT], color, false, true);
            rightWidth = Math.max(rightWidth, patches[MIDDLE_RIGHT].getRegionWidth());
            middleHeight = Math.max(middleHeight, patches[MIDDLE_RIGHT].getRegionHeight());
        }
        if (patches[TOP_LEFT] != null) {
            topLeft = add(patches[TOP_LEFT], color, false, false);
            leftWidth = Math.max(leftWidth, patches[TOP_LEFT].getRegionWidth());
            topHeight = Math.max(topHeight, patches[TOP_LEFT].getRegionHeight());
        }
        if (patches[TOP_CENTER] != null) {
            topCenter = add(patches[TOP_CENTER], color, true, false);
            middleWidth = Math.max(middleWidth, patches[TOP_CENTER].getRegionWidth());
            topHeight = Math.max(topHeight, patches[TOP_CENTER].getRegionHeight());
        }
        if (patches[TOP_RIGHT] != null) {
            topRight = add(patches[TOP_RIGHT], color, false, false);
            rightWidth = Math.max(rightWidth, patches[TOP_RIGHT].getRegionWidth());
            topHeight = Math.max(topHeight, patches[TOP_RIGHT].getRegionHeight());
        }
        if (idx < vertices.length) {
            float[] newVertices = new float[idx];
            System.arraycopy(vertices, 0, newVertices, 0, idx);
            vertices = newVertices;
        }
    }

    private int add (TextureRegion region, float color, boolean isStretchW, boolean isStretchH) {
        if (texture == null)
            texture = region.getTexture();
        else if (texture != region.getTexture()) //
            throw new IllegalArgumentException("All regions must be from the same texture.");

        float u = region.getU();
        float v = region.getV2();
        float u2 = region.getU2();
        float v2 = region.getV();

        // Add half pixel offsets on stretchable dimensions to avoid color bleeding when GL_LINEAR
        // filtering is used for the texture. This nudges the texture coordinate to the center
        // of the texel where the neighboring pixel has 0% contribution in linear blending mode.
        if (texture.getMagFilter() == Texture.TextureFilter.Linear || texture.getMinFilter() == Texture.TextureFilter.Linear) {
            if (isStretchW) {
                float halfTexelWidth = 0.5f * 1.0f / texture.getWidth();
                u += halfTexelWidth;
                u2 -= halfTexelWidth;
            }
            if (isStretchH) {
                float halfTexelHeight = 0.5f * 1.0f / texture.getHeight();
                v -= halfTexelHeight;
                v2 += halfTexelHeight;
            }
        }

        final float[] vertices = this.vertices;

        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;

        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;

        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;

        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        idx += 20;

        return idx - 20;
    }

    /** Set the coordinates and color of a ninth of the patch. */
    private void set (int idx, float x, float y, float width, float height, float color, float uScale, float vScale) {
        final float fx2 = x + width;
        final float fy2 = y + height;
        final float[] vertices = this.vertices;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;

        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;

        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;

        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;

        float u = vertices[idx + 3];
        float v = vertices[idx + 4];
        float u2 = vertices[idx + 13];
        float v2 = vertices[idx + 9];
        float uWidth = u2 - u;
        float vHeight = v2 - v;
        float newUWidth = uWidth * uScale;
        float newVHeight = vHeight * vScale;
        float newU2 = u + newUWidth;
        float newV2 = v + newVHeight;

        vertices[idx + 9] = newV2;
        vertices[idx + 13] = newU2;
        vertices[idx + 14] = newV2;
        vertices[idx + 18] = newU2;
    }

    private void prepareVertices (Batch batch, float x, float y, float width, float height) {
        final float centerColumnX = x + leftWidth;
        final float rightColumnX = x + width - rightWidth;
        final float middleRowY = y + bottomHeight;
        final float topRowY = y + height - topHeight;
        final float c = tmpDrawColor.set(color).mul(batch.getColor()).toFloatBits();

        float limitX = width * maskScaleX;
        float limitY = height * maskScaleY;

        // verticals
        float bottomScaleY = MathUtils.clamp(limitY / (middleRowY - y), 0, 1);
        float bottomResultHeight = (middleRowY - y) * bottomScaleY;
        float middleScaleY = MathUtils.clamp((y + limitY - middleRowY) / (topRowY - middleRowY), 0, 1);
        float middleResultHeight = (topRowY - middleRowY) * middleScaleY;
        float topScaleY = MathUtils.clamp((y + limitY - topRowY) / (y + height - topRowY), 0, 1);
        float topResultHeight = (y + height - topRowY) * topScaleY;

        // horizontals
        float leftScaleX = MathUtils.clamp(limitX / (centerColumnX - x), 0, 1);
        float leftResultWidth = (centerColumnX - x) * leftScaleX;
        float centerScaleX = MathUtils.clamp((x + limitX - centerColumnX) / (rightColumnX - centerColumnX), 0, 1);
        float centerResultWidth = (rightColumnX - centerColumnX) * centerScaleX;
        float rightScaleX = MathUtils.clamp((x + limitX - rightColumnX) / (x + width - rightColumnX), 0, 1);
        float rightResultWidth = (x + width - rightColumnX) * rightScaleX;

        if (bottomLeft != -1) set(bottomLeft, x, y, leftResultWidth, bottomResultHeight, c, leftScaleX,bottomScaleY);
        if (bottomCenter != -1) set(bottomCenter, centerColumnX, y, centerResultWidth, bottomResultHeight, c, centerScaleX,bottomScaleY);
        if (bottomRight != -1) set(bottomRight, rightColumnX, y, rightResultWidth, bottomResultHeight, c, rightScaleX,bottomScaleY);


        if (middleLeft != -1) set(middleLeft, x, middleRowY, leftResultWidth, middleResultHeight, c, leftScaleX,middleScaleY);
        if (middleCenter != -1) set(middleCenter, centerColumnX, middleRowY, centerResultWidth, middleResultHeight, c, centerScaleX,middleScaleY);
        if (middleRight != -1) set(middleRight, rightColumnX, middleRowY, rightResultWidth, middleResultHeight, c, rightScaleX,middleScaleY);

        if (topLeft != -1) set(topLeft, x, topRowY, leftResultWidth, topResultHeight, c, leftScaleX,topScaleY);
        if (topCenter != -1) set(topCenter, centerColumnX, topRowY, centerResultWidth, topResultHeight, c, centerScaleX,topScaleY);
        if (topRight != -1) set(topRight, rightColumnX, topRowY, rightResultWidth, topResultHeight, c, rightScaleX,topScaleY);
    }
    public void draw (Batch batch, float x, float y, float width, float height) {
        load(patches);
        prepareVertices(batch, x, y, width, height);
        batch.draw(texture, vertices, 0, idx);
    }

    public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
                      float scaleY, float rotation) {
        prepareVertices(batch, x, y, width, height);
        float worldOriginX = x + originX, worldOriginY = y + originY;
        int n = this.idx;
        float[] vertices = this.vertices;
        if (rotation != 0) {
            for (int i = 0; i < n; i += 5) {
                float vx = (vertices[i] - worldOriginX) * scaleX, vy = (vertices[i + 1] - worldOriginY) * scaleY;
                float cos = MathUtils.cosDeg(rotation), sin = MathUtils.sinDeg(rotation);
                vertices[i] = cos * vx - sin * vy + worldOriginX;
                vertices[i + 1] = sin * vx + cos * vy + worldOriginY;
            }
        } else if (scaleX != 1 || scaleY != 1) {
            for (int i = 0; i < n; i += 5) {
                vertices[i] = (vertices[i] - worldOriginX) * scaleX + worldOriginX;
                vertices[i + 1] = (vertices[i + 1] - worldOriginY) * scaleY + worldOriginY;
            }
        }
        batch.draw(texture, vertices, 0, n);
    }

    /** Copy given color. The color will be blended with the batch color, then combined with the texture colors at
     * {@link com.badlogic.gdx.graphics.g2d.NinePatch#draw(Batch, float, float, float, float) draw} time. Default is {@link Color#WHITE}. */
    public void setColor (Color color) {
        this.color.set(color);
    }

    public Color getColor () {
        return color;
    }

    public float getLeftWidth () {
        return leftWidth;
    }

    /** Set the draw-time width of the three left edge patches */
    public void setLeftWidth (float leftWidth) {
        this.leftWidth = leftWidth;
    }

    public float getRightWidth () {
        return rightWidth;
    }

    /** Set the draw-time width of the three right edge patches */
    public void setRightWidth (float rightWidth) {
        this.rightWidth = rightWidth;
    }

    public float getTopHeight () {
        return topHeight;
    }

    /** Set the draw-time height of the three top edge patches */
    public void setTopHeight (float topHeight) {
        this.topHeight = topHeight;
    }

    public float getBottomHeight () {
        return bottomHeight;
    }

    /** Set the draw-time height of the three bottom edge patches */
    public void setBottomHeight (float bottomHeight) {
        this.bottomHeight = bottomHeight;
    }

    public float getMiddleWidth () {
        return middleWidth;
    }

    /** Set the width of the middle column of the patch. At render time, this is implicitly the requested render-width of the
     * entire nine patch, minus the left and right width. This value is only used for computing the {@link #getTotalWidth() default
     * total width}. */
    public void setMiddleWidth (float middleWidth) {
        this.middleWidth = middleWidth;
    }

    public float getMiddleHeight () {
        return middleHeight;
    }

    /** Set the height of the middle row of the patch. At render time, this is implicitly the requested render-height of the entire
     * nine patch, minus the top and bottom height. This value is only used for computing the {@link #getTotalHeight() default
     * total height}. */
    public void setMiddleHeight (float middleHeight) {
        this.middleHeight = middleHeight;
    }

    public float getTotalWidth () {
        return leftWidth + middleWidth + rightWidth;
    }

    public float getTotalHeight () {
        return topHeight + middleHeight + bottomHeight;
    }

    /** Set the padding for content inside this ninepatch. By default the padding is set to match the exterior of the ninepatch, so
     * the content should fit exactly within the middle patch. */
    public void setPadding (float left, float right, float top, float bottom) {
        this.padLeft = left;
        this.padRight = right;
        this.padTop = top;
        this.padBottom = bottom;
    }

    /** Returns the left padding if set, else returns {@link #getLeftWidth()}. */
    public float getPadLeft () {
        if (padLeft == -1) return getLeftWidth();
        return padLeft;
    }

    /** See {@link #setPadding(float, float, float, float)} */
    public void setPadLeft (float left) {
        this.padLeft = left;
    }

    /** Returns the right padding if set, else returns {@link #getRightWidth()}. */
    public float getPadRight () {
        if (padRight == -1) return getRightWidth();
        return padRight;
    }

    /** See {@link #setPadding(float, float, float, float)} */
    public void setPadRight (float right) {
        this.padRight = right;
    }

    /** Returns the top padding if set, else returns {@link #getTopHeight()}. */
    public float getPadTop () {
        if (padTop == -1) return getTopHeight();
        return padTop;
    }

    /** See {@link #setPadding(float, float, float, float)} */
    public void setPadTop (float top) {
        this.padTop = top;
    }

    /** Returns the bottom padding if set, else returns {@link #getBottomHeight()}. */
    public float getPadBottom () {
        if (padBottom == -1) return getBottomHeight();
        return padBottom;
    }

    /** See {@link #setPadding(float, float, float, float)} */
    public void setPadBottom (float bottom) {
        this.padBottom = bottom;
    }

    /** Multiplies the top/left/bottom/right sizes and padding by the specified amount. */
    public void scale (float scaleX, float scaleY) {
        leftWidth *= scaleX;
        rightWidth *= scaleX;
        topHeight *= scaleY;
        bottomHeight *= scaleY;
        middleWidth *= scaleX;
        middleHeight *= scaleY;
        if (padLeft != -1) padLeft *= scaleX;
        if (padRight != -1) padRight *= scaleX;
        if (padTop != -1) padTop *= scaleY;
        if (padBottom != -1) padBottom *= scaleY;
    }

    public Texture getTexture () {
        return texture;
    }

    public void setMaskScale(float maskScaleX, float maskScaleY){
        this.maskScaleX = maskScaleX;
        this.maskScaleY = maskScaleY;
    }
}