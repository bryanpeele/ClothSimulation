package cs5643.particles;

import java.util.*;

import javax.vecmath.*;
import javax.media.opengl.*;

import com.jogamp.opengl.util.glsl.*;


/**
 * Maintains dynamic lists of Particle and Force objects, and provides
 * access to their state for numerical integration of dynamics.
 *
 * @author Doug James, January 2007
 * @author Eston Schweickart, February 2014
 */
public class ParticleSystem //implements Serializable
{
	/** Number of hanging points **/
	public static int hanging = 0;
	
	
    /** Current simulation time. */
    public double time = 0;

    /** List of Particle objects. */
    public ArrayList<Particle> P = new ArrayList<Particle>();

    /** List of Force objects. */
    public ArrayList<Force> F = new ArrayList<Force>();

    /** List of Constraint objects. */
    public ArrayList<Constraint> C = new ArrayList<Constraint>();
    
    /** List of Damping objects. */
    public ArrayList<Constraint> D = new ArrayList<Constraint>();
    
    /**
     * true iff prog has been initialized. This cannot be done in the
     * constructor because it requires a GL2 reference.
     */
    private boolean init = false;

    /** Filename of vertex shader source. */
    public static final String[] VERT_SOURCE = {"vert.glsl"};

    /** Filename of fragment shader source. */
    public static final String[] FRAG_SOURCE = {"frag.glsl"};

    /** The shader program used by the particles. */
    ShaderProgram prog;


    /** Basic constructor. */
    public ParticleSystem() {}

    /**
     * Set up the GLSL program. This requires that the current directory (i.e. the package in which
     * this class resides) has a vertex and fragment shader.
     */
    public synchronized void init(GL2 gl) {
        if (init) return;

        prog = new ShaderProgram();
        ShaderCode vert_code = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(), VERT_SOURCE, false);
        ShaderCode frag_code = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(), FRAG_SOURCE, false);
        if (!prog.add(gl, vert_code, System.err) || !prog.add(gl, frag_code, System.err)) {
            System.err.println("WARNING: shader did not compile");
            prog.init(gl); // Initialize empty program
        } else {
            prog.link(gl, System.err);
        }

        init = true;
    }

    /** Adds a force object (until removed) */
    public synchronized void addForce(Force f) {
        F.add(f);
    }

    /** Useful for removing temporary forces, such as user-interaction
     * spring forces. */
    public synchronized void removeForce(Force f) {
        F.remove(f);
    }

    
    /** Adds a constraint object (until removed) */
    public synchronized void addConstraint(Constraint c) {
        C.add(c);
    }

    /** Useful for removing temporary constraints, such as user-interaction*/
    public synchronized void removeConstraint(Constraint c) {
        C.remove(c);
    }

    /** Adds a damping object (until removed) */
    public synchronized void addDamping(Constraint d) {
        D.add(d);
    }

    /** Useful for removing temporary damping*/
    public synchronized void removeDamping(Constraint d) {
        D.remove(d);
    }
    
    
    /** Creates particle and adds it to the particle system.
     * @param p0 Undeformed/material position.
     * @return Reference to new Particle.
     */
    public synchronized Particle createParticle(Point3d p0)
    {
        Particle newP = new Particle(p0);
        P.add(newP);
        return newP;
    }

    /**
     * Helper-function that computes nearest particle to the specified
     * (deformed) position.
     * @return Nearest particle, or null if no particles.
     */
    public synchronized Particle getNearestParticle(Point3d x)
    {
        Particle minP      = null;
        double   minDistSq = Double.MAX_VALUE;
        for(Particle particle : P) {
            double distSq = x.distanceSquared(particle.x);
            if(distSq < minDistSq) {
                minDistSq = distSq;
                minP = particle;
            }
        }
        return minP;
    }

    /** Moves all particles to undeformed/materials positions, and
     * sets all velocities to zero. Synchronized to avoid problems
     * with simultaneous calls to advanceTime(). */
    public synchronized void reset()
    {
        for(Particle p : P)  {
            p.x.set(p.x0);
            p.p.set(p.x0);
            p.v.set(0,0,0);
            p.f.set(0,0,0);
            p.setHighlight(false);
        }
        time = 0;
    }

    /**
     * Incomplete/Debugging implementation of Forward-Euler
     * step. WARNING: Contains buggy debugging forces.
     */
    public synchronized void advanceTime(double dt)
    {
        /// Clear force accumulators:
        for(Particle p : P)  p.f.set(0,0,0);
        
        /// Gather forces:
        for(Force force : F) {
            force.applyForce();
        }
              
        for(Particle p : P) {
        	p.f.scale(p.w);
        	p.v.scaleAdd(dt, p.f, p.v);
        }
        
        // Apply damping constraint
        for(Constraint d : D) {
        	d.applyConstraint();
        }
        
        
        /// TIME-STEP:
        for(Particle p : P) {
            p.x.scaleAdd(dt*p.w, p.v, p.p);
        }
        
        int iter = 0;
        while (iter < Constants.solverIterations){
	        for(Constraint c : C){
	        	c.applyConstraint();
	        }
	        iter++;
        }
        
        for(Particle p : P) {
        	p.v.scaleAdd(-1, p.p, p.x);
        	p.v.scale(1/dt);
            p.p.set(p.x); 
        }
        
        boolean goAhead = true;
        try {
        	P.get(0);
        	P.get(8);
        	P.get(72);
        	P.get(80);
        } catch(IndexOutOfBoundsException e) {
        	System.out.println("IndexOutOfBoundsException");
        	goAhead = false;
        }
        
        if(goAhead){
	        if(hanging == 0){
	    		if(P.get(0)!=null){P.get(0).unFix();} 		
	    		if(P.get(8)!=null){P.get(8).unFix();}
	    		if(P.get(72)!=null){P.get(72).unFix();}
	    		if(P.get(80)!=null){P.get(80).unFix();}
	    	} if(hanging == 1){
	    		if(P.get(0)!=null){P.get(0).fix();}	
	    		if(P.get(8)!=null){P.get(8).unFix();}
	    		if(P.get(72)!=null){P.get(72).unFix();}
	    		if(P.get(80)!=null){P.get(80).unFix();}
	    	} if(hanging == 2) {
	    		if(P.get(0)!=null){P.get(0).fix();} 		
	    		if(P.get(8)!=null){P.get(8).fix();}
	    		if(P.get(72)!=null){P.get(72).unFix();}
	    		if(P.get(80)!=null){P.get(80).unFix();}
	    	} if(hanging == 3) {
	    		if(P.get(0)!=null){P.get(0).fix();} 		
	    		if(P.get(8)!=null){P.get(8).fix();}
	    		if(P.get(72)!=null){P.get(72).fix();}
	    		if(P.get(80)!=null){P.get(80).unFix();}
	    	} if(hanging == 4) {
	    		if(P.get(0)!=null){P.get(0).fix();} 		
	    		if(P.get(8)!=null){P.get(8).fix();}
	    		if(P.get(72)!=null){P.get(72).fix();}
	    		if(P.get(80)!=null){P.get(80).fix();}
	    	}
        }
        
        
        
        time += dt;
    }

    /**
     * Displays Particle and Force objects.
     */
    public synchronized void display(GL2 gl)
    {
        for(Force force : F) {
            force.display(gl);
        }

        if(!init) init(gl);

        prog.useProgram(gl, true);

        for(Particle particle : P) {
            particle.display(gl);
        }

        prog.useProgram(gl, false);
    }
}
