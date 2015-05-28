package cs5643.particles;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.*;

/**
 * CS5643: Assignment #1: Smoothed-Particle Hydrodynamics
 *
 * main() entry point class that initializes ParticleSystem, OpenGL
 * rendering, and GUI that manages GUI/mouse events.
 *
 * Spacebar toggles simulation advance.
 *
 * @author Doug James, January 2007
 * @author Eston Schweickart, February 2014
 */
public class ParticleSystemBuilder implements GLEventListener
{
    public static final int k_pressure_init = 100;
    public static final int k_pressure_min = 0;
    public static final int k_pressure_max = 1000;
    public static final int k_pressure_stiffness_init = 90;
    public static final int k_pressure_stiffness_min = 0;
    public static final int k_pressure_stiffness_max = 100;
    public static final int k_stretching_init = 50;
    public static final int k_stretching_min = 0;
    public static final int k_stretching_max = 100;
    public static final int k_bending_init = 50;
    public static final int k_bending_min = 0;
    public static final int k_bending_max = 100;
    public static final int k_damping_init = 50;
    public static final int k_damping_min = 0;
    public static final int k_damping_max = 100;
    public static final int k_clicking_init = 1000;
    public static final int k_clicking_min = 100;
    public static final int k_clicking_max = 10000;
    public static final int gravity_init = 100;
    public static final int gravity_min = -200;
    public static final int gravity_max = 200;
    public static final int hanging_init = 0;
    public static final int hanging_min = 0;
    public static final int hanging_max = 4;
    
	public FloatBuffer model = Buffers.newDirectFloatBuffer(16);
	public FloatBuffer projection  = Buffers.newDirectFloatBuffer(16);
	public IntBuffer view  = Buffers.newDirectIntBuffer(16);
	
    
    private FrameExporter frameExporter;

    private static int N_STEPS_PER_FRAME = 500;

    private GLU glu;

    /** Default graphics time step size. */
    public static final double DT = 0.01;

    /** Main window frame. */
    JFrame frame = null;

    private int width, height;

    /** The single ParticleSystem reference. */
    ParticleSystem PS;

    /** The single Mesh reference. */
    Mesh MS;

    /** Object that handles all GUI and user interactions of building
     * Task objects, and simulation. */
    BuilderGUI     gui;

    /** Position of the camera. */
    public Point3d eyePos = new Point3d(14, 10, 10);

    /** Position of the camera's focus. */
    public Point3d targetPos = new Point3d(0.5, 0.5, 0.5);
    
    
    public Point3d RayStart = new Point3d();
    public Point3d RayEnd = new Point3d();
    
    /** Position of the light. Fixed at the location of the camera. */
    private float[] lightPos = {0f, 0f, 0f, 1f};
    
    

    /** Main constructor. Call start() to begin simulation. */
    ParticleSystemBuilder()
    {
        PS = new ParticleSystem();
        MS = new Mesh();
    }

    /**
     * Builds and shows windows/GUI, and starts simulator.
     */
    public void start()
    {
        if(frame != null) return;

        gui   = new BuilderGUI();

        frame = new JFrame("CS567 Particle System Builder");
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities glc = new GLCapabilities(glp);
        GLCanvas canvas = new GLCanvas(glc);
        canvas.addGLEventListener(this);
        frame.add(canvas);

        canvas.addMouseListener(gui);
        canvas.addMouseMotionListener(gui);
        canvas.addKeyListener(gui);

        final Animator animator = new Animator(canvas);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // make sure the call to Animator.stop() completes before
                // exiting
                new Thread(new Runnable() {
                    public void run() {
                        animator.stop();
                        System.exit(0);
                    }
                }).start();
            }
        });

        frame.pack();
        frame.setSize(950,950);
        frame.setLocation(400, 0);
        frame.setVisible(true);
        animator.start();
    }

    /** GLEventListener implementation: Initializes JOGL renderer. */
    public void init(GLAutoDrawable drawable)
    {
        // DEBUG PIPELINE (can use to provide GL error feedback... disable for speed)
        //drawable.setGL(new DebugGL(drawable.getGL()));

        GL2 gl = drawable.getGL().getGL2();
        System.err.println("INIT GL IS: " + gl.getClass().getName());

        gl.setSwapInterval(1);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glLineWidth(1);

        gl.glEnable(GL2.GL_NORMALIZE);

        // SETUP LIGHTING
        float[] lightAmbient = {0f, 0f, 0f, 1f};
        float[] lightDiffuse = {0.9f, 0.9f, 0.9f, 1f};
        float[] lightSpecular = {1f, 1f, 1f, 1f};

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);
        gl.glEnable(GL2.GL_LIGHT0);

        
  	

    }

    /** GLEventListener implementation */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    /** GLEventListener implementation */
    public void dispose(GLAutoDrawable drawable) {}

    /** GLEventListener implementation */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        System.out.println("width="+width+", height="+height);
        height = Math.max(height, 1); // avoid height=0;

        this.width  = width;
        this.height = height;

        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0,0,width,height);

        /** Get all the matrices needed for gluUnProject **/
    	gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, model);
    	gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projection);
    	gl.glGetIntegerv(GL2.GL_VIEWPORT, view);
    	System.out.println("m-p-v initialized");
        
        
    }


    /**
     * Main event loop: OpenGL display + simulation
     * advance. GLEventListener implementation.
     */
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.1f,0.1f,0.2f,1f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        /// GET READY TO DRAW:
        // Revised for correction
        gl.glMatrixMode(GL2.GL_PROJECTION);
        if (glu == null) glu = GLU.createGLU();
        gl.glLoadIdentity();
        
        glu.gluPerspective(5, (float)width/height, 1, 100);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(eyePos.x, eyePos.y, eyePos.z, targetPos.x, targetPos.y, targetPos.z, 0, 1, 0);
        
        /// DRAW COMPUTATIONAL CELL BOUNDARY:
        gl.glColor3f(1, 0, 0);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(0, 0, 0);   gl.glVertex3d(1, 0, 0);   gl.glVertex3d(1, 1, 0);  gl.glVertex3d(0, 1, 0);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(0, 0, 1);   gl.glVertex3d(1, 0, 1);   gl.glVertex3d(1, 1, 1);  gl.glVertex3d(0, 1, 1);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(0, 0, 0);   gl.glVertex3d(0, 0, 1);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(1, 0, 0);   gl.glVertex3d(1, 0, 1);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(1, 1, 0);   gl.glVertex3d(1, 1, 1);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(0, 1, 0);   gl.glVertex3d(0, 1, 1);
        gl.glEnd();
        
        if(ClickForce.p != null) {
        	gl.glColor3f(0, 1, 1);
            gl.glPushMatrix();
            gl.glTranslated(ClickForce.p.x.x, ClickForce.p.x.y, ClickForce.p.x.z);
            GLU glu = GLU.createGLU();
            GLUquadric quadric = glu.gluNewQuadric();
            glu.gluSphere(quadric, 0.02, 16, 8);
            gl.glEndList();
            gl.glPopMatrix();
        	
        }
        

        /** Get all the matrices needed for gluUnProject **/
    	gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, model);
    	gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projection);
    	gl.glGetIntegerv(GL2.GL_VIEWPORT, view);

        
        
        /// SIMULATE/DISPLAY HERE (Handled by BuilderGUI):
        gui.simulateAndDisplayScene(gl);
    }

    /** Interaction central: Handles windowing/mouse events, and building state. */
    class BuilderGUI implements MouseListener, MouseMotionListener, KeyListener
    {
        boolean simulate = false;

        /** Current build task (or null) */
        Task task;

        JFrame  guiFrame;
        TaskSelector taskSelector = new TaskSelector();

        BuilderGUI()
        {
            guiFrame = new JFrame("Tasks");
            guiFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            guiFrame.setLayout(new SpringLayout());
            guiFrame.setLayout(new GridLayout(19,1));

            /* Add new task buttons here, then add their functionality below. */
            ButtonGroup      buttonGroup  = new ButtonGroup();
            AbstractButton[] buttons      = { new JButton("Reset"),
                new JButton("Load File"),
                new JToggleButton("Grab Particle", false),
                //new JToggleButton ("Create Particle", false),
                //new JToggleButton ("[Some Other Task]", false),
            };

            for(int i=0; i<buttons.length; i++) {
                buttonGroup.add(buttons[i]);
                guiFrame.add(buttons[i]);
                buttons[i].addActionListener(taskSelector);
            }
            /*** Test to add slider **/
            // TODO make it all work
            // reference: http://docs.oracle.com/javase/tutorial/uiswing/components/slider.html
            //  http://www.java2s.com/Code/JavaAPI/javax.swing/JSlideraddChangeListenerChangeListenerl.htm
            JLabel pressureLabel = new JLabel("Pressure", JLabel.CENTER);
            JSlider pressureSelect = new JSlider(JSlider.HORIZONTAL,k_pressure_min, k_pressure_max, k_pressure_init);
            pressureSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = pressureSelect.getValue();
                    Balloon.k_pressure = value/100.0;
                    System.out.println(Balloon.k_pressure);
                    
                  }
                });
            guiFrame.add(pressureLabel);
            guiFrame.add(pressureSelect);

            JLabel pressureStiffnessLabel = new JLabel("Pressure Stiffness", JLabel.CENTER);
            JSlider pressureStiffnessSelect = new JSlider(JSlider.HORIZONTAL,k_pressure_stiffness_min, k_pressure_stiffness_max, k_pressure_stiffness_init);
            pressureStiffnessSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = pressureStiffnessSelect.getValue();
                    Balloon.k_pressure_stiffness = value/100.0;
                    System.out.println(Balloon.k_pressure_stiffness);
                    
                  }
                });
            guiFrame.add(pressureStiffnessLabel);
            guiFrame.add(pressureStiffnessSelect);
            
            JLabel stretchLabel = new JLabel("Stretch Stiffness", JLabel.CENTER);
            JSlider stretchSelect = new JSlider(JSlider.HORIZONTAL,k_stretching_min, k_stretching_max, k_stretching_init);
            stretchSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = stretchSelect.getValue();
                    Stretching.k_stretching = value/100.0;
                    System.out.println(Stretching.k_stretching);
                    
                  }
                });
            guiFrame.add(stretchLabel);
            guiFrame.add(stretchSelect);
            
            JLabel bendingLabel = new JLabel("Bending Stiffness", JLabel.CENTER);
            JSlider bendingSelect = new JSlider(JSlider.HORIZONTAL,k_bending_min, k_bending_max, k_bending_init);
            bendingSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = bendingSelect.getValue();
                    Bending.k_bending = value/100.0;
                    System.out.println(Bending.k_bending);
                    
                  }
                });
            guiFrame.add(bendingLabel);
            guiFrame.add(bendingSelect);
            

            JLabel dampingLabel = new JLabel("Damping", JLabel.CENTER);
            JSlider dampingSelect = new JSlider(JSlider.HORIZONTAL,k_damping_min, k_damping_max, k_damping_init);
            dampingSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = dampingSelect.getValue();
                    Damping.k_damping = value/100.0;
                    System.out.println(Damping.k_damping);
                    
                  }
                });
            guiFrame.add(dampingLabel);
            guiFrame.add(dampingSelect);
            
            
            JLabel clickingLabel = new JLabel("Clicking Stiffness", JLabel.CENTER);
            JSlider clickingSelect = new JSlider(JSlider.HORIZONTAL,k_clicking_min, k_clicking_max, k_clicking_init);
            clickingSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = clickingSelect.getValue();
                    ClickForce.k_clicking = value;
                    System.out.println(ClickForce.k_clicking);
                    
                  }
                });
            guiFrame.add(clickingLabel);
            guiFrame.add(clickingSelect);
            
            JLabel gravityLabel = new JLabel("Gravity", JLabel.CENTER);
            JSlider gravitySelect = new JSlider(JSlider.HORIZONTAL,gravity_min, gravity_max, gravity_init);
            gravitySelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = gravitySelect.getValue();
                    Gravity.gravity = value/10.0;
                    System.out.println(Gravity.gravity);
                  }
                });
            guiFrame.add(gravityLabel);
            guiFrame.add(gravitySelect);
            
            
            JLabel hangingLabel = new JLabel("Hanging Points", JLabel.CENTER);
            JSlider hangingSelect = new JSlider(JSlider.HORIZONTAL,hanging_min, hanging_max, hanging_init);
            hangingSelect.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = hangingSelect.getValue();
                    ParticleSystem.hanging = value;
                    System.out.println(ParticleSystem.hanging);
                  }
                });
            guiFrame.add(hangingLabel);
            guiFrame.add(hangingSelect);
            
            guiFrame.setSize(200,800);
            guiFrame.pack();
            guiFrame.setVisible(true);

            task = null; // Set default task here
        }

        /** Simulate then display particle system and any builder
         * adornments. */
        void simulateAndDisplayScene(GL2 gl)
        {

            if(simulate) {
                if(true) {//ONE EULER STEP
                    PS.advanceTime(DT);
                }

            }

            // Draw particles, forces, etc.
            MS.display(gl);

            if(simulate && frameExporter != null) {
                frameExporter.writeFrame(gl);
            }

            // Display task if any
            if(task != null) task.display(gl);
        }

        /**
         * ActionListener implementation to manage Task selection
         * using (radio) buttons.
         */
        class TaskSelector implements ActionListener
        {
            /**
             * Resets ParticleSystem to undeformed/material state,
             * disables the simulation, and removes the active Task.
             */
            void resetToRest() {
                PS.reset();//synchronized
                simulate = false;
                task = null;
            }

            /** Creates new Task objects to handle specified button action.
             *  Switch to a new task, or perform custom button actions here.
             */
            public void actionPerformed(ActionEvent e)
            {
                String cmd = e.getActionCommand();
                System.out.println(cmd);

                if(cmd.equals("Reset")) {
                    if(task != null) {
                        task.reset();
                    } else {
                        resetToRest(); // set task=null
                    }
                }
                else if(cmd.equals("Grab Particle")){
                    task = new GrabParticleTask();
                }
                else if(cmd.equals("Load File")){
                    loadFrameFromFile();
                }
                else {
                    System.out.println("UNHANDLED ActionEvent: "+e);
                }
            }
        }

        // Methods required for the implementation of MouseListener
        public void mouseEntered (MouseEvent e) { if(task!=null) task.mouseEntered(e);  }
        public void mouseExited  (MouseEvent e) { if(task!=null) task.mouseExited(e);   }
        public void mousePressed (MouseEvent e) { if(task!=null) task.mousePressed(e);  }
        public void mouseReleased(MouseEvent e) { if(task!=null) task.mouseReleased(e); }
        public void mouseClicked (MouseEvent e) { if(task!=null) task.mouseClicked(e);  }

        // Methods required for the implementation of MouseMotionListener
        public void mouseDragged (MouseEvent e) { if(task!=null) task.mouseDragged(e);  }
        public void mouseMoved   (MouseEvent e) { if(task!=null) task.mouseMoved(e);    }

        // Methods required for the implementation of KeyListener
        public void keyTyped(KeyEvent e) { } // NOP
        public void keyPressed(KeyEvent e) { dispatchKey(e); }
        public void keyReleased(KeyEvent e) { } // NOP

        /**
         * Handles keyboard events, e.g., spacebar toggles
         * simulation/pausing, and escape resets the current Task.
         */
        public void dispatchKey(KeyEvent e)
        {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    simulate = !simulate;
                    if(simulate) {
                        System.out.println("Starting simulation...");
                    }
                    else {
                        System.out.println("Simulation paused.");
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    taskSelector.resetToRest(); //sets task=null;
                    break;
                case KeyEvent.VK_E:
                    frameExporter = ((frameExporter==null) ? (new FrameExporter()) : null);
                    System.out.println("'e' : frameExporter = "+frameExporter);
                    break;
                case KeyEvent.VK_I:
                    frameExporter = ((frameExporter==null) ? (new FrameExporter(true)) : null);

                    System.out.println("'i' : frameExporter = "+frameExporter);
                    break;
                case KeyEvent.VK_L:
                    loadFrameFromFile();
                    break;
                case KeyEvent.VK_EQUALS:
                    N_STEPS_PER_FRAME = Math.max((int)(1.05*N_STEPS_PER_FRAME), N_STEPS_PER_FRAME+1);
                    System.out.println("N_STEPS_PER_FRAME="+N_STEPS_PER_FRAME+";  dt="+(DT/(double)N_STEPS_PER_FRAME));
                    break;
                case KeyEvent.VK_MINUS:
                    int n = Math.min((int)(0.95*N_STEPS_PER_FRAME), N_STEPS_PER_FRAME-1);
                    N_STEPS_PER_FRAME = Math.max(1, n);
                    System.out.println("N_STEPS_PER_FRAME="+N_STEPS_PER_FRAME+";  dt="+(DT/(double)N_STEPS_PER_FRAME));
                    break;
                case KeyEvent.VK_LEFT:
                    Vector2d vec = new Vector2d(eyePos.x-targetPos.x, eyePos.z-targetPos.z);
                    eyePos.x = vec.x*Constants.CAM_COS_THETA - vec.y*Constants.CAM_SIN_THETA + targetPos.x;
                    eyePos.z = vec.x*Constants.CAM_SIN_THETA + vec.y*Constants.CAM_COS_THETA + targetPos.z;
                    break;
                case KeyEvent.VK_RIGHT:
                    vec = new Vector2d(eyePos.x-targetPos.x, eyePos.z-targetPos.z);
                    eyePos.x = vec.x*Constants.CAM_COS_THETA + vec.y*Constants.CAM_SIN_THETA + targetPos.x;
                    eyePos.z = -vec.x*Constants.CAM_SIN_THETA + vec.y*Constants.CAM_COS_THETA + targetPos.z;
                    break;

                case KeyEvent.VK_UP:
                    eyePos.y += 1;
                    break;
                case KeyEvent.VK_DOWN:
                    eyePos.y -= 1;
                    break;

                default:
            }
        }

        /**
         * "Task" command base-class extended to support
         * building/interaction via mouse interface.  All objects
         * extending Task are implemented here as inner classes for
         * simplicity.
         *
         * Add tasks as necessary for different interaction modes.
         */
        abstract class Task implements MouseListener, MouseMotionListener
        {
            /** Displays any task-specific OpengGL information,
             * e.g., highlights, etc. */
            public void display(GL2 gl) {}

            // Methods required for the implementation of MouseListener
            public void mouseEntered (MouseEvent e) {}
            public void mouseExited  (MouseEvent e) {}
            public void mousePressed (MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseClicked (MouseEvent e) {}

            // Methods required for the implementation of MouseMotionListener
            public void mouseDragged (MouseEvent e) {}
            public void mouseMoved   (MouseEvent e) {}

            /** Override to specify reset behavior during "escape" button
             * events, etc. */
            abstract void reset();

        }

        
        /** Clicking task that applies . */
        class GrabParticleTask extends Task
        {
        	private Vector3d clicked = new Vector3d();
        	private Particle selected;
        	public FloatBuffer nearBuffer = Buffers.newDirectFloatBuffer(16);
        	public FloatBuffer farBuffer = Buffers.newDirectFloatBuffer(16);

        	Vector3d x0 = new Vector3d();
        	Vector3d x1 = new Vector3d();
        	Vector3d x2 = new Vector3d();
        	Vector3d x2minusx1 = new Vector3d();
        	Vector3d x1minusx0 = new Vector3d();
        	Vector3d crossProd = new Vector3d();
        	
            public void mousePressed (MouseEvent e) {
                selected = findClosest(e.getX(), e.getY());
                //System.out.println(selected);
                
                if (selected!=null){
                	selected.setHighlight(true);
                	ClickForce.p = selected;	
                }
                //System.out.println(ClickForce.p);
            }

            public void mouseReleased (MouseEvent e) {
            	ClickForce.p.setHighlight(false);
            	ClickForce.p = null;
            }
            
            public void mouseDragged (MouseEvent e) {
            	Vector3d nearPoint = new Vector3d();
            	Vector3d farPoint = new Vector3d();
            	
            	double t;
            	
            	double x = e.getX();
            	double y = e.getY();
            	y = view.get(3) - y;
            	
            	glu.gluUnProject((float) x,(float) y,(float) 0.0,model,projection,view,nearBuffer);
            	glu.gluUnProject((float) x,(float) y,(float) 1.0,model,projection,view,farBuffer);
            	nearPoint.x = nearBuffer.get(0);
            	nearPoint.y = nearBuffer.get(1);
            	nearPoint.z = nearBuffer.get(2);
            	farPoint.x = farBuffer.get(0);
            	farPoint.y = farBuffer.get(1);
            	farPoint.z = farBuffer.get(2);
            	
            	x1.set(nearPoint);
            	x2.set(farPoint);
            	x1minusx0.sub(x1,x0);
            	x2minusx1.sub(x2,x1);
            	
            	t = -x1minusx0.dot(x2minusx1);
            	t = t/(x2minusx1.lengthSquared());
            	
            	ClickForce.goal.scaleAdd(t,x2minusx1,x1);
            	
            	
            }
           
            private Particle findClosest(int x, int y) {
            	Vector3d nearPoint = new Vector3d();
            	Vector3d farPoint = new Vector3d();
           	
            	// TODO: references
            	// http://gamedev.stackexchange.com/questions/71472/using-gluunproject-to-transform-mouse-position-to-world-coordinates-lwjgl
            	// http://stackoverflow.com/questions/3746759/java-bufferutil
            	         	
            	//x = frame.getHeight() - x;
            	//y = frame.getHeight() - y;
            	y = view.get(3) - y;
            	
            	glu.gluUnProject((float) x,(float) y,(float) 0.0,model,projection,view,nearBuffer);
            	glu.gluUnProject((float) x,(float) y,(float) 1.0,model,projection,view,farBuffer);
            	nearPoint.x = nearBuffer.get(0);
            	nearPoint.y = nearBuffer.get(1);
            	nearPoint.z = nearBuffer.get(2);
            	farPoint.x = farBuffer.get(0);
            	farPoint.y = farBuffer.get(1);
            	farPoint.z = farBuffer.get(2);
            
            	
            	Particle closest = null;
            	double minDistance = 99999;

            	//TODO distance reference
            	//http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
            	

            	double distanceSq;
            	
            	x1.set(nearPoint);
            	x2.set(farPoint);
            	
            	for(Particle p: PS.P) {
            		
            		x0.set(p.x);
            		x2minusx1.sub(x2,x1);
            		x1minusx0.sub(x1,x0);
            		crossProd.cross(x2minusx1,x1minusx0);
            		
            		distanceSq = crossProd.lengthSquared()/x2minusx1.lengthSquared();
            		
            		if (distanceSq < minDistance) {
            			minDistance = distanceSq;
            			closest = p;
            		}
            	}
            	
            	return closest;
            	
            }
            
            void reset() {
                taskSelector.resetToRest(); //sets task=null;
            }
        }
        
        
    }

    /**
     * Displays a filechooser, and then loads a frame file.
     * Files are expected to be in the same format as those exported by the
     * FrameExporter class.
     */
    private void loadFrameFromFile()
    {
        JFileChooser fc = new JFileChooser("./frames");
        int choice = fc.showOpenDialog(frame);
        if (choice != JFileChooser.APPROVE_OPTION) return;
        String fileName = fc.getSelectedFile().getAbsolutePath();

        java.io.File file = new java.io.File(fileName);
        if (!file.exists()) {
            System.err.println("Error: Tried to load a frame from a non-existant file.");
            return;
        }

        try {

        	MS = MeshBuilder.buildMesh(file, PS);
        	if(Constants.enableGravity) PS.addForce(new Gravity(PS));
        	if(Constants.enableClickForce) PS.addForce(new ClickForce(PS));
        	if(Constants.enableBoundary) PS.addConstraint(new Boundary(PS));
        	if(Constants.enableStretching) PS.addConstraint(new Stretching(PS,MS));
        	if(Constants.enableBending) PS.addConstraint(new Bending(PS,MS));
        	if(Constants.enableBalloon) PS.addConstraint(new Balloon(PS,MS));
        	if(Constants.enableDamping) PS.addDamping(new Damping(PS));
        	

        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("OOPS: "+e);
        }
    }

    /// Used by the FrameExporter class
    private static int exportId = -1;

    /**
     * A class that either writes the current position of all particles to a text file,
     * or outputs a png of the current window. Toggle the image boolean to switch modes.
     *
     * Text file specification:
     * The file's first line is an integer N denoting the number of particles in the system.
     * N lines follow, each with 3 floating point numbers describing the points'
     * x, y, and z coordinates.
     *
     * WARNING: the directory "./frames/" must exist for this class to work properly.
     */
    private class FrameExporter
    {
        public boolean image = false;
        private int nFrames  = 0;

        FrameExporter()  {
            exportId += 1;
        }

        FrameExporter(boolean image) {
            this.image = image;
            exportId += 1;
        }

        void writeFrame(GL2 gl)
        {
            long   timeNS   = -System.nanoTime();
            String number   = Utils.getPaddedNumber(nFrames, 5, "0");
            String filename = "frames/export"+exportId+"-"+number+
                (image ? ".png" : ".txt");/// Bug: DIRECTORY MUST EXIST!

            try{
                java.io.File   file     = new java.io.File(filename);
                if(file.exists()) System.out.println("WARNING: OVERWRITING PREVIOUS FILE: "+filename);

                if (image) {
                    GLReadBufferUtil rbu = new GLReadBufferUtil(false, false);
                    rbu.readPixels(gl, false);
                    rbu.write(file);
                } else {
                    java.io.BufferedWriter output = new java.io.BufferedWriter(new java.io.FileWriter(file));

                    output.write(""+PS.P.size()+"\n");
                    for (Particle p : PS.P) {
                        output.write(""+p.x.x+" "+p.x.y+" "+p.x.z+"\n");
                    }
                    output.close();
                }

                System.out.println((timeNS/1000000)+"ms:  Wrote frame: "+filename);

            }catch(Exception e) {
                e.printStackTrace();
                System.out.println("OOPS: "+e);
            }

            nFrames += 1;
        }
    }

    /**
     * ### Runs the ParticleSystemBuilder. ###
     */
    public static void main(String[] args)
    {
        try{
            ParticleSystemBuilder psb = new ParticleSystemBuilder();
            psb.start();

        }catch(Exception e) {
            e.printStackTrace();
            System.out.println("OOPS: "+e);
        }
    }
}
