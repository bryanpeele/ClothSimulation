package cs5643.particles;
import javax.media.opengl.GL2;

public class Gravity implements Force{

	private ParticleSystem ps;
	
	public static double gravity = 10.0;
	
	public Gravity(ParticleSystem ps){
		this.ps=ps;
	}
	
	public void applyForce(){
		for (Particle p : getParticleSystem().P){
			p.f.y = -gravity;
			//p.f.y = 10*Math.cos(10*ps.time); //sine wave forcing funtion, for fun
		}

	}
	
	
	public void display(GL2 gl){}
	
	public ParticleSystem getParticleSystem() {
	    return ps;
	}

}
