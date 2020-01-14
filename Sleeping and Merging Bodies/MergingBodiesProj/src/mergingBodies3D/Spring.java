package mergingBodies3D;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import mintools.parameters.DoubleParameter;


public class Spring {

	/** The body to which this spring is attached */
	private RigidBody body1;
	/** The other body to which this spring is attached */
	private RigidBody body2;
	/** The point at which this spring is attached in body1 coordinates */
	private Point3d pb1 = new Point3d();
	/** The point at which this spring is attached in body2 coordinates */
	private Point3d pb2 = new Point3d();
	/** The point at which this spring is attached in world coordinates */
	private Point3d pb1W = new Point3d();
	/** The point at which this spring is attached in world coordinates */
	private Point3d pb2W = new Point3d();
	/** The point in the world to which this spring is attached */
	private Point3d pw = new Point3d();
	
	/** The original point in the world to which this spring is attached*/
	private Point3d pwo = new Point3d();

	enum SpringType {ZERO, WORLD, BODYBODY};
	private SpringType type; 
	
	/** spring stiffness, set to a default value */
	double k = 100; 
	/** spring damping, set to a default value */
	double d = 10;  
	
	/** 
	 * Rest length of the spring 
	 * TODO: better to make this a parameter.
	 */
	private double l0 = 0.5;

	/** Temporary working variables */
	private Vector3d displacement = new Vector3d();
	private Vector3d velocity = new Vector3d(); // velocity of the point on the body
	private Vector3d force = new Vector3d();
	

	/** 
	 * Creates a new body pinned to world spring.
	 * @param pB 	The attachment point in the body frame
	 * @param body	The body to which the spring should be attached
	 */
	public Spring(Point3d pB, RigidBody body) {
		type = SpringType.ZERO;
		this.body1 = body;
		this.pb1.set( pB );
		body.transformB2W.transform( pb1, pb1W );
		body.transformB2W.transform( pb1, pw );
		
		
	}
	
	/**
	 * Non-zero rest length spring with this constructor
	 * to a pinned location in the world.
	 * @param pB
	 * @param body
	 * @param pW
	 */
	public Spring( Point3d pB, RigidBody body, Point3d pW ) {
		type = SpringType.WORLD;
		this.body1 = body;
		this.pb1.set( pB );
		body.transformB2W.transform( pb1, pb1W );
		this.pw.set( pW );
		displacement.sub( pw, pb1W ); 
		l0 = displacement.length();
	
	}
	
	/**
	 * Body to body spring
	 * @param pB1
	 * @param body1
	 * @param pB2
	 * @param body2
	 */
	public Spring( Point3d pB1, RigidBody body1, Point3d pB2, RigidBody body2 ) {
		type = SpringType.BODYBODY;
		this.body1 = body1;
		this.body2 = body2;
		this.pb1.set( pB1 );
		body1.transformB2W.transform( pb1, pb1W );
		this.pb2.set( pB2 );
		body2.transformB2W.transform( pb2, pb2W );
		displacement.sub( pb1W, pb2W ); 
		l0 = displacement.length();
		
	}

	public void reset() {
		if (controllable) {
			pw.set(pwo);
			targetpW.set(pw);
		}
		switch (type) {
			case ZERO:
				body1.transformB2W.transform(pb1, pw);
				body1.transformB2W.transform(pb1, pb1W);
				break;
			case WORLD:
				body1.transformB2W.transform(pb1, pb1W);
				break;
			case BODYBODY:
				body1.transformB2W.transform(pb1, pb1W);
				body2.transformB2W.transform(pb2, pb2W);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Applies the spring force by adding a force and a torque to the body.
	 * If this body is in a collection, then it applies the force to *BOTH* the sub-body 
	 * and the collection.
	 * @param ks modulates the spring's stiffness
	 * @param cs modulates the spring's damping
	 */
	public void apply(double ks, double ds) {
		if (type == SpringType.BODYBODY)	
			applyTwoBodies(ks, ds);
		else
			applyOneBody(ks, ds);
	}

	/**
	 * Applies the spring force by adding a force and a torque to the body.
	 * If this body is in a collection, then it applies the force to *BOTH* the sub-body 
	 * and the collection.
	 * @param ks modulates the spring's stiffness
	 * @param cs modulates the spring's damping
	 */
	public void applyOneBody(double ks, double ds) {
			
		body1.transformB2W.transform( pb1, pb1W );
		displacement.sub( pw, pb1W ); 

		// Silly fix... the force should go gracefully to zero without giving NaNs :(
		if ( displacement.length() < 1e-3 ) return;  // hmm... should just give it some rest length?

		body1.getSpatialVelocity( pb1W, velocity );
		
		double scale = 
				- (k*ks * (displacement.length()  - l0) - d*ds * (velocity.dot(displacement) / displacement.length())) 
				/ displacement.length();

		force.scale( - scale, displacement );

		body1.applyForceW( pb1W, force );
		if ( body1.isInCollection() ) {
			body1.parent.applyForceW( pb1W, force );
		}
	}

	/**
	 * Applies the spring force by adding a force and a torque to the body.
	 * If this body is in a collection, then it applies the force to *BOTH* the sub-body 
	 * and the collection.
	 * @param ks modulates the spring's stiffness
	 * @param cs modulates the spring's damping
	 */
	public void applyTwoBodies(double ks, double ds) {
			
		body1.transformB2W.transform( pb1, pb1W );
		body2.transformB2W.transform( pb2, pb2W );
		displacement.sub( pb1W, pb2W ); 

		// Silly fix... the force should go gracefully to zero without giving NaNs :(
		if ( displacement.length() < 1e-3 ) return;  // hmm... should just give it some rest length?
		
		double scale = 
				- (k*ks * (displacement.length()  - l0)) 
				/ displacement.length();

		force.scale( scale, displacement );
		body1.applyForceW( pb1W, force );
		if ( body1.isInCollection() ) {
			body1.parent.applyForceW( pb1W, force );
		}
		
		force.scale( -scale, displacement );
		body2.applyForceW( pb2W, force );
		if ( body2.isInCollection() ) {
			body2.parent.applyForceW( pb2W, force );
		}
	}

	/**
	 * Draws the spring end points with a red line through them
	 * @param drawable
	 */
	public void displaySpring( GLAutoDrawable drawable ) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(2);
		gl.glColor4f(1, 0 ,0, 0.5f);
		gl.glBegin( GL.GL_LINES );
		
		if (type == SpringType.BODYBODY) {
			gl.glVertex3d( pb1W.x, pb1W.y, pb1W.z );	
			gl.glVertex3d( pb2W.x, pb2W.y, pb2W.z );	
		} else {
			gl.glVertex3d( pw.x, pw.y, pw.z );
			gl.glVertex3d( pb1W.x, pb1W.y, pb1W.z );	
		}
		gl.glEnd();
	}
	
	/** adjust the spring properties */
	public void moveWorldAttachmentAndRestLength( double dx, double dy, double dl ) {
		pw.x += dx;
		pw.y += dy;
		l0 += dl;
		if (l0 < 0 ) l0 = 0;
	}
	
	/** Boolean: true if spring is moveable with spring controls */
	public boolean controllable = false;
	
	/** Target point in world coordinates for our controllable springs */
	public Point3d targetpW = new Point3d();

	/** Parameters to help solve ODE to get controllable springs to desired position smoothly
	 * TODO: Should make these available to modify on interface
	 */
	private DoubleParameter animationStepSize = new DoubleParameter( "Anim step size", 0.005, 1e-4, 1 );
	private DoubleParameter animationk1= new DoubleParameter( "Anim stiffness", 100, 1, 1e6 );
	private DoubleParameter animationCDM = new DoubleParameter( "Anim critical damping multiplyer", 0.9, 0, 2);
	private DoubleParameter moveScale = new DoubleParameter("Step Movement Scale", 0.2, 0.1, 10);
	
	/** Vector that stores error in each direction*/
	private Vector3d err = new Vector3d();
	/** Velocity of moving pW */
	private Vector3d vpW = new Vector3d();
	
	/*
	 * Moves world coordinates of spring to desired target spoothly.
	 */
	public void moveSpring() {
		
		double h = animationStepSize.getValue();
    	double k1 = animationk1.getValue();
    	// tiny bit underdamped and we don't notice!
    	double k2 = 2*Math.sqrt(k1)*animationCDM.getValue(); // critical damping at sqrt(b^2- 4 k)
    	
    	err.sub(targetpW, pw);
    	
    	// x direction
    	vpW.x += h * ( k1 * err.x - k2 * vpW.x);
    	pw.x += h * vpW.x;
    	
    	// y direction
    	vpW.y += h * ( k1 * err.y - k2 * vpW.y);
    	pw.y += h * vpW.y;
    
    	//z direction
    	vpW.z += h * ( k1 * err.z - k2 * vpW.z);
    	pw.z += h * vpW.z;
    	
	}
	
	Vector3d temp = new Vector3d();
	/**
	 * Moves target position... called in LCPApp3d whenever WASDQE are pressed. 
	 * (Maybe with alt down)
	 * @param direction
	 */
	public void moveTargetpW(Vector3d direction) {
		temp.set(direction);
		double scale = moveScale.getValue();
		temp.scale(scale);
		targetpW.add(temp);
		
	}

	public void setTargetpW() {
		targetpW.set(pw);
		pwo.set(pw);
	}
	
}
