package cs5643.particles;
import javax.vecmath.*;
import javax.media.opengl.GL2;


public class Balloon implements Constraint{
	
	private ParticleSystem ps;
	private Mesh ms;
	
	public static double k_pressure = 1.0;
	public static double k_pressure_stiffness = 0.9;
	private static double k_p =  k_pressure;
	private static double k_b =	1 - Math.pow((1-k_pressure_stiffness),1.0/Constants.solverIterations);
	private double volume;
	private Vector3d p1 = new Vector3d();
	private Vector3d p2 = new Vector3d();
	private Vector3d p3 = new Vector3d();
	private Vector3d P1crossP2 = new Vector3d();
	private Vector3d P2crossP3 = new Vector3d();
	private Vector3d P3crossP1 = new Vector3d();
	private double C;
	private Vector3d gradC = new Vector3d();
	private double s = 0.0;
	private double denom = 0.0;
	private Vector3d deltaP = new Vector3d();
	
	public Balloon(ParticleSystem ps, Mesh ms){
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
		//Update k_p based on slider
		k_p = k_pressure;
		k_b =	1 - Math.pow((1-k_pressure_stiffness),1.0/Constants.solverIterations);
		
		//Initialize values to zero
		volume = 0.0;
		s = 0.0;
		deltaP.set(0.0,0.0,0.0);
		
		// Calculate C by iterating over all triangles to get volume
		for (Triangle t : getMesh().triangles) {
			p1.set(t.v0.x);
			p2.set(t.v1.x);
			p3.set(t.v2.x);			
			P1crossP2.cross(p1,p2);
			volume += P1crossP2.dot(p3);		
		}
		C = volume - (k_p*getMesh().initialVolume);
		
		// Calculate s by getting the gradient at each vertex
		denom = 0.0;
		for (Vertex v : getMesh().vertices) {
			gradC.set(0,0,0);
			for (Triangle t : v.triangles) {
				p1.set(t.v0.x);
				p2.set(t.v1.x);
				p3.set(t.v2.x);
				
				if(v == t.v0) {
					P2crossP3.cross(p2, p3);
					gradC.add(P2crossP3);
				} else if(v == t.v1) {
					P3crossP1.cross(p3, p1);
					gradC.add(P3crossP1);
				} else if(v == t.v2) {
					P1crossP2.cross(p1, p2);
					gradC.add(P1crossP2);
				}
			}
			denom += gradC.lengthSquared();
		}
		s = C/denom;		
		
		// Calculate deltaP and apply to each vertex
		for (Vertex v : getMesh().vertices) {
			gradC.set(0,0,0);
			for (Triangle t : v.triangles) {
				p1.set(t.v0.x);
				p2.set(t.v1.x);
				p3.set(t.v2.x);
				
				if(v == t.v0) {
					P2crossP3.cross(p2, p3);
					gradC.add(P2crossP3);
				} else if(v == t.v1) {
					P3crossP1.cross(p3, p1);
					gradC.add(P3crossP1);
				} else if(v == t.v2) {
					P1crossP2.cross(p1, p2);
					gradC.add(P1crossP2);
				}
			}
			deltaP.set(0,0,0);
			deltaP.scaleAdd(-s*v.w,gradC, deltaP);
			v.x.scaleAdd(k_b,deltaP,v.x);
		}
	}

}
