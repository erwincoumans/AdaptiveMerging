package mergingBodies3D;

import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import mergingBodies3D.Merging.MergeParameters;
import mergingBodies3D.Contact.ContactState;
import mintools.viewer.FancyAxis;

/**
 * Simple 2D rigid body based on image samples
 * @author kry
 */
public class RigidBody {
    
	/** Bodies can have names, as specified in XML, but not needed for anything yet except conveience */
	String name;
	
	/** 
	 * Used for collections 
	 */
	RigidCollection parent;
	
    /** Variable to keep track of identifiers that can be given to rigid bodies */
    static public int nextIndex = 0;
	
    RigidBodyGeom geom;
        
    BVNode root;
    
    /** accumulator for forces acting on this body */
    Vector3d force = new Vector3d();
    
    /** accumulator for torques acting on this body */
    Vector3d torque = new Vector3d();
    
    /** rotational inertia in rest pose with rotation equal to identity */
    protected Matrix3d massAngular0 = new Matrix3d();
    /** rotational inertia in the current pose */
    Matrix3d massAngular = new Matrix3d();
    
    double massLinear;
        
    public boolean pinned = false;
   
    /** non-pinned bodies will be generated by the factory unless this flag is set to false */
    public boolean factoryPart = true;
    
    // TODO:  RigidBody improvements... 
    // Use a Vector6d for (v,omega)
    // Update RigidTransform to take theta and x as backing memory, and have rigid body extend RigidTransform so as not to duplicate memory
    // RigidTransform can have transform and inverseTransform methods for points, vectors, and 6D vectors
    
    /** Transforms points in Body coordinates to World coordinates */
    public RigidTransform transformB2W = new RigidTransform();
    
    /** Transforms points in World coordinates to Body coordinates */
    public RigidTransform transformW2B = new RigidTransform();
    
    
	/**
	 * TODO:  Somewhat wasted memory???  
	 * Transforms points in body coordinates to collection coordinates, if a
	 * collection exists
	 */
	RigidTransform transformB2C = new RigidTransform();

	/**
	 * Transforms points in collection coordinates to body coordinates, if a
	 * collection exists
	 */
	RigidTransform transformC2B = new RigidTransform();
	
	/**
	 * list of contacting bodies present with this RigidBody. In case of a collection, the list will contain
	 * both internal and external bpc.
	 * We may want to split this list in two parts...? (external/internal)
	 **/
	public HashSet<BodyPairContact> bodyPairContacts = new HashSet<BodyPairContact>();

    /** DeltaV for PGS resolution */
    Vector6d deltaV = new Vector6d();
    
    /** linear velocity */
    public Vector3d v = new Vector3d();

    /** initial linear velocity */
    public Vector3d v0 = new Vector3d();
    
    /** Position of center of mass in the world frame */
    public Point3d x = new Point3d();
    
    /** initial position of center of mass in the world frame */
    Point3d x0 = new Point3d();
    
    /** orientation of the body TODO: refactor to be R later?? */
    public Matrix3d theta = new Matrix3d();
    /**
     * inverse orientation (i.e., world to body)  TODO: RIGIDTRANSFORM: we shouldn't be storing this! :(
     * This is primarily a temporary working variable!
     */
    public Matrix3d thetaT = new Matrix3d();

    /** orientation of the body TODO: refactor to be R later?? */
    public Matrix3d theta0 = new Matrix3d();
    
    /** angular velocity in radians per second */
    public Vector3d omega = new Vector3d();    
    
    /** initial angular velocity in radians per second */
    public Vector3d omega0 = new Vector3d();

    /** inverse of the linear mass, or zero if pinned */
    double minv;
    
    /** inverse of the angular mass, or zero if pinned, for current pose */
    Matrix3d jinv = new Matrix3d();
    
    /** inverse of the angular mass, or zero if pinned, for REST pose */
    Matrix3d jinv0 = new Matrix3d();
    
	/** 
	 * bounding box, in the body frame (all 8 points)
	 * the purpose of this is to bound maximum velocity relative to another
	 * body so we might want to do something specific or different for special geometry.
	 */
	public ArrayList<Point3d> boundingBoxB = new ArrayList<Point3d>(); 
	
	/** Friction coefficient */
	public double friction = 0.8; 
	
	/** Restitution coefficient */
	public double restitution = 0; 
	
	public boolean isSleeping = false;

	/** true if body is a magnetic */
	public boolean magnetic = false;
	
	/** true if magnetic field is activate */
	public boolean activateMagnet = false;
    
	/** 
	 * A trivial bounding sphere about the COM
	 * currently only updated for box geometries!
	 */
	public double radius;
	
	public float[] col;
	
	/**
	 * keeps track of the activity of the last N steps, if it is false, means that
	 * should be asleep, if true, should be awake
	 */
	public ArrayList<Double> metricHistory = new ArrayList<Double>();

	MotionMetricProcessor motionMetricProcessor = new MotionMetricProcessor();

	/** Empty constructor needed in some special cases... use carefully! */
	protected RigidBody() {}
	
	/**
	 * Constructs a new rigid body
	 * @param massLinear  can be zero if pinned
	 * @param massAngular can be null if pinned (can also be null if you plan to fix it later, e.g., composite bodies)
	 * @param pinned
	 * @param boundingBoxB can be null if pinned
	 */
	public RigidBody( double massLinear, Matrix3d massAngular, boolean pinned, ArrayList<Point3d> boundingBoxB ) {
		this.pinned = pinned;
        if ( pinned ) {
        	this.massLinear = 0;
        	this.minv = 0;
        	this.massAngular.setZero();
        	this.massAngular0.setZero();
        	this.jinv0.setZero();
        	this.jinv.setZero();        
        } else {
        	this.boundingBoxB.addAll( boundingBoxB );
        	this.massLinear = massLinear;
        	this.minv = 1.0 / massLinear;
        	if ( massAngular != null ) {
	            this.massAngular0.set( massAngular );
	            this.massAngular.set( massAngular );            
	            this.jinv0.invert(massAngular0);
	            this.jinv.set( jinv0 );
        	}
        } 
        theta.setIdentity();
        theta0.setIdentity();
	}
	
    /**
     * Creates a copy of the provided rigid body 
     * @param body
     */
    public RigidBody( RigidBody body ) {
        massLinear = body.massLinear;
        minv = body.minv;
        massAngular.set( body.massAngular );
        massAngular0.set( body.massAngular0 );
        jinv.set( body.jinv );
        jinv0.set( body.jinv0 );

        x0.set( body.x0 );
        x.set( body.x );
        theta.set( body.theta );
        theta0.set( body.theta0 );
        omega.set( body.omega );
        
		boundingBoxB = new ArrayList<Point3d>(body.boundingBoxB);
        // we can share the blocks and boundary blocks...
        // no need to update them as they are in the correct body coordinates already        
        updateTransformations();
        if ( body.root != null ) {
        	root = new BVNode( body.root, this ); // create a copy
        }
        pinned = body.pinned;
        geom = body.geom; 
        friction = body.friction;
        restitution = body.restitution;
        radius = body.radius;
        
        col = body.col; // this can be shared memory!
    }
    
	/**
	 * Clear deltaV, force and torque
	 */
	public void clear() {
		force.set(0,0,0);
		torque.set(0,0,0);
		deltaV.setZero();
	}
    	
	public boolean isInCollection() {
		return (parent!=null);
	}
	
	public boolean isInSameCollection(RigidBody body) {
		return (parent!=null && parent==body.parent);
	}
	
	public boolean isInCollection(RigidCollection collection) {
		return ( parent == collection );
	}
	
	public void wake() {
		if (isSleeping) {
	    	isSleeping = false;
	    	metricHistory.clear();
	    }
		
		if (isInCollection() && parent.isSleeping)
	    	parent.wake();
	}
	
    /**
     * Updates the B2W and W2B transformations
     */
    public void updateTransformations() {
        transformB2W.set( theta, x );
        thetaT.transpose(theta);
        tmp.scale(-1,x);
        thetaT.transform(tmp);
        transformW2B.set( thetaT, tmp );

        // might be done more often than necessary, but need to have 
        // rotational inertia updated give we are storing information in a 
        // world aligned frame... note that the non-inverted angular inertia
        // used for energy computation and for the corriollis force (disabled)
        // also used by composite bodies at the time of their creation 
        if ( ! pinned ) {
	        massAngular.mul( theta, massAngular0 );
	        massAngular.mul( thetaT );
	        jinv.mul( theta, jinv0 );
	        jinv.mul( thetaT );
        } 
    }
    
    /**
     * Apply a contact force specified in world coordinates at the specified point.
     * @param contactPointW
     * @param contactForceW
     */
    public void applyForceW( Point3d contactPointW, Vector3d contactForceW ) {
        force.add( contactForceW );        
        tmp.sub( contactPointW, x );
        tmp2.cross( tmp,  contactForceW );
        torque.add( tmp2 );
    }
    
    /**
     * Adds to the torque a corriolis term (J omega) * omegacross
     */
    public void applyCoriollisTorque() {
    	if ( !pinned ) {
	    	massAngular.transform( omega, tmp );
	    	tmp2.cross( tmp, omega );
	    	// TODO: CORRIOLIS: this is certainly broken :( Check that this is correct!! And make more efficient?
	    	//torque.sub(tmp2); // seems like this should be added, but could also be a sign error. :(
    	}
    }
    
    /**
     * Advances the body state using symplectic Euler, first integrating accumulated force and torque 
     * (which are then set to zero), and then updating position and angle.  The internal rigid transforms
     * are also updated. 
     * @param dt step size
     */
    public void advanceTime( double dt ) {
        if ( !pinned ) {   
			advanceVelocities(dt);
			advancePositions(dt);
        }        
    }
    
	/**
	 * Given normalized R^3 vector of rotation w, we compute exp([w]t) using
	 * Rodrigues' formula:
	 * 
	 * exp([w]t) = I + [w] sin(t) + [w](1-cos(t)).
	 * 
	 * @param R :=
	 *            exp([w]t)
	 * @param w
	 *            Normalized 3D vector.
	 * @param t
	 *            Step size (in radians).
	 */
	private static void expRodrigues(Matrix3d R, Vector3d w, double t) {
		double wX = w.x;
		double wY = w.y;
		double wZ = w.z;
		double c = Math.cos(t);
		double s = Math.sin(t);
		double c1 = 1 - c;

		R.m00 = c + wX * wX * c1;
		R.m10 = wZ * s + wX * wY * c1;
		R.m20 = -wY * s + wX * wZ * c1;

		R.m01 = -wZ * s + wX * wY * c1;
		R.m11 = c + wY * wY * c1;
		R.m21 = wX * s + wY * wZ * c1;

		R.m02 = wY * s + wX * wZ * c1;
		R.m12 = -wX * s + wY * wZ * c1;
		R.m22 = c + wZ * wZ * c1;
	}

	/** worker variables, held to avoid memory thrashing */
	private Vector3d domega = new Vector3d(); 
	private Matrix3d dR = new Matrix3d();
	private Vector3d tmp = new Vector3d();
	private Vector3d tmp2 = new Vector3d();

	public void advanceVelocities(double dt) {
		v.scaleAdd( dt*minv, force, v );
		v.add( deltaV.v );
		
        jinv.transform( torque, domega );
        domega.scale( dt );
        omega.add( domega );
        omega.add( deltaV.w );
	}
	
	public void advancePositions(double dt) {
		x.scaleAdd( dt, v, x );
		
		double t = omega.length()*dt;
        domega.set(omega);
        domega.normalize();
        if ( t > 1e-8 ) {
        	expRodrigues(dR, domega, t);
        	dR.mul( theta );                
        	theta.normalizeCP( dR ); // keep it clean!
        }
		
        updateTransformations();
	}	

    /**
     * Computes the total kinetic energy of the body.
     * @return the total kinetic energy
     */
    public double getKineticEnergy() {
    	massAngular.transform(omega,tmp);
        return 0.5 * massLinear * v.lengthSquared() + 0.5 * tmp.dot( omega ); 
    }
    
    /** 
     * Computes the velocity of the provided point provided in world coordinates due
     * to motion of this body.   
     * @param contactPointW
     * @param result the velocity
     */
    public void getSpatialVelocity( Point3d contactPointW, Vector3d result ) {
    	tmp.sub( contactPointW, x );
        result.cross( omega, tmp );
        result.add( v );
    }
    
    /**
	 * Check if the body is part of a cycle formed by three bodies with one contact between each.
	 * @param count
	 * @param startBody
	 * @param bpcFrom
	 * @return true or false
	 */
    private ArrayList<BodyPairContact> cycle = new ArrayList<BodyPairContact>(4);
	protected boolean checkCycle(int count, RigidBody startBody, BodyPairContact bpcFrom, MergeParameters mergeParams) {
		
		cycle.add(bpcFrom);
		
		if (count>3) { // more than four bodies in the cycle
			cycle.clear();
			return false;
		}
		
		// if we come across a collection in the contact graph, we consider it as a body and check the external bpc 
		HashSet<BodyPairContact> bpcList = (this.isInCollection())? parent.bodyPairContacts : bodyPairContacts;
		
		RigidBody otherBodyFrom = bpcFrom.getOtherBodyWithCollectionPerspective(this);		
		
		BodyPairContact bpcToCheck = null;
		for (BodyPairContact bpc: bpcList) {
			
			if(bpc != bpcFrom &&      // we do not want to check the bpc we come from
			  !bpc.inCollection &&    // nor the bpc inside the collection
			  !bpc.inCycle && 		  // a bpc should only be part of one cycle
			  !cycle.contains(bpc)) { // we do not want to check a bpc that has already been tag as part of the cycle
																	  					   
				
				// we only consider bodies that are ready to be merged
				if(bpc.checkMergeCondition(mergeParams, false)) {
			
					RigidBody otherBody = bpc.getOtherBodyWithCollectionPerspective(this);
	
					int nbActiveContact = 0;
					for (Contact contact : bpc.contactList)
						if (contact.state != ContactState.BROKEN)
							nbActiveContact += 1;
					
					if (nbActiveContact<3) // if there is less than three active contacts in the bpc, it is a direction we want to check for cycle
						bpcToCheck = bpc;
					
					if(otherBody == otherBodyFrom || // we are touching two different bodies in a same collection
					   otherBody.isInSameCollection(startBody) || // we are part of a collection that touches a same body 
					   otherBody == startBody || // we have reached the body from which the cycle started
					  (otherBody.pinned && startBody.pinned)) { // there is a body between two pinned body	
						cycle.add(bpc);
						updateCycle();
						return true;
					}
				}
			}
		}

		// we did not find a cycle, but there is another candidate, continue
		if(bpcToCheck!=null) {
			RigidBody otherBody = bpcToCheck.getOtherBodyWithCollectionPerspective(this);
			return otherBody.checkCycle(++count, startBody, bpcToCheck, mergeParams); 
		}
		
		// we did not find a cycle, and there is no another candidate, break
		cycle.clear();
		return false;
	}
	
	/**
	 * Input bpc is added as being part of the cycle. Update all lists and datas accordingly. 
	 * @param bpc
	 */
	public void updateCycle() {
		
		Color color = new Color();
		color.setRandomColor();
		for (BodyPairContact bpc: cycle) {
			if (bpc.cycle == null)
				bpc.cycle = new ArrayList<BodyPairContact>();
			bpc.cycle.clear();
			bpc.cycle.addAll(cycle);
			
			if (bpc.cycleColor == null) 
				bpc.cycleColor = new Color();
			bpc.cycleColor.set(color);
			
			bpc.inCycle = true;
		}
		
		cycle.clear();
	}
        
    /**
     * Resets this rigid body to its initial position and zero velocity, recomputes transforms
     */
    public void reset() {
        x.set( x0 );        
        theta.set( theta0 );
        massAngular.set( massAngular0 ); 
        jinv.set( jinv0 );       
        v.set(v0);
        omega.set(omega0);
        updateTransformations();
        
        metricHistory.clear();
    }
   
    /** 
     * Draws the geometry of a rigid body
     * @param drawable
     */
    public void display( GLAutoDrawable drawable ) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glPushMatrix();        
        gl.glMultMatrixd( transformB2W.Tflat.asArray(),0 );
        	
        if (geom == null ) 
        	geom = null; // set your breakpoint here...
        
    	geom.display( drawable );
        gl.glPopMatrix();
    }
    
    public void displayFrame( GLAutoDrawable drawable ) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glPushMatrix();        
        gl.glMultMatrixd( transformB2W.Tflat.asArray(),0 );
        FancyAxis.draw(drawable);
        gl.glPopMatrix();
    }
    
	/**
	 * Draws bounding box
	 * @param drawable
	 */
	public void displayBB(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize( 10 );
		float[] col = new  float[] {0, 0, 1, 0.5f};
		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, col, 0 );
		gl.glBegin(GL.GL_POINTS);
		for (Point3d point : boundingBoxB) {
			Point3d p = new Point3d(point);
			transformB2W.transform(p);
			gl.glVertex3d(p.x, p.y, p.z);
		}					
		gl.glEnd();
	}
	
	@Override
	public String toString() {
		return name + " " + super.toString();
	}

}