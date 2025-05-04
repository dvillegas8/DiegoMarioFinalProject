#type vertex
#version 330 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec4 aColor;
layout (location=2) in vec2 aTexCords;
layout (location=3) in float aTexId;

uniform mat4 uProjection;
uniform mat4 uView;

out vec4 fColor;
out vec2 fTexCords;
out float fTexId;

void main()
{
    fColor = aColor;
    fTexCords = aTexCords;
    fTexId = aTexId;
    gl_Position = uProjection * uView * vec4(aPos, 1.0);
}

#type fragment
#version 330 core

in vec4 fColor;
in vec2 fTexCords;
in float fTexId;

uniform sampler2D uTextures[8];

out vec4 color;

void main()
{
    if(fTexId > 0){
        int id = int(fTexId);
        color = fColor * texture(uTextures[id], fTexCords);
    }
    else{
        color = fColor;
    }
}