import numpy as np, wave
sr=44100
notes=[(1046.50,0.0),(1318.51,0.09),(1567.98,0.18),(2093.00,0.30)]
dur=1.3
t=np.arange(int(sr*dur))/sr
out=np.zeros_like(t)
for i,(f,start) in enumerate(notes):
    tt=t-start; m=tt>=0; x=tt[m]
    decay=3.2 if i==3 else 6.0
    env=(1-np.exp(-x/0.004))*np.exp(-x*decay)
    tone=np.sin(2*np.pi*f*x)+0.25*np.sin(2*np.pi*2*f*x)*np.exp(-x*8)+0.08*np.sin(2*np.pi*3*f*x)*np.exp(-x*12)
    out[m]+=tone*env*(0.9 if i==3 else 0.7)
fade=np.ones_like(t); n=int(0.15*sr); fade[-n:]=np.linspace(1,0,n); out*=fade
out=out/np.max(np.abs(out))*10**(-9/20)
pcm=(out*32767).astype('<i2')
with wave.open('win.wav','wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes(pcm.tobytes())
