package cs5643.particles;
import javax.media.opengl.GL2;

public class Boundary implements Constraint{

	private ParticleSystem ps;
	
	double k = 1;
	
	public Boundary(ParticleSystem ps){
		this.ps=ps;
	}
	
	public void display(GL2 gl) {}
	
	public ParticleSystem getParticleSystem() {
	    return ps;
	  }
	
	public void applyConstraint() {
		for(Particle p:getParticleSystem().P){
			if(p.x.x < 0.0) {p.x.x = 0.0;}
			if(p.x.x > 1.0) {p.x.x = 1.0;}
			if(p.x.y < 0.0) {p.x.y = 0.0;}
			if(p.x.y > 1.0) {p.x.y = 1.0;}
			if(p.x.z < 0.0) {p.x.z = 0.0;}
			if(p.x.z > 1.0) {p.x.z = 1.0;}
		}
	}
	
}
