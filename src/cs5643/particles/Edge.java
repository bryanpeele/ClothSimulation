package cs5643.particles;

import javax.vecmath.Vector3d;

/**
 * A class specifying an oriented edge between two vertices of a mesh. The order of the vertices
 * matters; that is, if a Triangle t is specified with Vertex a, Vertex b, and Vertex c, then
 * the Edge e where e.v0==a and e.v1==b will have e.t0 == t. If instead the edge is specified
 * where e.v0=b and e.v1==a, then e.t1 == t. For example, in the figure below:
 *
 *        o-v1
 *       /|\
 *   t0-/ | \-t1
 *     /  |  \
 * v3-o   |-e o-v4
 *     \  |  / 
 *      \ | /
 *       \|/
 *        o-v0
 *
 * t0 would be a Triangle (v0, v1, v3), and t1 would be (v0, v4, v1).
 *
 * @author Eston Schweickart, February 2014
 */
public class Edge {
    
    /** The first vertex that defines this edge. */
    public Vertex v0;
    /** The second vertex that defines this edge. */
    public Vertex v1;
    
    /** The triangle on one side of the this edge. */
    public Triangle t0;
    /** The triangle on the other side of the this edge. */
    public Triangle t1;

    /** The length of this edge at rest. */
    public double restLength;

    // TODO: define other edge-specific parameters here.
    /** starting angle between two triangles t0 and t1*/
    public double phi0;

    /**
     * The constructor for an edge given the input vertices.
     * Pointers from the verties to this edge are automatically added,
     * but Triangle pointers must be set separately.
     */
    public Edge(Vertex v0, Vertex v1) {
	this.v0 = v0;
	this.v1 = v1;

	v0.edges.add(this);
	v1.edges.add(this);

	Vector3d edge = new Vector3d();
	edge.sub(v1.x0, v0.x0);
	restLength = edge.length();
    }

    /** Returns the current length of this edge. */
    public double length() {
	Vector3d edge = new Vector3d();
	edge.sub(v1.x, v0.x);
	return edge.length();
    }

    /** 
     * Given one of the end vertices of this edge, returns the other.
     * If the given vertex is not part of this edge, returns null instead.
     */
    public Vertex getOtherVertex(Vertex self) {
	if (self == v0) return v1;
	if (self == v1) return v0;
	return null;
    }
    
    

    
}