package cs5643.particles;

import java.io.*;
import java.util.Scanner;
import javax.vecmath.*;

/**
 * A static class that builds a Mesh from a specified Wavefront OBJ file. Can throw
 * a FileNotFoundException if the input file does not exist, or a BadMeshException if
 * the OBJ file is malformed. The following must hold, or an exception will be thrown: </br>
 * 
 * - The mesh must be manifold (i.e. an edge separates at most 2 faces). </br>
 * - All faces must be triangles. </br>
 * - All vertices must be specified before faces. </br>
 *
 * Only vertex and face definitions are supported by default. </br>
 *
 * @author Eston Schweickart, February 2014
 */

public class MeshBuilder {
    /**
     * Builds a Mesh object from a given OBJ file.
     *
     * @param file The OBJ file.
     * @param ps The ParticleSystem into which new vertices will be inserted.
     * @return The constructed Mesh.
     */

	private static Vector3d p1 = new Vector3d();
	private static Vector3d p2 = new Vector3d();
	private static Vector3d p3 = new Vector3d();
	private static Vector3d p4 = new Vector3d();
	private static Vector3d n1 = new Vector3d();
	private static Vector3d n2 = new Vector3d();
	private static Vector3d p2crossp3 = new Vector3d();
	private static Vector3d p2crossp4 = new Vector3d();
	private static Vector3d p1crossp2 = new Vector3d();
	private static double d;

	
	public static Mesh buildMesh(File file, ParticleSystem ps) throws FileNotFoundException, BadMeshException {
	Mesh result = new Mesh();
	Scanner s = new Scanner(file);
	boolean doneWithVertices = false;
	Edge[][] connectivity = new Edge[0][0];
	int line = 0;
	
	//Initialize inital volume of mesh to zero
	result.initialVolume = 0.0;

	while(s.hasNextLine()) {
	    String next = s.nextLine();
	    line++;
	    next = next.trim();
	    if (next.startsWith("#") || next.isEmpty()) {
		continue;
	    }
	    String[] tokens = next.split(" ");
	    
	    if(tokens[0].equals("v")) {
		if(doneWithVertices) {
		    throw new BadMeshException(line, "Please specify all vertices before faces.");
		}
		if(tokens.length != 4) {
		    throw new BadMeshException(line, "Malformed vertex definition.");
		}
		Point3d x0 = new Point3d();
		x0.x = Double.parseDouble(tokens[1]);
		x0.y = Double.parseDouble(tokens[2]);
		x0.z = Double.parseDouble(tokens[3]);
		Vertex p = new Vertex(x0);
		ps.P.add(p);
		result.vertices.add(p);

	    } else if(tokens[0].equals("f")) {
		int numVertices = result.vertices.size();
		if(!doneWithVertices) {
		    connectivity = new Edge[numVertices][numVertices];
		    doneWithVertices = true;
		}
		if(tokens.length != 4) {
		    throw new BadMeshException(line, "Only triangle meshes are supported.");
		}
		int[] indices = new int[3];
		// Subtract 1 to account for 1-based indexing *shudder*
		indices[0] = Integer.parseInt(tokens[1].split("/")[0]) - 1;
		indices[1] = Integer.parseInt(tokens[2].split("/")[0]) - 1;
		indices[2] = Integer.parseInt(tokens[3].split("/")[0]) - 1;
		if(indices[0] < 0 || indices[1] < 0 || indices[2] < 0) {
		    throw new BadMeshException(line, "Vertex indices must be greater than 0.");
		}
		if(indices[0] >= numVertices || indices[1] >= numVertices || indices[2] >= numVertices) {
		    throw new BadMeshException(line, "Please define all vertices before defining faces.");
		}
		
		Vertex v0 = result.vertices.get(indices[0]);
		Vertex v1 = result.vertices.get(indices[1]);
		Vertex v2 = result.vertices.get(indices[2]);
		Triangle t = new Triangle(v0, v1, v2);

		result.triangles.add(t);
		
		/** For each vertex, maintain a list connected triangles **/
		v0.triangles.add(t);
		v1.triangles.add(t);
		v2.triangles.add(t);
		
		/** Add up the new volume of each triangle **/	
		p1.set(t.v0.x0);
		p2.set(t.v1.x0);
		p3.set(t.v2.x0);
		p1crossp2.cross(p1,p2);
		result.initialVolume += p1crossp2.dot(p3);
		
		
		// Create/modify edges
		for(int i=0; i<3; i++) {
		    int index0 = indices[i];
		    int index1 = indices[(i+1)%3];
		
		    int smallIndex = index0 < index1 ? index0 : index1;
		    int bigIndex = index0 < index1 ? index1 : index0;

		    if(connectivity[smallIndex][bigIndex] == null) {
			connectivity[smallIndex][bigIndex] = new Edge(result.vertices.get(index0),
								      result.vertices.get(index1));
		    }
		    if (index0 < index1) {
			if (connectivity[smallIndex][bigIndex].t0 != null) {
			    throw new BadMeshException(line, "Make sure all triangles have the same"+
						       " orientation, and that the mesh is manifold.");
			}
			connectivity[smallIndex][bigIndex].t0 = t;
		    } else {
			if (connectivity[smallIndex][bigIndex].t1 != null) {
			    throw new BadMeshException(line, "Make sure all triangles have the same"+
						       " orientation, and that the mesh is manifold.");

			}
			connectivity[smallIndex][bigIndex].t1 = t;
		    }
		    
		    //Calculate initial angle for each edge
		    if(connectivity[smallIndex][bigIndex].t1 != null && connectivity[smallIndex][bigIndex].t0 != null){
		    	
		    	p1.set(connectivity[smallIndex][bigIndex].v0.x0);
		    	p2.set(connectivity[smallIndex][bigIndex].v1.x0);
		    	p3.set(connectivity[smallIndex][bigIndex].t0.getThirdVertex(connectivity[smallIndex][bigIndex].v0, connectivity[smallIndex][bigIndex].v1).x0);
		    	p4.set(connectivity[smallIndex][bigIndex].t1.getThirdVertex(connectivity[smallIndex][bigIndex].v0, connectivity[smallIndex][bigIndex].v1).x0);
		    	
		    	//Set p1 as origin
				p2.sub(p1);
				p3.sub(p1);
				p4.sub(p1);
				p1.set(0,0,0);
				
				//Get normals
				//Set normal, n1
				p2crossp3.cross(p2, p3);
				n1.normalize(p2crossp3);
				
				//Set normal, n2
				p2crossp4.cross(p2, p4);
				n2.normalize(p2crossp4);
				
				d = n1.dot(n2);
				if(d>1.0) {d = 1.0;}
				else if(d<-1.0) {d = -1.0;}
				
				//Set initial angle using arc cosine
				connectivity[smallIndex][bigIndex].phi0 = Math.acos(d);
		    }
		    
		}

	    } else {
		// TODO(Optional): support other OBJ properties.
		System.err.println("WARNING: unsupported mesh property: "+tokens[0]);
	    }
	}

	for(int i=0; i<connectivity.length; i++) {
	    for(int j=i+1; j<connectivity[i].length; j++) {
		if (connectivity[i][j] != null) {
		    result.edges.add(connectivity[i][j]);
		}
	    }
	}

	s.close();
	return result;
    }
    
    /** An Exception class for dealing with bad input meshes. */
    public static class BadMeshException extends Exception {
	public BadMeshException(String s) {
	    super(s);
	}
	
	public BadMeshException(int line, String s) {
	    super("Line " + line + ": " + s);
	}
    }
}
    