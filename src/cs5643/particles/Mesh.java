package cs5643.particles;

import java.util.ArrayList;
import javax.media.opengl.*;
import com.jogamp.opengl.util.glsl.*;

/**
 * A mesh data structure which may be displayed to the screen. The mesh is assumed
 * to be manifold and triangular, and all triangles are assumed to have coherent orientation.
 * The structure works as follows, where -> means "has pointers to": </br>
 *
 * Mesh     -> Triangle, Vertex, Edge </br>
 * Vertex   -> Edge, Triangle </br>                <<<< I added pointed to triangle inside of vertex
 * Edge     -> Vertex, Triangle </br>
 * Triangle -> Vertex </br>
 *
 * @author Eston Schweickart, February 2014
 */
public class Mesh {

    private boolean init = false;
    private boolean fallback = false;
    
    // TODO: you may not need to explicitly store vertices or edges here; remove these if you so choose.
    /** The vertices of the mesh. */
    public ArrayList<Vertex> vertices = new ArrayList<Vertex>();

    /** The edges of the mesh. */
    public ArrayList<Edge> edges = new ArrayList<Edge>();

    /** The faces of the mesh. */
    public ArrayList<Triangle> triangles = new ArrayList<Triangle>();

    /** Store the initial volume of mesh for use with balloon constraint **/
	public double initialVolume;
    
    /** Filename of the vertex shader source. */
    private static final String[] VERT_SOURCE = {"mesh-vert.glsl"};

    /** Filename of the fragment shader source. */
    private static final String[] FRAG_SOURCE = {"mesh-frag.glsl"};

    /** True if rendering using glsl shaders. Set to false to use fixed-function rendering. */
    private boolean useGLSL = true;

    /** The shader program used by the mesh. */
    private ShaderProgram prog;

    /** Gets ready to display the mesh; compiles programs, etc. */
    private void initDisplay(GL2 gl) {
	if(init || !useGLSL) return;

	
	
	prog = new ShaderProgram();
	ShaderCode vertCode = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(), VERT_SOURCE, false);
	ShaderCode fragCode = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(), FRAG_SOURCE, false);
	if (!prog.add(gl, vertCode, System.err) || !prog.add(gl, fragCode, System.err)) {
	    System.err.println("WARNING: shader did not compile");
	    useGLSL = false;
	} else {
	    prog.link(gl, System.err);
	}

	init = true;
    }

    /** Displays the mesh to the screen. */
    public void display(GL2 gl) {
	if(!init) initDisplay(gl);

	float[] cFront = {0f, 0.8f, 0.3f};
	float[] cBack = {0.8f, 0f, 0.3f};

	gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, cFront, 0);
	gl.glMaterialfv(GL2.GL_BACK,  GL2.GL_DIFFUSE, cBack , 0);

	if (useGLSL) {
	    prog.useProgram(gl, true);
	} else {	    
	    gl.glEnable(GL2.GL_LIGHTING);
	    gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
	}

	for(Triangle t : triangles) {
	    javax.vecmath.Vector3d n = t.getNormal();
	    gl.glNormal3d(n.x, n.y, n.z);
	    gl.glBegin(GL2.GL_TRIANGLES);
	    gl.glVertex3d(t.v0.x.x, t.v0.x.y, t.v0.x.z);
	    gl.glVertex3d(t.v1.x.x, t.v1.x.y, t.v1.x.z);
	    gl.glVertex3d(t.v2.x.x, t.v2.x.y, t.v2.x.z);
	    gl.glEnd();
	}

	if(useGLSL) {
	    prog.useProgram(gl, false);
	} else {
	    gl.glDisable(GL2.GL_LIGHTING);
	}

	// For debugging purposes- displays the edges of the mesh
	
	gl.glColor3f(0f, 0.8f, 0.3f);
	for(Edge e : edges) {
	    gl.glBegin(GL2.GL_LINES);
	    gl.glVertex3d(e.v0.x.x, e.v0.x.y, e.v0.x.z);
	    gl.glVertex3d(e.v1.x.x, e.v1.x.y, e.v1.x.z);
	    gl.glEnd();
	}
	
    }

    
}