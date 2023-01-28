#version 150
 
uniform mat4 viewMatrix, projMatrix;

in vec4 position;
in vec2 texcoord;
 
out vec2 ftexcoord;
 
void main()
{
    ftexcoord = texcoord;
    gl_Position = projMatrix * viewMatrix * position ;
}


