// Liquid Ether concept: slow, domain-warped procedural flow in native WebGL.
// https://www.reactbits.dev/backgrounds/liquid-ether
// Deliberately avoids the original multi-pass pressure solver and dependencies.
export function startLoginLiquid(canvas){
    const host=canvas.parentElement,reduce=matchMedia('(prefers-reduced-motion: reduce)'),touch=matchMedia('(hover: none)');
    const gl=canvas.getContext('webgl',{alpha:false,antialias:false,powerPreference:'low-power'});
    if(!gl){canvas.hidden=true;host.dataset.renderer='static';return;}
    const resources=[];let program,frame=0,last=0,paint=0,time=0,lost=false;
    const vertex='attribute vec2 p;varying vec2 uv;void main(){uv=p*.5+.5;gl_Position=vec4(p,0.,1.);}';
    const fragment=`precision highp float;varying vec2 uv;uniform float t;uniform float aspect;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+1.),f.x),f.y);}
float fbm(vec2 p){float v=0.,a=.5;for(int i=0;i<3;i++){v+=a*noise(p);p=mat2(.8,-.6,.6,.8)*p*2.02+3.7;a*=.5;}return v;}
void main(){vec2 p=(uv-.5)*vec2(aspect,1.)*2.4;float s=t*.065;
vec2 q=vec2(fbm(p+vec2(s,-s*.3)),fbm(p+vec2(4.2,-s*.45)));
vec2 r=vec2(fbm(p+2.8*q+vec2(1.7,s*.5)),fbm(p+2.8*q+vec2(8.3,-s*.4)));
float flow=fbm(p+2.6*r+vec2(s*.2,0));float ribbon=pow(.5+.5*sin(flow*10.+q.x*3.+s*.45),5.);
float pool=smoothstep(.22,.78,flow);vec3 ink=mix(vec3(.027,.031,.043),vec3(.29,.086,.149),pool*.84);
ink=mix(ink,vec3(.65,.118,.235),ribbon*.45);ink+=vec3(.19,.026,.060)*pow(r.y,3.);
float edge=1.-smoothstep(.3,1.6,length(p*.55));ink*=.72+.28*edge;ink+=(hash(gl_FragCoord.xy)-.5)/255.;gl_FragColor=vec4(ink,1.);}`;
    function shader(type,source){const s=gl.createShader(type);resources.push(s);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw Error('shader');return s;}
    function fallback(){lost=true;cancelAnimationFrame(frame);canvas.hidden=true;host.dataset.renderer='static';}
    try{
        program=gl.createProgram();gl.attachShader(program,shader(gl.VERTEX_SHADER,vertex));gl.attachShader(program,shader(gl.FRAGMENT_SHADER,fragment));gl.linkProgram(program);if(!gl.getProgramParameter(program,gl.LINK_STATUS))throw Error('program');gl.useProgram(program);
        const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,1,-1,-1,1,1,1]),gl.STATIC_DRAW);
        const pos=gl.getAttribLocation(program,'p');gl.enableVertexAttribArray(pos);gl.vertexAttribPointer(pos,2,gl.FLOAT,false,0,0);
        const clock=gl.getUniformLocation(program,'t'),aspect=gl.getUniformLocation(program,'aspect');
        function draw(){if(lost)return;gl.uniform1f(clock,time);gl.uniform1f(aspect,innerWidth/innerHeight);gl.drawArrays(gl.TRIANGLE_STRIP,0,4);}
        function resize(){if(lost)return;const budget=touch.matches?220000:450000,scale=Math.min(1,Math.sqrt(budget/(innerWidth*innerHeight)));canvas.width=Math.max(1,Math.round(innerWidth*scale));canvas.height=Math.max(1,Math.round(innerHeight*scale));gl.viewport(0,0,canvas.width,canvas.height);draw();}
        function tick(now){frame=0;if(lost||document.hidden||reduce.matches)return;const dt=last?Math.min((now-last)/1000,.08):0;last=now;time+=dt;if(now-paint>=1000/(touch.matches?15:24)){draw();paint=now;}frame=requestAnimationFrame(tick);}
        function sync(){cancelAnimationFrame(frame);last=0;host.dataset.motion=reduce.matches?'static':'animated';if(!lost&&!document.hidden&&!reduce.matches)frame=requestAnimationFrame(tick);else draw();}
        window.addEventListener('resize',resize,{passive:true});document.addEventListener('visibilitychange',sync);reduce.addEventListener('change',sync);touch.addEventListener('change',resize);
        canvas.addEventListener('webglcontextlost',event=>{event.preventDefault();fallback();});
        window.addEventListener('pagehide',()=>{cancelAnimationFrame(frame);last=0;});window.addEventListener('pageshow',sync);
        host.dataset.renderer='webgl';resize();sync();
    }catch{fallback();if(program)gl.deleteProgram(program);resources.forEach(s=>gl.deleteShader(s));}
}
