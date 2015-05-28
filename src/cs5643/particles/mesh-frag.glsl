#version 120

// Standard diffuse shader. Code originally from Edward Angel's OpenGL: A Primer.

varying vec3 N;
varying vec4 eyePosition;

void main()
{
  vec3 normal  = normalize(N);
  vec3 light   = normalize(gl_LightSource[0].position.xyz - eyePosition.xyz);
  vec4 front   = max(dot(normal, light), 0.0) *
          gl_FrontMaterial.diffuse*gl_LightSource[0].diffuse;
  vec4 back    = max(dot(-normal, light), 0.0) *
          gl_BackMaterial.diffuse*gl_LightSource[0].diffuse;
  
  //vec3 temp = (normal - vec3(0.5, 0.5, 0.5)) * 2;
  gl_FragColor = front + back; //vec4(temp, 1);
}