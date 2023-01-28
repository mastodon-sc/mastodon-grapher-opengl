#version 150
 
uniform sampler2D texUnit; 
 
in vec2 ftexcoord;
out vec4 outColor;
 
void main()
{
		float value = texture(texUnit, ftexcoord).r;
    outColor = vec4(value,value,value,value);
}
