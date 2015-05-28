package cs5643.particles;

/**
 * Default constants. Add your own as necessary.
 *
 * @author Doug James, January 2007
 * @author Eston Schweickart, February 2014
 */
public interface Constants
{
    /** Mass of a particle. */
    public static final double PARTICLE_MASS     = 1.0;

    /** Camera rotation speed constants. */
    public static final double CAM_SIN_THETA     = Math.sin(0.2);
    public static final double CAM_COS_THETA     = Math.cos(0.2);
    
    
    
    
    // Enables
    public static final boolean enableGravity = true;
    public static final boolean enableClickForce = true;
    public static final boolean enableBoundary = true;
    public static final boolean enableStretching = true;
    public static final boolean enableBending = true;
    public static final boolean enableBalloon = true;
    public static final boolean enableDamping = true;
    
    // Stiffness constants
    // Now stored in individual constraint/force classes, adjustable with sliders
  
    
    
    // Iterations for constraint functions (stretching and bending)
    public static final int solverIterations = 10;
}
