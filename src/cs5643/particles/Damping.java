package cs5643.particles;
import javax.vecmath.*;
import javax.media.opengl.GL2;

public class Damping implements Constraint{

	private ParticleSystem ps;
	
	
	//Damping constant
	public static double k_damping = 0.5;
	private static double k_d = k_damping;
	
	// Position for center of mass
	private Vector3d xcm = new Vector3d();
	
	// Velocity of center of mass
	private Vector3d vcm = new Vector3d();
	
	// Total mass of cloth
	private double clothMass = 0;
	
	// Angular momentun
	private Vector3d L = new Vector3d();
	
	// r is the distance of each particle to COM >>> r_i = x_i - x_cm
	private Vector3d r = new Vector3d();
	
	// Moment of Inertia matrix (3x) and its Inverse
	private Matrix3d I = new Matrix3d();
	
	// (rbar)(v) = r x v, cross product operator (3x3 matrix), rbarT is transpose of rbar
	private Matrix3d rbar = new Matrix3d();
	private Matrix3d rbarT = new Matrix3d();
	
	// omega is the angular velocity of the cloth
	private Vector3d omega = new Vector3d();
	
	// temporary value used for v_i scaled by m_i
	private Vector3d mV = new Vector3d();
	
	// temporary holder for intermediate cross products
	private Vector3d tempCross = new Vector3d();
	
	// temporary holder for intermediate 3x3 matrix
	private Matrix3d m3x3 = new Matrix3d();
	
	// change in velocity prior to scaling by damping factor
	private Vector3d deltaV = new Vector3d();
	
	public Damping(ParticleSystem ps){
		this.ps=ps;
	}
	
	public void display(GL2 gl) {}
	
	public ParticleSystem getParticleSystem() {
	    return ps;
	  }

	
	public void applyConstraint() {
		
		//Update k_d based on slider
		k_d = k_damping;
		
		//Calculate total mass of cloth based on all points (includes mass of fixed points where w=0)
		//Calculate position and velocity of COM
		clothMass = 0.0;
		xcm.set(0,0,0);
		vcm.set(0,0,0);
		for(Particle p : getParticleSystem().P){
			clothMass+=p.m;
			xcm.scaleAdd(p.m,p.x,xcm);
			vcm.scaleAdd(p.m,p.v,vcm);
		}
		xcm.scale(1/clothMass);
		vcm.scale(1/clothMass);
		
		// Calculate L and I
		L.set(0,0,0);
		rbar.setZero();
		rbarT.setZero();
		I.setZero();
		for(Particle p : getParticleSystem().P){
			r.sub(p.x, xcm);
			mV.scale(p.m,p.v);
			tempCross.cross(r, mV);
			L.add(tempCross);
			
			/**
			 * Matrix3d reference notation
			 *    [  m00     m01     m02   ]
			 *    [  m10     m11     m12   ]
			 *    [  m20     m21     m22   ]
			 *    
			 * Skew symmetric matrix reference   
			 *    [   0     -a3     a2   ]
			 *    [  a3       0    -a1   ]
			 *    [ -a2      a1      0   ]
			 */
			
			rbar.m01 = -r.z;
			rbar.m02 =  r.y;
			rbar.m12 = -r.x;
			rbar.m10 =  r.z;
			rbar.m20 = -r.y;
			rbar.m21 =  r.x;
			
			rbarT.transpose(rbar);
			
			m3x3.mul(rbar,rbarT);
			m3x3.mul(p.m);
			
			I.add(m3x3);
		}
		
		//Calculate omega
		I.invert();
		I.transform(L,omega);
		
		deltaV.set(0,0,0);
		for (Particle p : getParticleSystem().P){
			r.sub(p.x, xcm);
			tempCross.cross(omega, r);
			deltaV.add(vcm,tempCross);
			deltaV.sub(p.v);
			p.v.scaleAdd(k_d,deltaV,p.v);
		}
		
	}
	
}
