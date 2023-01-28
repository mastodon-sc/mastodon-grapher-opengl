#version 150
 
in vec3 fcolor;
out vec4 outColor;
 
void main()
{
    outColor = vec4(fcolor,1.0);
}
