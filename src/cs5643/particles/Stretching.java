package cs5643.particles;
import javax.vecmath.*;
import javax.media.opengl.GL2;

public class Stretching implements Constraint{

	private ParticleSystem ps;
	private Mesh ms;
	
	public static double k_stretching = 0.5;
	private static double k_s = 1 - Math.pow((1-k_stretching),1.0/Constants.solverIterations);
	private Vector3d normal = new Vector3d();
	private double C = 0.0;
	private double s = 0.0;
	private Vector3d deltaP0 = new Vector3d();
	private Vector3d deltaP1 = new Vector3d();
	
	public Stretching(ParticleSystem ps, Mesh ms){
		this.ps=ps;
		this.ms=ms;
	}
	
	public void display(GL2 gl) {}
	
	public ParticleSystem getParticleSystem() {
	    return ps;
	  }

	public Mesh getMesh() {
	    return ms;
	  }
	
	public void applyConstraint() {
		//Update k_s based on slider each iteration
		k_s = 1 - Math.pow((1-k_stretching),1.0/Constants.solverIterations);
		
		for(Edge e : getMesh().edges){
			deltaP0.set(0,0,0);
			deltaP1.set(0,0,0);
			normal.sub(e.v1.x,e.v0.x);
			C = (e.length() - e.restLength);
			s = C/((e.v0.w + e.v1.w)*e.length());
			
			// Calculate deltaP for each vertex
			deltaP0.scaleAdd(e.v0.w*s,normal,deltaP0);
			deltaP1.scaleAdd(-e.v1.w*s,normal,deltaP1);
			
			// Add deltaP based on the preset stiffness
			e.v0.x.scaleAdd(k_s,  deltaP0, e.v0.x);
			e.v1.x.scaleAdd(k_s,  deltaP1, e.v1.x);
		}
		
	}
	
}
