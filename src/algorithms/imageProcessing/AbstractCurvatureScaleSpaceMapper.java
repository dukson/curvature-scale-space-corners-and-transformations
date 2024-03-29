package algorithms.imageProcessing;

import algorithms.util.PairIntArray;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author nichole
 */
public class AbstractCurvatureScaleSpaceMapper {

    protected CurvatureScaleSpaceMapperState state = 
        CurvatureScaleSpaceMapperState.UNINITIALIZED;
    
    protected GreyscaleImage img;
    
    protected final GreyscaleImage originalImg;
    
    /**
     * edges extracted from image.  if an instance of PairIntArrayWithColor
     * is present, that holds a color field in which a value of '1' means
     * the curve is closed.
     */
    protected List<PairIntArray> edges = new ArrayList<PairIntArray>();
    
    /**
     * when making the corner list for the purpose of using them as stable
     * features to identify in other images, may want to exclude 
     * corners that are in this list, highChanges.
     */
    protected List<Integer> highChangeEdges = new ArrayList<Integer>();
 
    protected PairIntArray tCorners = new PairIntArray();
    
    protected boolean doNotNormalizeByHistogram = false;
    
    protected boolean useLineDrawingMode = false;
    
    protected boolean doNotStraightenEdges = false;
    
    protected boolean useLowestHighIntensityCutoff = false;
    
    protected boolean useLowHighIntensityCutoff = false;
    
    protected boolean useSegmentationForSky = false;
    
    protected final int trimmedXOffset;
    
    protected final int trimmedYOffset;
    
    protected boolean useOutdoorMode = false;
    
    protected GreyscaleImage gradientXY = null;
     
    protected Logger log = Logger.getLogger(this.getClass().getName());
    
    /**
     * constructor w/ input image which is operated on.  the same instance
     * input is modified by this class.
     * 
     * @param input 
     */
    public AbstractCurvatureScaleSpaceMapper(GreyscaleImage input) {
        
        img = input;
        
        ImageProcesser imageProcesser = new ImageProcesser();
        
        originalImg = input.copyImage();
            
        int[] offsetXY = imageProcesser.shrinkImageToFirstNonZeros(img);
        
        trimmedXOffset = offsetXY[0];
        
        trimmedYOffset = offsetXY[1];
    }
    
    /**
     * constructor with input image and the already extracted edges.
     * The input image is needed only for debugging purposes and 
     * may be removed as an argument after testing is complete.
     * @param input
     * @param theEdges 
     */
    public AbstractCurvatureScaleSpaceMapper(GreyscaleImage input, 
        List<PairIntArray> theEdges) {
        
        img = input;
        
        ImageProcesser imageProcesser = new ImageProcesser();
        
        originalImg = input.copyImage();
        
        int[] offsetXY = imageProcesser.shrinkImageToFirstNonZeros(img);
        
        trimmedXOffset = offsetXY[0];
        
        trimmedYOffset = offsetXY[1];
        
        this.edges = new ArrayList<PairIntArray>(theEdges);
        
        state = CurvatureScaleSpaceMapperState.INITIALIZED;
    }

    /**
     * apply histogram normalization before processing.  For some images, this
     * will increase the contrast of fainter features.
     */
    public void doNotPerformHistogramEqualization() {
        this.doNotNormalizeByHistogram = true;
    }
    
    public void useLowestHighIntensityCutoff() {
        useLowestHighIntensityCutoff = true;
    }
    
    public void useLowHighIntensityCutoff() {
        useLowHighIntensityCutoff = true;
    }
    
    public void useLineDrawingMode() {
        useLineDrawingMode = true;
    }
    
    public void useOutdoorMode() {
        
        useLowestHighIntensityCutoff();
        
        useOutdoorMode = true;
    }
    
    /**
     * use segmentation to try to reduce the sky to one color which makes the
     * horizon features such as peaks more visible.
     * This method has the side effects of disabling histogram equalization
     * and setting the intensity for the higher threshold in the 2-layer
     * filter of the Canny Edge filter to a lower value than normal.
     * Note that "line drawing" mode should probably not be used with this,
     * but hasn't been tested yet.
     */
    public void useSegmentationForSky() {
        useSegmentationForSky = true;
        //doNotNormalizeByHistogram = true;
        useLowHighIntensityCutoff = true;
    }
    
    protected void initialize() {
        
        if (state.ordinal() < 
            CurvatureScaleSpaceMapperState.INITIALIZED.ordinal()) {
            
            // (1) apply an edge filter
            applyEdgeFilter();
            
            // (2) extract edges
            extractEdges();
            
            //TODO: note that there may be a need to search for closed
            //      curves in the EdgeContourExtractor instead of here
            //      in order to create shapes instead of creating
            //      lines preferentially.
            // (3) look for t-junctions and closed curves
            markTheClosedCurvesAndTCorners();
            
            state = CurvatureScaleSpaceMapperState.INITIALIZED;
        }
    }

    protected void applyEdgeFilter() {
        
        CannyEdgeFilter filter = new CannyEdgeFilter();
        
        if (useOutdoorMode) {
            filter.useOutdoorMode();
        }
        
        if (useSegmentationForSky) {
            try {
                ImageProcesser imageProcessor = new ImageProcesser();
                imageProcessor.applyImageSegmentation(img, 2);
            } catch (IOException e) {
                log.severe("segmentation could not be performed: " + 
                    e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                log.severe("segmentation could not be performed: " + 
                    e.getMessage());
            }
        }
        
        if (doNotNormalizeByHistogram) {
            filter.doNotPerformHistogramEqualization();
        }
        if (useLineDrawingMode) {
            filter.useLineDrawingMode();
        }
        
        if (useLowestHighIntensityCutoff) {
            filter.overrideHighThreshold(1.0f);
        } else if (useLowHighIntensityCutoff) {
            filter.overrideHighThreshold(2.0f);
        }
        
        filter.applyFilter(img);
        
        gradientXY = filter.getGradientXY();
                        
        state = CurvatureScaleSpaceMapperState.EDGE_FILTERED;
    }

    protected void extractEdges() {
        
        EdgeExtractor contourExtractor;
        
        if (doNotStraightenEdges) { 
            contourExtractor = new EdgeExtractor(img);
        } else {
            contourExtractor = new EdgeExtractor(img, gradientXY);
        }
        
        List<PairIntArray> tmpEdges = contourExtractor.findEdges();
       
        edges.clear();
        edges.addAll(tmpEdges);
        
        GreyscaleImage output = new GreyscaleImage(img.getWidth(), img.getHeight());
        for (PairIntArray edge : edges) {
            addCurveToImage(edge, output, 0, 255);
        }
        
        img = output;
        
        state = CurvatureScaleSpaceMapperState.EDGES_EXTRACTED;
        
        log.fine("edges extracted");
    }

    protected void markTheClosedCurvesAndTCorners() {
        
        ClosedCurveAndJunctionFinder ccjFinder = 
            new ClosedCurveAndJunctionFinder();
        
        ccjFinder.findClosedCurves(edges);
        
        /*
              ---@---
                 |
                 |
        */
        // Fill small gaps in edge contours. When the gap forms a T-junction, 
        // mark it as a T-corner.
        // May have already filled these in in the contourextractor.
        // TODO: revisit this and consider tracking t-junctions in the
        // contourExtractor when filling a gap
        // see http://www.diva-portal.org/smash/get/diva2:457189/FULLTEXT01.pdf
        //  pg 26
    }
  
    public List<PairIntArray> getEdges() {
        return edges;
    }

    protected void addCurveToImage(PairIntArray edge, GreyscaleImage input, 
        int nExtraForDot, int value) {
        
        for (int i = 0; i < edge.getN(); i++) {
            int x = edge.getX(i);
            int y = edge.getY(i);
            for (int dx = -1 * nExtraForDot; dx < (nExtraForDot + 1); dx++) {
                float xx = x + dx;
                if ((xx > -1) && (xx < (input.getWidth() - 1))) {
                    for (int dy = -1 * nExtraForDot; dy < (nExtraForDot + 1); 
                        dy++) {
                        
                        float yy = y + dy;
                        if ((yy > -1) && (yy < (input.getHeight() - 1))) {
                            input.setValue((int) xx, (int) yy, value);
                        }
                    }
                }
            }
        }
    }
    
    public boolean getInitialized() {
        
        return (state.ordinal() >= 
            CurvatureScaleSpaceMapperState.INITIALIZED.ordinal());
    }

    public List<Integer> getNoisyEdgeIndexes() {
        return highChangeEdges;
    }
    
    public GreyscaleImage getImage() {
        return img;
    }

    public GreyscaleImage getOriginalImage() {
        return originalImg;
    }
    
    public int getTrimmedXOffset() {
        return trimmedXOffset;
    }
    
    public int getTrimmedYOffset() {
        return trimmedYOffset;
    }
   
    /*
    The making of a curvature scale space image is in
    "Scale-Based Description and Recognition of Planar Curves and Two-Dimensional
    Shapes" by FARZIN MOKHTARIAN AND ALAN MACKWORTH
    IEEE 'TRANSACTIONS ON PATTERN ANALYSIS AND MACHINE INTELLIGENCE,
    VOL. PAMI-8, NO. 1. JANUARY 1986
    https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0CCIQFjAA&url=https%3A%2F%2Fwww.cs.ubc.ca%2F~mack%2FPublications%2FIEEE-PAMI86.pdf&ei=jiIFVJGNLIa0igLv74DgDw&usg=AFQjCNHj7v2JaUDqSFkQZSNOSpqBbfbOAQ&sig2=L08nOsKD1Mw_XJX-EPmY-w&bvm=bv.74115972,d.cGE
    planar curve:
    f_curve = {x(t), y(t)}
    t = linear function of the path length bounded by values [0, 1], that is,
    one can make this by scaling the range os indexes for x and y
    for a curve to values between 0 and 1.
    If f_curve is closed, x(t) and y(t) are periodic functions.
    The curvature, k, is the the change of the angle of the tangent line at
    point P on arc s with respect to the arc length s.
    #  /
    # /
    #/      /|
    P   ds / | dy
    #/      /__|
    # /        dx
    #  /
    /
    / theta
    ._________
    ds^2 = dx^2 + dy^2
    ds = sqrt(dx^2 + dy^2) = sqrt(1 + (dy/dx)^2)*dx = sqrt((dx/dy)^2 + 1)*dy
    k = dTheta/ds = 1/rho
    where rho is the radius of the circle of curvature at point P
    dTheta   dTheta   dx
    ------ = ------ * --
    ds       dx      ds
    theta = tan^-1 (dy / dx)
    d                 d/dx (dy/dx)      d^2y/dx^2
    dTheta/dx = -- arctan(dy/dx) = ------------- = -------------
    dx                 1 + (dy/dx)^2   1 + (dy/dx)^2
    dx    1             1
    -- = ------ = -------------------
    ds   ds/dx    sqrt(1 + (dy/dx)^2)
    and use y' = (dy/dx)
    and use y" = (d^2y/dx^2)
    dTheta          y"                  1
    k =  ------ = --------------- * -------------------
    ds     (1 + (dy/dx)^2)   sqrt(1 + (dy/dx)^2)
    d^2y/dx^2
    = ---------------------  for planar curves
    (1 + (dy/dx)^2)^(1.5)
     * the sign of k is + if y" is + and is - if y" is -. the absolute value
    might be used instead though.
    NOTE that if dy/dx doesn’t exist at a point, such as where
    the tangent line is parallel to the y-axis,
    one can invert the y/x relationships in k to x/y
    (d^2x/dy^2)
    k = ---------------------
    (1 + (dx/dy)^2)^(1.5)
     * Need to express k in terms of a function of t, the parameteric form of k
    dTheta   dTheta   dt     1     dTheta
    k = ------ = ------ * -- = ----- * ------
    ds       dt      ds   ds/dt     dt
    where (ds/dt)^2 = (dx/dt)^2 + (dy/dt)^2
    dy   dy/dt
    tan(theta) = -- = -----
    dx   dx/dt
    d
    --(tan(theta)) = sec^2(theta) * (dTheta/dt)
    dt
    (d^2y/dt^2)   (dy/dt)*(d^2x/dt^2)
    = ----------- - -------------------
    (dx/dt)           (dx/dt)^2
    (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    = -----------------------------------------
    (dx/dt)^2
    dTheta        1         (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    so ------ = ------------ * -----------------------------------------
    dt      sec^2(theta)                   (dx/dt)^2
    1           (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    = ---------------- * -----------------------------------------
    1 + tan^2(theta)                 (dx/dt)^2
    1           (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    = ---------------- * -----------------------------------------
    1 + (dy/dt)^2                  (dx/dt)^2
    ---------
    (dx/dt)^2
    (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    = ------------------------------------------
    (dx/dt)^2 + (dy/dt)^2
     * now can return to
    1      dTheta
    k_geodesic = ----- *  ------
    ds/dt     dt
    (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    = ---------------------------------------------------------
    (((dx/dt)^2 + (dy/dt)^2)^(0.5)) * ((dx/dt)^2 + (dy/dt)^2)
    (d^2y/dt^2)*(dx/dt) - (dy/dt)*(d^2x/dt^2)
    = -----------------------------------------
    ((dx/dt)^2 + (dy/dt)^2)^(1.5)
    REWRITE in terms of code:
    X_dot(t,o~) * Y_dot_dot(t,o~) - Y_dot(t,o~) * X_dot_dot(t,o~)
    k(t,o~) = ----------------------------------------------------------------
    (X^2(t,o~) + Y^2(t,o~))^1.5
    where o~ denotes the width of the Gaussian
    convolve X and Y w/ one dimensional gaussian kernel each:
    X(t, o~) = Integ(x(v) * exp(-(v)^2/2o~^2) * dv)
    Y(t, o~) = Integ(y(v) * exp(-(v)^2/2o~^2) * dv)
    Integ denotes the integral evaluated from -infinity to +infinity.
    First Deriv:
    X_dot(t,o~) = Integ(x(v) * (-2*(v)) * exp(-(v)^2/2o~^2) * dv)
    Y_dot(t,o~) = Integ(y(v) * (-2*(v)) * exp(-(v)^2/2o~^2) * dv)
    Second Deriv:
    X_dot_dot(t,o~) = Integ(x(v) * (-2 + 4 * (v)^2)) * exp(-(v)^2/2o~^2) * dv)
    Y_dot_dot(t,o~) = Integ(y(v) * (-2 + 4 * (v)^2)) * exp(-(v)^2/2o~^2) * dv)
    The curvture of a straight line is zero.
    Points where k = 0 are called the points of inflection.
     */

}
