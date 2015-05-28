package cs5643.particles;

import javax.media.opengl.GL2;
import javax.vecmath.*;

public class ClickForce implements Force{

	private ParticleSystem ps;
	public static Particle p = null;
	public static Vector3d goal = new Vector3d();
	private static Vector3d difference = new Vector3d();
	public static int k_clicking = 1000;
	
	public ClickForce(ParticleSystem ps){
		this.ps=ps;
	}
	
	
	public void display(GL2 gl){}
	
	public ParticleSystem getParticleSystem() {
	    return ps;
	}
	
	public void applyForce(){
		if (p != null && goal != null) {
			difference.sub(goal,p.x);
			difference.scale((double) k_clicking);
			p.f.set(difference);
		}
		
	}
	
}
