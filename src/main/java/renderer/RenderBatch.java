package renderer;

import Util.AssetPool;
import components.SpriteRenderer;
import jade.Window;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class RenderBatch implements Comparable<RenderBatch>{
    // Vertex
    // ======
    // Pos                  Color                               tex cords      tex id
    // float, float,        float, float, float                 float, float,   float
    private final int POS_SIZE = 2;
    // Because there is 4 colors
    private final int COLOR_SIZE = 4;
    private final int TEX_CORDS_SIZE = 2;
    private final int TEX_ID_SIZE = 1;

    private final int POS_OFFSET = 0;
    private final int COLOR_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
    private final int TEX_CORDS_OFFSET = COLOR_OFFSET + COLOR_SIZE * Float.BYTES;
    private final int TEX_ID_OFFSET = TEX_CORDS_OFFSET + TEX_CORDS_SIZE * Float.BYTES;
    // Total size of our vertex contains 6 floats
    private final int VERTEX_SIZE = 9;
    private final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private SpriteRenderer[] sprites;
    private int numSprites;
    private boolean hasRoom;
    private float[] vertices;
    private int[] texSlots = {0, 1, 2, 3, 4, 5, 6, 7};

    private List<Texture> textures;
    private int vaoID, vboID;
    private int maxBatchSize;
    private Shader shader;
    private int zIndex;

    public RenderBatch(int maxBatchSize, int zIndex){
        this.zIndex = zIndex;
        // Reference count goes from 10 to 1
        shader = AssetPool.getShader("assets/shaders/default.glsl");
        this.sprites = new SpriteRenderer[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        // 4 vertices quads, the 4 because we have 4 vertices per quad
        vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];

        this.numSprites = 0;
        this.hasRoom = true;
        this.textures = new ArrayList<>();
    }

    public void start(){
        // Generate and bind a vertex array Object
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Allocate space for vertices
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        // Create and upload indices buffer
        int eboID = glGenBuffers();
        int[] indices = generateIndices();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Enable the buffer attribute pointers
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_OFFSET);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TEX_CORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_CORDS_OFFSET);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_ID_OFFSET);
        glEnableVertexAttribArray(3);

    }

    public void addSprite(SpriteRenderer spr){
        // Get index and add renderObject
        int index = this.numSprites;
        // [0, 1, 2, 3, 4, 5]
        this.sprites[index] = spr;
        this.numSprites++;

        // Check if the sprite has a texture
        if(spr.getTexture() != null){
            // Check if we have it already
            if(!textures.contains(spr.getTexture())){
                // Add to list of textures if we don't have it yet
                textures.add(spr.getTexture());

            }
        }

        // Add properties to local vertices array
        loadVertexProperties(index);

        if(numSprites >= this.maxBatchSize){
            this.hasRoom = false;
        }

    }

    public void render(){
        boolean rebufferData = false;
        for(int i = 0; i < numSprites; i++){
            SpriteRenderer spr = sprites[i];
            if(spr.isDirty()){
                loadVertexProperties(i);
                spr.setClean();
                rebufferData = true;
            }
        }
        if(rebufferData){
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        }

        // Use shader
        shader.use();
        shader.uploadMat4f("uProjection", Window.getScene().camera().getProjectionMatrix());
        shader.uploadMat4f("uView", Window.getScene().camera().getViewMatrix());
        for(int i = 0; i < textures.size(); i++){
            glActiveTexture(GL_TEXTURE0 + i + 1);
            textures.get(i).bind();
        }
        // Tells openGL that these are all of the texture slots we are uploading
        shader.uploadIntArray("uTextures", texSlots);

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawElements(GL_TRIANGLES, this.numSprites * 6, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        for(int i = 0; i < textures.size(); i++){
            textures.get(i).unbind();
        }

        shader.detach();

    }

    private void loadVertexProperties(int index){
        SpriteRenderer sprite = this.sprites[index];

        // Find offset within array (4 vertices per sprite)
        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = sprite.getColor();
        Vector2f[] texCords = sprite.getTexCords();

        int texID = 0;
        if(sprite.getTexture()!= null){
            for(int i = 0; i < textures.size(); i++){
                // If we are on the texture that the sprite has
                if(textures.get(i) == sprite.getTexture()){
                    // +1 because 0 is a special reserve slot that doesn't hold a texture
                    texID = i + 1;
                    break;
                }
            }
        }



        // Add vertice with the appropriate properties

        float xAdd = 1.0f;
        float yAdd = 1.0f;
        for(int i = 0; i < 4; i++){
            if(i == 1){
                yAdd = 0.0f;
            }
            else if(i ==2){
                xAdd = 0.0f;
            }
            else if(i == 3){
                yAdd = 1.0f;
            }

            // Load Position, make sure we order the vertices in the correct order
            vertices[offset] = sprite.gameObject.transform.position.x + (xAdd * sprite.gameObject.transform.scale.x);
            vertices[offset + 1] = sprite.gameObject.transform.position.y + (yAdd * sprite.gameObject.transform.scale.y);

            // Load color
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;

            // Load texture coordinates
            vertices[offset + 6] = texCords[i].x;
            vertices[offset + 7] = texCords[i].y;

            // Load texture ID
            vertices[offset + 8] = texID;

            offset += VERTEX_SIZE;
        }
    }

    private int[] generateIndices(){
        // 6 indices per quad (3 per triangle)
        int[] elements = new int[6 * maxBatchSize];
        for(int i = 0; i < maxBatchSize; i++){
            loadElementIndices(elements, i);
        }
        return elements;
    }

    private void loadElementIndices(int[] elements, int index){
        int offsetArrayIndex = 6 * index;
        int offset = 4 * index;

        // 3, 2, 0, 0, 2, 1         7, 6 , 4, 4, 6, 5
        // Triangle 1
        elements[offsetArrayIndex] = offset + 3;
        elements[offsetArrayIndex + 1] = offset + 2;
        elements[offsetArrayIndex + 2] = offset + 0;

        // Triangle 2
        elements[offsetArrayIndex + 3] = offset + 0;
        elements[offsetArrayIndex + 4] = offset + 2;
        elements[offsetArrayIndex + 5] = offset + 1;
    }
    public boolean hasRoom(){
        return this.hasRoom;
    }
    public boolean hasTextureRoom(){
        return this.textures.size() < 8;
    }
    public boolean hasTexture(Texture tex){
        return this.textures.contains(tex);
    }
    public int zIndex(){
        return this.zIndex;
    }

    @Override
    public int compareTo(@NotNull RenderBatch o) {
        return Integer.compare(this.zIndex, o.zIndex());
    }
}
