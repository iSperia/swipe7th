package com.game7th.swipe.game.battle

val waterFragmentShader = """#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_texture2;
uniform float timedelta;
uniform vec2 sss;
uniform vec2 u_maxclamp;
void main()                                  
{    
  gl_FragColor = v_color * texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + sin(v_texCoords.x * 40.0 + timedelta) * 0.002));

//    gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
//    //после получения итогового цвета, меняем его на противоположный
//    gl_FragColor.rgb=1.0-gl_FragColor.rgb;
}"""

val vertexShader = """attribute vec4 a_position;    
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
varying vec4 v_color;varying vec2 v_texCoords;void main()                  
{                            
   v_color = vec4(1, 1, 1, 1); 
   v_texCoords = a_texCoord0; 
   gl_Position =  u_projTrans * a_position;  
}"""