package mergingBodies;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import mergingBodies.RigidBodySystem.MetricType;

/**
 * This class is used for relative motion's calculations
 */
public class MetricProcessor {	
	
	private MetricType metricType;
	
	public void setMetricType(int type) {
		if(type ==  MetricType.LARGESTVELOCITY.ordinal())
			metricType = MetricType.LARGESTVELOCITY;
		else if(type ==  MetricType.RELATIVEKINETICENERGY.ordinal())
			metricType = MetricType.RELATIVEKINETICENERGY;
		else if(type ==  MetricType.VELOCITIESNORM.ordinal())
			metricType = MetricType.VELOCITIESNORM;
		else
			System.err.println("[getMetric] metric type unknown");
	}
	
	public double getMetric(RigidBody body1, RigidBody body2) {
	
			double metric = 0.;
			
			if (metricType == MetricType.VELOCITIESNORM)
				metric = getRelativeVelocitiesNorm(body1, body2);
			else if (metricType == MetricType.RELATIVEKINETICENERGY) 
				metric = getRelativeKineticEnergy(body1, body2);
			else if (metricType == MetricType.LARGESTVELOCITY)
				metric = getLargestVelocityNorm(body1, body2);
			else
				System.err.println("[getMetric] metric type unknown");
				
			return metric;
	}
	
	public double getRelativeVelocitiesNorm(RigidBody body1, RigidBody body2) {
		
		Vector2d relativeLinearVelocity = getRelativeLinearVelocity(body1, body2);
		double relativeAngularVelocity = getRelativeAngularVelocity(body1, body2);
		
		double k = 0.5*relativeLinearVelocity.lengthSquared() + 0.5*relativeAngularVelocity*relativeAngularVelocity;
		return k;
	}
	
	public double getRelativeKineticEnergy(RigidBody body1, RigidBody body2) {

		Vector2d relativeLinearVelocity = getRelativeLinearVelocity(body1, body2);
		double relativeAngularVelocity = getRelativeAngularVelocity(body1, body2);
		
		double massDifference = Math.abs(body1.massLinear - body2.massLinear);
		double inertiaDifference = Math.abs(body1.massAngular - body2.massAngular);
		double k = 0.5*relativeLinearVelocity.lengthSquared()*massDifference+ 0.5*relativeAngularVelocity*relativeAngularVelocity*inertiaDifference;
		
		return k/massDifference;
	}
	
	public double getLargestVelocityNorm(RigidBody body1, RigidBody body2) {
		
		Point2d COM = getCommonCOM(body1, body2);
		double largestVelocityNorm = getLargestVelocityNorm(body1, COM);
		largestVelocityNorm = Math.max(largestVelocityNorm, getLargestVelocityNorm(body2, COM));
		
		return largestVelocityNorm;
	}
	
	/**
	 * Compute the relative linear velocity
	 * @param body1
	 * @param body2
	 * @return relative linear velocity
	 */
	public Vector2d getRelativeLinearVelocity(RigidBody body1, RigidBody body2) {
		
		if ( body1.pinned || body1.temporarilyPinned ) {
			if(body1.v.x != 0. || body1.v.y != 0.) {
				System.err.print("[getRelativeLinearVelocity] linear velocity of pinned body is not zero: ");
				System.err.println(body1.v);
			} else {
				return body2.v;
			}
		} else if ( body2.pinned || body2.temporarilyPinned ) {
			if(body2.v.x != 0. || body2.v.y != 0.) {
				System.err.print("[getRelativeLinearVelocity] linear velocity of pinned body is not zero: ");
				System.err.println(body2.v);
			} else {
				return body1.v;
			}
		}
		
		Vector2d relativeLinearVelocity = new Vector2d();
	
		Point2d COM = getCommonCOM(body1, body2);
	
		relativeLinearVelocity.sub(body2.v, body1.v);

		Vector2d tmp = new Vector2d();
		Vector2d tmp2 = new Vector2d();
		
		tmp.sub( COM, body2.x );
		tmp.scale( body2.omega );
		tmp2.set( -tmp.y, tmp.x );
		relativeLinearVelocity.add( tmp2 );
		
		tmp.sub( COM, body1.x );
		tmp.scale( body1.omega );
		tmp2.set( -tmp.y, tmp.x );
		relativeLinearVelocity.sub( tmp2 );
		
		return relativeLinearVelocity;
	}
	
	/**
	 * Compute the relative linear velocity
	 * @param body1
	 * @param body2
	 * @return relative linear velocity
	 */
	public double getLargestVelocityNorm(RigidBody body, Point2d COM) {
		double largestVelocity = -Double.MAX_VALUE;
		
		for (Point2d point : body.boundingBoxB) {
			final Vector2d rw = new Vector2d( -(point.y - COM.y), point.x - COM.x );
			rw.scale( body.omega );
			largestVelocity = Math.max(largestVelocity, Math.sqrt(rw.lengthSquared()));
		}
		
		return largestVelocity;
	}
	
	protected Point2d getCommonCOM(RigidBody body1, RigidBody body2) {
		Point2d massCOM1 = new Point2d(body1.x);
		Point2d massCOM2 = new Point2d(body2.x);
		massCOM1.scale(body1.massLinear);
		massCOM2.scale(body2.massLinear);
		Point2d newCOM = new Point2d();
		newCOM.add( massCOM1, massCOM2 );
		newCOM.scale( 1./(body1.massLinear + body2.massLinear) );
		return newCOM;
	}

	/**
	 * Compute the relative angular velocity
	 * @param body1
	 * @param body2
	 * @return relative angular velocity
	 */
	public double getRelativeAngularVelocity(RigidBody body1, RigidBody body2) {
		
		if ( body1.pinned || body1.temporarilyPinned ) {
			if(body1.omega != 0.) {
				System.err.print("[getRelativeAngularVelocity] angular velocity of pinned body is not zero: ");
				System.err.println(body1.omega);
			} else {
				return body2.omega;
			}
		} else if ( body2.pinned || body2.temporarilyPinned ) {
			if(body2.omega != 0.) {
				System.err.print("[getRelativeAngularVelocity] angular velocity of pinned body is not zero: ");
				System.err.println(body2.omega);
			} else {
				return body1.omega;
			}
		}

		return body2.omega - body1.omega;
	}
}