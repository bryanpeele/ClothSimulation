package cs5643.particles;
import javax.vecmath.*;
import javax.media.opengl.GL2;

public class Bending implements Constraint{
	
	private ParticleSystem ps;
	private Mesh ms;
	
	public static double k_bending = 0.5;
	private static double k_b = 1.0 - Math.pow((1.0-k_bending),1.0/Constants.solverIterations);
	private Vector3d p1 = new Vector3d();
	private Vector3d p2 = new Vector3d();
	private Vector3d p3 = new Vector3d();
	private Vector3d p4 = new Vector3d();
	private boolean bendingPlane = true;
	private Vector3d n1 = new Vector3d();
	private Vector3d n2 = new Vector3d();
	private Vector3d p2crossp3 = new Vector3d();
	private Vector3d p2crossp4 = new Vector3d();
	private double d = 0;
	private double C = 0;
	private Vector3d q1 = new Vector3d();
	private Vector3d q2 = new Vector3d();
	private Vector3d q2Left = new Vector3d();
	private Vector3d q2Right = new Vector3d();
	private Vector3d q3 = new Vector3d();
	private Vector3d q4 = new Vector3d();
	private Vector3d p2crossn2 = new Vector3d();
	private Vector3d n1crossp2 = new Vector3d();
	private Vector3d n1crossp3 = new Vector3d();
	private Vector3d p2crossn1 = new Vector3d();
	private Vector3d n2crossp2 = new Vector3d();
	private Vector3d p3crossn2 = new Vector3d();
	private Vector3d p4crossn1 = new Vector3d();
	private Vector3d n2crossp4 = new Vector3d();
	private double denom = 0.0;
	private double numer = 0.0;
	private Vertex v1;
	private Vertex v2;
	private Vertex v3;
	private Vertex v4;
	private Vector3d deltaP1 = new Vector3d();
	private Vector3d deltaP2 = new Vector3d();
	private Vector3d deltaP3 = new Vector3d();
	private Vector3d deltaP4 = new Vector3d();
	
	
	public Bending(ParticleSystem ps, Mesh ms){
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
		//Update k_b based on slider
		k_b = 1.0 - Math.pow((1.0-k_bending),1.0/Constants.solverIterations);
		
		for(Edge e : getMesh().edges) {
			v1 = e.v0;
			p1.set(v1.x);
			v2 = e.v1;
			p2.set(v2.x);
			
			bendingPlane = true;
			//Need to check to see if there are triangle on both sides of the edge.
			
			//First check t0 to see if p3 exists
			try {
				v3 = e.t0.getThirdVertex(e.v0,e.v1);
				p3.set(v3.x);
			}
			catch(NullPointerException t0DoesNotExist){
				bendingPlane = false;
			}
			//Now try t1 to see if p4 exists
			try {
				v4 = e.t1.getThirdVertex(e.v0,e.v1);
				p4.set(v4.x);
			}
			catch(NullPointerException t1DoesNotExist){
				bendingPlane = false;
			}
			
			//If both triangles exist, we can apply the bending constraint for this edge
			if (bendingPlane == true){
				
				//Set p1 as origin
				p2.sub(p1);
				p3.sub(p1);
				p4.sub(p1);
				p1.set(0.0,0.0,0.0);
				
				//Set normal, n1
				p2crossp3.cross(p2, p3);
				n1.normalize(p2crossp3);
				
				//Set normal, n2
				p2crossp4.cross(p2, p4);
				n2.normalize(p2crossp4);
								
				d = n1.dot(n2);
				if(d>1.0) {d = 1.0;}
				else if(d<-1.0) {d = -1.0;}
				
				//Constraint function
				C = Math.acos(d) - e.phi0;
				
				
				//Lots of cross products used to find q1....q4
				p2crossn2.cross(p2,n2);
				n1crossp2.cross(n1,p2);
				n1crossp3.cross(n1,p3);
				p2crossn1.cross(p2,n1);
				n2crossp2.cross(n2,p2);
				p3crossn2.cross(p3,n2);
				p4crossn1.cross(p4,n1);
				n2crossp4.cross(n2,p4);
				
				//Initialize q1...q4 to 0 vector
				q1.set(0,0,0);
				q2.set(0,0,0);
				q3.set(0,0,0);
				q4.set(0,0,0);
				deltaP1.set(0,0,0);
				deltaP2.set(0,0,0);
				deltaP3.set(0,0,0);
				deltaP4.set(0,0,0);
				
				//Solve for q1....q4
				if(p2crossp3.length()!=0) {
					q3.scaleAdd(d,n1crossp2,p2crossn2);
					q3.scale(1.0/p2crossp3.length());
					q2Left.scaleAdd(d,n1crossp3,p3crossn2);
					q2Left.scale(-1.0/p2crossp3.length());
				} else {
					q3.set(0,0,0);
					q2Left.set(0,0,0);
				}

				if(p2crossp4.length()!=0) {
					q2Right.scaleAdd(d,n2crossp4,p4crossn1);
					q2Right.scale(-1.0/p2crossp4.length());					
					q4.scaleAdd(d,n2crossp2,p2crossn1);
					q4.scale(1.0/p2crossp4.length());
					
				} else {
					q4.set(0,0,0);
					q2Right.set(0,0,0);
				}

				
				q2.add(q2Left,q2Right);
				
				//To get q1, first add q2,q3,q4 and then sale to make negative
				q1.add(q2);
				q1.add(q3);
				q1.add(q4);
				q1.scale(-1.0);

			 
				//Now ready to update particle positions with Eqn 29
				denom = (v1.w*q1.lengthSquared()) + (v2.w*q2.lengthSquared()) + (v3.w*q3.lengthSquared()) + (v4.w*q4.lengthSquared());
				numer = C*Math.sqrt(1.0-(d*d));
				
				if(denom!=0){
					deltaP1.scaleAdd(-v1.w*numer/denom,q1,deltaP1);
					deltaP2.scaleAdd(-v2.w*numer/denom,q2,deltaP2);
					deltaP3.scaleAdd(-v3.w*numer/denom,q3,deltaP3);
					deltaP4.scaleAdd(-v4.w*numer/denom,q4,deltaP4);
				}

				e.v0.x.scaleAdd(k_b,deltaP1,e.v0.x);
				e.v1.x.scaleAdd(k_b,deltaP2,e.v1.x);
				e.t0.getThirdVertex(e.v0,e.v1).x.scaleAdd(k_b,deltaP3,e.t0.getThirdVertex(e.v0,e.v1).x);
				e.t1.getThirdVertex(e.v0,e.v1).x.scaleAdd(k_b,deltaP4,e.t1.getThirdVertex(e.v0,e.v1).x);
				
			}
		}
	}
	
}
