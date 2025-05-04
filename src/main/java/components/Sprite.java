package components;

import org.joml.Vector2f;
import renderer.Texture;

public class Sprite {

    private Texture texture;
    private Vector2f[] texCords;


    public Sprite(Texture texture){
        this.texture = texture;
        Vector2f[] texCords = {
                new Vector2f(1,1),
                new Vector2f(1,0),
                new Vector2f(0,0),
                new Vector2f(0,1)
        };
        this.texCords = texCords;
    }

    public Sprite(Texture texture, Vector2f[] texCords){
        this.texture = texture;
        this.texCords = texCords;
    }
    public Texture getTexture(){
        return this.texture;
    }
    public Vector2f[] getTexCords(){
        return this.texCords;
    }

}
