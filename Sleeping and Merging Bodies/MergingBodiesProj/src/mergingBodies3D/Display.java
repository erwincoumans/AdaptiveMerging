package mergingBodies3D;

import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.BooleanParameter;
import mintools.parameters.DoubleParameter;
import mintools.swing.HorizontalFlowPanel;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.EasyViewer;

public class Display {
	
	public class DisplayParameters {

	    public BooleanParameter hideOverlay = new BooleanParameter( "hide overlay", false );
	    public BooleanParameter drawGraphs = new BooleanParameter( "draw performance graphs", false );
	    public BooleanParameter drawMemGraphs = new BooleanParameter( "draw memory graphs", false );
	    
		private DoubleParameter transparency = new DoubleParameter("body block transparency", 1., 0, 1 );
		private BooleanParameter drawBodies = new BooleanParameter( "draw bodies", true );
		private BooleanParameter drawCollections = new BooleanParameter( "draw collections with different colors", true );
		
		private BooleanParameter drawBoundingVolumes = new BooleanParameter( "draw root bounding volumes", false );
		private BooleanParameter drawBoundingVolumesUsed = new BooleanParameter( "draw bounding volumes used", false );
		private BooleanParameter drawAllBoundingVolumes = new BooleanParameter( "draw ALL bounding volumes", false );
		
		private BooleanParameter drawContactForces = new BooleanParameter("draw contact forces", true );
		private BooleanParameter drawContactForcesInCollection = new BooleanParameter("draw contact forces in collections", true );
		private BooleanParameter drawContactLocations = new BooleanParameter( "draw contact locations", true );
		private DoubleParameter contactLocationSize = new DoubleParameter( "contact point size ", 5, 1, 20);
		private BooleanParameter drawContactGraph = new BooleanParameter( "draw contact graph", false );
		private BooleanParameter drawCollectionContactGraph = new BooleanParameter( "draw collections' contact graph", false );
		private BooleanParameter drawCycles = new BooleanParameter( "draw cycles", true );
		
		private BooleanParameter drawCOMs = new BooleanParameter( "draw COM", false );
		private BooleanParameter drawSpeedCOMs = new BooleanParameter( "draw speed COM", false );
		private BooleanParameter drawBB = new BooleanParameter( "draw bounding box (for merge test, not collision)", false );
		public BooleanParameter drawIndex = new BooleanParameter( "draw Index", false );
	}
	public DisplayParameters params = new DisplayParameters();
	
	ArrayList<RigidBody> bodies;
	CollisionProcessor collisionProcessor;
	
	Display(ArrayList<RigidBody> bodies, CollisionProcessor collisionProcessor) {
		this.bodies = bodies;
		this.collisionProcessor = collisionProcessor;
	}
	
    /** Might want to allow for different coloured blocks?? but for now, in 3D this is easiest */
    private float[] green = new float[] { 0, 1, 0, 0.25f };
    private float[] colourPinned = new float[] { 0.75f,0.75f,1, 1 };		        			
	private float[] colour = new float[] { 0.9f,0.9f,0.9f, 1 };        			
    private float[] red = new float[] { 1, 0, 0, 0.5f };
    private float[] blue = new float[] { 0, 0, 1, 0.25f };
    
    public void displayNonShadowable( GLAutoDrawable drawable, double dt ) {
    	GL2 gl = drawable.getGL().getGL2();
        gl.glDisable(GL2.GL_DEPTH_TEST);

    	// Should move this stuff below to a display non-shadowable function
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, green, 0);
        if ( params.drawContactGraph.getValue() ) {
            for ( Contact c : collisionProcessor.contacts ) {
                c.displayConnection(drawable);
            }
        }

        gl.glPointSize( params.contactLocationSize.getFloatValue() );
        
    	if (params.drawContactLocations.getValue() || params.drawContactForces.getValue()) {
			for ( Contact c : collisionProcessor.contacts ) {
				if (params.drawContactLocations.getValue()) {
					c.display(drawable, false); 
				}
				if (params.drawContactForces.getValue())
					c.displayContactForce(drawable, false, dt );
			}
		}
		
		if ( params.drawContactLocations.getValue() || params.drawContactForcesInCollection.getValue()) {
			for (RigidBody b : bodies) {
				if (b instanceof RigidCollection) {
					RigidCollection collection = (RigidCollection)b;
					if (params.drawContactLocations.getValue())
						collection.displayInternalContactLocations(drawable);
					if (params.drawContactForcesInCollection.getValue())
						collection.displayInternalContactForces( drawable, dt );
				}
			}
		}
        
        if ( params.drawBB.getValue() ) {
        	 for ( RigidBody b : bodies ) {
               b.displayBB(drawable);
           }
        }
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }
	
    ArrayList<RigidBody> bodiesDrawnForPicking = new ArrayList<RigidBody>();
    
	/**
	 * Draws all rigid bodies
	 * @param drawable
	 */
	public void display( GLAutoDrawable drawable, boolean picking ) {
		
		GL2 gl = drawable.getGL().getGL2();
		
		if ( picking ) {
			bodiesDrawnForPicking.clear();
			int i = 0;
			for ( RigidBody b : bodies ) {
    			if ( b instanceof RigidCollection ) {
    				for ( RigidBody sb : ((RigidCollection) b).bodies ) {
        				LCPApp3D.setColorWithID( gl, i++ );
        				bodiesDrawnForPicking.add( sb );
                        sb.display( drawable );    					
    				}
    			} else {
    				LCPApp3D.setColorWithID( gl, i++ );
    				bodiesDrawnForPicking.add( b );
                    b.display( drawable );
    			}
			}
			return;
		}
		
		if ( params.drawCOMs.getValue() ) {
        	for ( RigidBody b : bodies ) {
        		b.displayFrame(drawable);
        	}
        }
        
        if ( params.drawBodies.getValue() ) {
        	for ( RigidBody b : bodies ) {
    			// let's control the colour of geometry here as it will let us 
    			// decide when we want to override this colour (e.g., if we have a 
    			// rigid body collection)
    			float[] c = colour;
    			if ( b.pinned ) {
    				c = colourPinned;
    			} else if ( b.col != null ) {
    				c = b.col;
    			}
				if( b instanceof RigidCollection && ! params.drawCollections.getValue() ) {
					for (RigidBody b2 : ((RigidCollection)b).bodies) {
						c = ( b2.col != null ) ? b2.col : colour;
						c[3] = params.transparency.getFloatValue();         			
						gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, c, 0 );
						b2.display(drawable);
					}
				} else {
					c[3] = params.transparency.getFloatValue();         			
	    			gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, c, 0 );
	                b.display( drawable );
				}
			}
        }
        
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, red, 0);
        gl.glNormal3f(0,0,1);
    	for ( RigidBody b : bodies ) {
    		for (Spring s : b.springs) {
				s.displaySpring(drawable);
			}
    		
    		if ( b instanceof RigidCollection ) {
    			for ( RigidBody body : ((RigidCollection)b).bodies ) {
	        		for (Spring s : body.springs) {
	    				s.displaySpring(drawable);
	    			}
    			}
        	}   
    	}        
        gl.glLineWidth(1);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, blue, 0);
        if ( params.drawBoundingVolumes.getValue() ) {
            for ( RigidBody b : bodies ) {
            	if ( b.root == null ) continue; // rigid body planes don't have a BVH
            	if (!(b instanceof RigidCollection)) {
            		b.root.boundingSphere.display(drawable);
            	}
            }
        }
        if ( params.drawAllBoundingVolumes.getValue() ) {
            for ( RigidBody b : bodies ) {
            	if ( b.root == null ) continue; // rigid body planes don't have a BVH
            	if (!(b instanceof RigidCollection)) {
            		b.root.display( drawable );
	            } else {
            		displayCollectionBV((RigidCollection) b, drawable);
            	}
            }
        }        
        if ( params.drawBoundingVolumesUsed.getValue() ) {
            for ( RigidBody b : bodies ) {
            	if ( b.root == null ) continue; // rigid body planes don't have a BVH
            	if (!(b instanceof RigidCollection)) {
            		b.root.displayVisitBoundary( drawable, collisionProcessor.visitID );
            	} else {
            		displayVisitBoundaryCollection((RigidCollection) b, drawable);
            	}
            }
        }	        
		
		// TODO: finish updating this display stuff...
		
//		if ( params.drawContactGraph.getValue() ) {
//			if (collisionProcessor.useContactGraph.getValue()) {
//				for (BodyPairContact bpc : collisionProcessor.bodyPairContacts) {
//					for (Contact c : bpc.contactList) {
//						c.displayContactGraph(drawable);
//					}
//				} 
//			} 
//			else {
//				for ( Contact c : collisionProcessor.contacts ) {
//					c.displayContactGraph(drawable);
//				}
//			}
//		}
//		
//		if (params.drawCollectionContactGraph.getValue()) {
//			for (RigidBody b : bodies) {
//				if (b instanceof RigidCollection) {
//					((RigidCollection)b).displayContactGraph(drawable);
//				}
//			}
//		}
//		
//		if ( params.drawSpeedCOMs.getValue() ) {
//			for (RigidBody b: bodies) {
//				b.displaySpeedCOM(drawable);
//			}
//		}
//
//	
//		
//		if ( params.drawCycles.getValue() ) {
//			for (RigidBody b : bodies) {
//				if (b instanceof RigidCollection) {
//					((RigidCollection)b).displayCycles(drawable, params.contactLocationSize.getValue());
//				}
//			}
//		}
		
		
		if ( params.drawIndex.getValue()) {
			for (RigidBody b : bodies) {
				displayIndex(b, bodies.indexOf(b), drawable, GLUT.BITMAP_8_BY_13);
			}
		}
	}

	private void displayVisitBoundaryCollection(RigidCollection b, GLAutoDrawable drawable) {
		for (RigidBody body: b.bodies) {
			body.root.displayVisitBoundary(drawable, collisionProcessor.visitID);
		}
	}

	private void displayCollectionBV(RigidCollection b, GLAutoDrawable drawable) {
		for (RigidBody body: b.bodies) {
			body.root.display(drawable);
		}
	}
	
	public void displayIndex(RigidBody body, int index, GLAutoDrawable drawable, int font) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glColor3f(1, 0, 0);
		gl.glRasterPos2d(body.x.x, body.x.y);

		EasyViewer.glut.glutBitmapString(font, Integer.toString(index));
	}

	/**
	 * @return control panel for the system
	 */
	public JPanel getControls() {

		VerticalFlowPanel vfp = new VerticalFlowPanel();
		
		HorizontalFlowPanel hfp = new HorizontalFlowPanel();
        hfp.add( params.hideOverlay.getControls() );
        hfp.add( params.drawGraphs.getControls() );
        hfp.add( params.drawMemGraphs.getControls() );
        vfp.add( hfp.getPanel() );
        
		VerticalFlowPanel vfpc = new VerticalFlowPanel();
		vfpc.add( params.drawContactForces.getControls() );
		vfpc.add( params.drawContactForcesInCollection.getControls() );
		vfpc.add( params.drawContactLocations.getControls() );
		vfpc.add( Contact.forceVizScale.getSliderControls(true) ); // Gross?
		vfpc.add( params.contactLocationSize.getSliderControls(false));
		vfpc.setBorder( new TitledBorder("Contact Force Visualization") );
        ((TitledBorder) vfpc.getPanel().getBorder()).setTitleFont(new Font("Tahoma", Font.BOLD, 18));
		vfp.add( vfpc.getPanel() );


        VerticalFlowPanel vfpb = new VerticalFlowPanel();
		vfpb.add( params.transparency.getSliderControls(false));
		vfpb.add( params.drawBodies.getControls() );
		vfpb.add( params.drawCollections.getControls() );
		vfpb.add( params.drawCOMs.getControls() );
		vfpb.add( params.drawSpeedCOMs.getControls() );
		vfpb.add( params.drawIndex.getControls() );
		vfpb.setBorder( new TitledBorder( "Rigid Body Visualization") );
        ((TitledBorder) vfpb.getPanel().getBorder()).setTitleFont(new Font("Tahoma", Font.BOLD, 18));
		vfp.add( vfpb.getPanel() );
		
		VerticalFlowPanel vfpg = new VerticalFlowPanel();
		vfpg.add( params.drawContactGraph.getControls() );
		vfpg.add( params.drawCollectionContactGraph.getControls() );
		vfpg.add( params.drawCycles.getControls() );
		vfpg.setBorder( new TitledBorder( "Contact Graph Visualization") );
        ((TitledBorder) vfpg.getPanel().getBorder()).setTitleFont(new Font("Tahoma", Font.BOLD, 18));
		vfp.add( vfpg.getPanel() );

		VerticalFlowPanel vfpv = new VerticalFlowPanel();
		vfpv.add( params.drawBoundingVolumes.getControls() );
		vfpv.add( params.drawBoundingVolumesUsed.getControls() );
		vfpv.add( params.drawAllBoundingVolumes.getControls() );
		vfpv.add( params.drawBB.getControls() );
		vfpv.setBorder( new TitledBorder("Bounding Volume Visualization") );
        ((TitledBorder) vfpv.getPanel().getBorder()).setTitleFont(new Font("Tahoma", Font.BOLD, 18));
		vfp.add( vfpv.getPanel() );
		
		return vfp.getPanel();
	}
}