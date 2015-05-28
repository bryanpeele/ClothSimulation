package cs5643.particles;

import javax.media.opengl.*;

/**
 * Particle system constraint.
 *
 * @author Bryan Peele, March 2015
 */
public interface Constraint
{
	
	double k = 0; //constraint stiffness
	
    /**
     * Causes force to be applied to affected particles.
     */
    public void applyConstraint();

    /** Display any instructive force information, e.g., direction. */
    public void display(GL2 gl);

    /** Reference to the ParticleSystem this force affects. */
    public ParticleSystem getParticleSystem();
}
