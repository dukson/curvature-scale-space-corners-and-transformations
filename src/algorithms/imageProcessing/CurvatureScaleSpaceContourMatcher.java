package algorithms.imageProcessing;

import algorithms.util.PairInt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * class to match the contours extracted from two images and match them.
 * 
 * Based upon the algorithm contained in
 * <pre>
 * IEEE 'TRANSACTIONS ON PATTERN ANALYSIS AND MACHINE INTELLIGENCE, VOL. PAMI-8, 
 * NO. 1. JANUARY 1986.  "Scale-Based Description and Recognition of Planar 
 * Curves and Two-Dimensional Shapes" by FARZIN MOKHTARIAN AND ALAN MACKWORTH
 * </pre>
 * 
 * A small change was made to the calculation and use of shift.
 * 
 * In CurvatureScaleSpaceImageMaker:
 * Edges are extracted from an image and for the closed curves in those edges,
 * scale space maps are made.  inflection points are found in those maps.
 * The range of sigma for each curve's scale space maps are from the lowest 
 * sigma, increasing by a factor of sqrt(2) until a sigma where there are no 
 * more inflection points.
 * t vs sigma "images" are created and the contours are extracted from those.
 * 
 * This code accepts the contours from two different images and maps the first
 * image to the other using the contours. 
 * 
 * @author nichole
 */
public final class CurvatureScaleSpaceContourMatcher {
    
    protected Heap heap = null;
    
    /**
     * the costs calculated here are small fractions, so they need to be
     * multiplied by a large constant for use with the Fibonacci heap
     * which uses type long for its key (key is where cost is stored).
     * using 1E12 here
     */
    protected final static long heapKeyFactor= 1000000000000l;
    
    protected List<CurvatureScaleSpaceContour> c1;
    
    protected List<CurvatureScaleSpaceContour> c2;
    
    protected Map<Integer, List<Integer> > curveIndexToC1;
    
    protected Map<Integer, List<Integer> > curveIndexToC2;
        
    private double solutionScale = Double.MAX_VALUE;
    
    private double solutionShift = Double.MAX_VALUE;
    
    private double solutionCost = Double.MAX_VALUE;
    
    private List<CurvatureScaleSpaceContour> solutionMatchedContours1 = null;
    
    private List<CurvatureScaleSpaceContour> solutionMatchedContours2 = null;
    
    private final Logger log = Logger.getLogger(this.getClass().getName());
        
    private boolean scalesSomeSmallerThanOne = false;
    
    /**
     * constructor taking required contour lists as arguments.  note that the
     * contour lists should only be from one edge each.
     * (Note, it should be possible to find shadows in an image too using this 
     * on edges in same image).
     * 
     * constructor.  the creation of internal data structures in this method
     * has runtime complexity:
     * <pre>
     *   O(N) + O(N*lg_2(N)) + O(N_curves^2) + O(N_curves^3)
     * 
     *       where each curve has a number of contours.
     * 
     *       N = number of contours
     *       N_curve = number of encapsulating curves.  this number is smaller
     *                 than N.  
     * </pre>
     */
    public CurvatureScaleSpaceContourMatcher() {
    }
    
    private void initializeVariables(final List<CurvatureScaleSpaceContour> contours1, 
        final List<CurvatureScaleSpaceContour> contours2) {
        
        // then use collections synchronized and LongestEdgeComparator to
        // sort the ordered list

        c1 = new ArrayList<CurvatureScaleSpaceContour>(contours1);
        
        c2 = new ArrayList<CurvatureScaleSpaceContour>(contours2);
        
        Collections.sort(c1, new DescendingSigmaComparator());
        
        Collections.sort(c2, new DescendingSigmaComparator());
        
        curveIndexToC1 = new HashMap<Integer, List<Integer> >();
        
        curveIndexToC2 = new HashMap<Integer, List<Integer> >();
        
        for (int i = 0; i < c1.size(); i++) {
            CurvatureScaleSpaceContour contour = c1.get(i);            
            Integer curveIdx = Integer.valueOf(contour.getEdgeNumber());
            List<Integer> indexes = curveIndexToC1.get(curveIdx);
            if (indexes == null) {
                indexes = new ArrayList<Integer>();
                curveIndexToC1.put(curveIdx, indexes);
            }
            indexes.add(Integer.valueOf(i));            
        }
        
        for (int i = 0; i < c2.size(); i++) {
            CurvatureScaleSpaceContour contour = c2.get(i);            
            Integer curveIdx = Integer.valueOf(contour.getEdgeNumber());
            List<Integer> indexes = curveIndexToC2.get(curveIdx);
            if (indexes == null) {
                indexes = new ArrayList<Integer>();
                curveIndexToC2.put(curveIdx, indexes);
            }
            indexes.add(Integer.valueOf(i));            
        }
        
        initializeHeapNodes();
    }
    
    /**
    <pre>
       (1) create a node for every possible pair of the tallest contour of
          each curve in c1 with the same in c2.
          
          Solve for kScale and dShift for each node:
              t2 = kScale * t1 + dShift
              sigma2 = kScale * sigma1
              
          The runtime complexity for (1) is O(N_curves^2).
         
       (2) initial costs:
           apply the transformation parameters to the tallest contours from
           each curve, using only one per curve.

           Note that the transformation may wrap around the image, that is
           the t values.
           
           The cost is the difference between the predicted location of the
           2nd node, that is the model location, and the actual location of the
           node.
            
           The cost is either the straight line distance between the peaks
           as a function of t and sigma, or it only includes the sigma
           differences.
           (the paper isn't clear)
           
            The runtime complexity for this (2) is 
    </pre>
    */
    private void initializeHeapNodes() {
                
        heap = new Heap();
        
        // (1)  create initial scale and translate nodes from all possible 
        // contour combinations
       
        for (int index1 = 0; index1 < c1.size(); index1++) {
            
            CurvatureScaleSpaceContour contour1 = c1.get(index1);
            
            // find this contour's place within the parent edge's contours
            // to correct shift due to wrapping around 1 to 0 or vice versa                
            float minT = Float.MAX_VALUE;
            float maxT = Float.MIN_VALUE;
            List<Integer> c1Indexes = curveIndexToC1.get(
                Integer.valueOf(contour1.getEdgeNumber()));
            for (int i = 0; i < c1Indexes.size(); i++) {
                int c1Idx = c1Indexes.get(i);
                CurvatureScaleSpaceContour ei = c1.get(c1Idx);
                float t = ei.getPeakScaleFreeLength();
                if (t < minT) {
                    minT = t;
                }
                if (t > maxT) {
                    maxT = t;
                }
            }
            boolean contour1IsLast = (contour1.getPeakScaleFreeLength() == maxT);
            boolean contour1IsFirst = (contour1.getPeakScaleFreeLength() == minT);
            
            for (int index2 = 0; index2 < c2.size(); index2++) {
            
                CurvatureScaleSpaceContour contour2 = c2.get(index2);
                
                // find this contour's place within the parent edge's contours
                // to correct shift due to wrapping around 1 to 0 or vice versa                
                minT = Float.MAX_VALUE;
                maxT = Float.MIN_VALUE;
                for (Integer i
                    : curveIndexToC2.get(Integer.valueOf(contour2.getEdgeNumber()))) {
                    CurvatureScaleSpaceContour ei = c2.get(i.intValue());
                    float t = ei.getPeakScaleFreeLength();
                    if (t < minT) {
                        minT = t;
                    }
                    if (t > maxT) {
                        maxT = t;
                    }
                }
                boolean contour2IsLast = (contour2.getPeakScaleFreeLength() == maxT);
                boolean contour2IsFirst = (contour2.getPeakScaleFreeLength() == minT);
                
                TransformationPair obj = new TransformationPair(index1, index2);
                
                /*
                t2 = kScale * t1 + dShift
                sigma2 = kScale * sigma1
                */
                
                double scale = contour2.getPeakSigma()/contour1.getPeakSigma();
                
                // tolerance for within range of '1'?
                if ((scale + 0.05) < 1) {
                    // cannot match for scale < 1 because cost function could
                    // prefer smaller sigma peaks that were not good matches.
                    scalesSomeSmallerThanOne = true;
                    continue;
                }
                
                double shift = contour2.getPeakScaleFreeLength() -
                    (contour1.getPeakScaleFreeLength() * scale);
                
                /*
                correct for wrapping around the scale free axis
                */
                if (contour1IsLast && contour2IsFirst) {
                    shift = (1 - contour1.getPeakScaleFreeLength()) +
                        contour2.getPeakScaleFreeLength();
                } else if (contour1IsFirst && contour2IsLast) {
                    shift = -1 * (contour1.getPeakScaleFreeLength() +
                        (1 - contour2.getPeakScaleFreeLength()));
                }
                
                obj.setScale(scale);
                
                obj.setShift(shift);
                
                List<CurvatureScaleSpaceContour> visited = new 
                    ArrayList<CurvatureScaleSpaceContour>();
                visited.add(contour1);
                visited.add(contour2);
                
                NextContour nc = new NextContour(c1, true, curveIndexToC1,
                    visited);
                nc.addMatchedContours(contour1, contour2);
                
                obj.setNextContour(nc);
                
                double cost = 0;
                
                //(2) calc cost: apply to tallest contours from each curve
                
                Iterator<Entry<Integer, List<Integer> > > iter = 
                    curveIndexToC1.entrySet().iterator();
                
                while (iter.hasNext()) {
                
                    List<Integer> indexes = iter.next().getValue();
            
                    int index1s = indexes.get(0);
                    
                    CurvatureScaleSpaceContour contour1s = c1.get(index1s);
                    
                    while (nc.getMatchedContours1().contains(contour1s)
                        && ((index1s + 1) < c1.size())) {
                        index1s++;
                        contour1s = c1.get(index1s);
                    }
                    if (nc.getMatchedContours1().contains(contour1s)) {
                        continue;
                    }
                                                            
                    /*
                    t2 = kScale * t1 + dShift
                    sigma2 = kScale * sigma1
                    */
                    double sigma2 = scale * contour1s.getPeakSigma();
                    double t2 = (scale * contour1s.getPeakScaleFreeLength()) 
                        + shift;
                    //double t2 = contour1s.getPeakScaleFreeLength() + shift;
                    if (t2 < 0) {
                        t2 = 1 + t2;
                    } else if (t2 > 1) {
                        t2 = t2 - 1;
                    }
                    
                    CurvatureScaleSpaceContour contour2s =
                        findMatchingC2(contour1s.getEdgeNumber(),
                        sigma2, t2, nc);
                 
                    if (contour2s != null) {
                        nc.addMatchedContours(contour1s, contour2s);                   
                    }
                    
                    double cost2 = calculateCost(contour2s, sigma2, t2);                    

                    cost += cost2;
                }
                
                /*
                Penalty for starting a match from a peak which is not the max
                height peak for the edge:
                [last paragraph, section IV. A. of Mokhtarian & Macworth 1896]
                "Since it is desirable to find a match corresponding to the 
                coarse features of the curves, there is a penalty associated 
                with starting a match with a small contour.  
                This penalty is a linear function of the difference in height
                of that contour and the tallest contour of the same scale space
                image and is added to the cost of the match computed when a node
                is created.
                */
                /*if (!isTallestPeakInEdge1) {
                    
                    int c1Idx = curveIndexToC1.get(
                        Integer.valueOf(contour1.getEdgeNumber()))
                        .get(0).intValue();
                    
                    CurvatureScaleSpaceContour ei = c1.get(c1Idx);
                    
                    double penalty = ei.getPeakSigma() - contour1.getPeakSigma();
                    
                    cost += penalty;
                }*/
                double penalty = c1.get(0).getPeakSigma() 
                    - contour1.getPeakSigma();
                cost += penalty;
                
                long costL = (long)(cost * heapKeyFactor);
                
                HeapNode node = new HeapNode(costL);
                
                node.setData(obj);
                
                heap.insert(node);
            }
        }
    }
    
    /**
     * get the curvature scale space images factor of scale between the
     * first set of contours and the second set.
     * @return 
     */
    public double getSolvedScale() {
        return solutionScale;
    }
    
    /**
     * get the curvature scale space images shift between the
     * first set of contours and the second set.
     * @return 
     */
    public double getSolvedShift() {
        return solutionShift;
    }
    
    /**
     * get the curvature scale space images shift between the
     * first set of contours and the second set.
     * @return 
     */
    public double getSolvedCost() {
        return solutionCost;
    }
    
     /**
     * match contours from the first list to the second.  the best scale and
     * shifts between the contour lists can be retrieved with getSolvedScale()
     * and getSolvedShift().  if a solution was found, returns true, else
     * returns false.
     * @param contours1
     * @param contours2
     * @return 
     */
    public boolean matchContours(List<CurvatureScaleSpaceContour> contours1, 
        List<CurvatureScaleSpaceContour> contours2) {
        
        initializeVariables(contours1, contours2);
                
        if (heap.n == 0) {            
            return swapOrderAndMatchContours();            
        }
        
        matchContours();
        
        if (!scalesSomeSmallerThanOne) {
            if (solutionScale < Double.MAX_VALUE) {
                return true;
            } else {
                return false;
            }
        }
        
        // else, some scales were smaller than one, so swap and try again
        // and return the best solution,
        // while also swapping the order back
        
        double origOrderScale = solutionScale;
        double origOrderShift = solutionShift;
        double origOrderCost = solutionCost;

        List<CurvatureScaleSpaceContour> origOrderC1 = 
            new ArrayList<CurvatureScaleSpaceContour>(c1);
        List<CurvatureScaleSpaceContour> origOrderC2 = 
            new ArrayList<CurvatureScaleSpaceContour>(c2);

        Map<Integer, List<Integer> > origOrderCurveIndexToC1 =
            new HashMap<Integer, List<Integer> >(curveIndexToC1);

        Map<Integer, List<Integer> > origOrderCurveIndexToC2 =
            new HashMap<Integer, List<Integer> >(curveIndexToC2);
            
        List<CurvatureScaleSpaceContour> origOrderSolutionMatchedContours1 = 
            new ArrayList<CurvatureScaleSpaceContour>(
            solutionMatchedContours1);

        List<CurvatureScaleSpaceContour> origOrderSolutionMatchedContours2 =
            new ArrayList<CurvatureScaleSpaceContour>(
            solutionMatchedContours2);
    
        initializeVariables(contours2, contours1);
        
        if (heap.n > 0) {
            matchContours();
        }
          
        // compare solutions and re-order the variables
        
        if (solutionScale == Double.MAX_VALUE) {
            
            log.info("the first solution was the best");
            
            solutionScale = origOrderScale;
            solutionShift = origOrderShift;
            solutionCost = origOrderCost;
            
        } else {
            
            /*
            compare the solutions.  
                number of matches and cost...
            TODO: retain both solutions to determine best with the refinement
            using edges?
            */
            
            double normalizedCost1 = origOrderCost/
                (double)origOrderSolutionMatchedContours1.size();

            double normalizedCost2 = solutionCost/
                (double)solutionMatchedContours2.size();
            
            StringBuilder sb = new StringBuilder("first solution:\n");
            sb.append(String.format(" cost=%f  nMatched1=%d  normalizedCost=%f\n"
                , origOrderCost, origOrderSolutionMatchedContours1.size(), 
                normalizedCost1));
            sb.append(String.format(" scale=%f  shift=%f\n", origOrderScale,
                origOrderShift));
            log.info(sb.toString());
            
            sb = new StringBuilder("second solution:\n");
            sb.append(String.format(" cost=%f  nMatched1=%d  normalizedCost=%f\n"
                , solutionCost, solutionMatchedContours2.size(), 
                normalizedCost2));
            sb.append(String.format(" scale=%f  shift=%f\n", solutionScale,
                solutionShift));
            log.info(sb.toString());

            
            if (normalizedCost2 < normalizedCost1) {
                solutionScale = 1./solutionScale;
                solutionShift = 1. - solutionShift;
                
                List<CurvatureScaleSpaceContour> swap3 = solutionMatchedContours1;
                solutionMatchedContours1 = solutionMatchedContours2;
                solutionMatchedContours2 = swap3;        
            } else {
                solutionScale = origOrderScale;
                solutionShift = origOrderShift;
                
                solutionMatchedContours1 = origOrderSolutionMatchedContours1;
                solutionMatchedContours2 = origOrderSolutionMatchedContours2;
            }
        }
                            
        log.info("have solution.  swapping the datasets back and inverting"
            + " the solution for reference frame 1 to reference frame 2");

        List<CurvatureScaleSpaceContour> swap = c1;
        c1 = c2;
        c2 = swap;
        Map<Integer, List<Integer> > swap2 = curveIndexToC1;
        curveIndexToC1 = curveIndexToC2;
        curveIndexToC2 = swap2;
        
        if (solutionScale < Double.MAX_VALUE) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean swapOrderAndMatchContours() {
        
        // swap the order of datasets and try again.
        log.info("initialization resulted in 0 nodes, presumably due to "
            + " scale < 0, so swapping the order and trying again.");

        List<CurvatureScaleSpaceContour> contours1 = 
            new ArrayList<CurvatureScaleSpaceContour>(c1);

        List<CurvatureScaleSpaceContour> contours2 = 
            new ArrayList<CurvatureScaleSpaceContour>(c2);

        initializeVariables(contours2, contours1);
        
        matchContours();
        
        log.info("have solution.  swapping the datasets back and inverting"
            + " the solution for reference frame 1 to reference frame 2");
        
        boolean solved = (solutionScale < Double.MAX_VALUE);
        
        List<CurvatureScaleSpaceContour> swap = c1;
        c1 = c2;
        c2 = swap;
        Map<Integer, List<Integer>> swap2 = curveIndexToC1;
        curveIndexToC1 = curveIndexToC2;
        curveIndexToC2 = swap2;
        List<CurvatureScaleSpaceContour> swap3 = solutionMatchedContours1;
        solutionMatchedContours1 = solutionMatchedContours2;
        solutionMatchedContours2 = swap3;
        solutionScale = 1. / solutionScale;
        solutionShift = 1. - solutionShift;
        
        return solved;
    }

    /**
     * match contours from the first list to the second.  the best scale and
     * shifts between the contour lists can be retrieved with getSolvedScale()
     * and getSolvedShift().  if a solution was found, returns true, else
     * returns false.
     * @return 
     */
    private void matchContours() {
        
        solutionShift = Double.MAX_VALUE;
        solutionScale = Double.MAX_VALUE;
        solutionCost = Double.MAX_VALUE;
        
        if (heap.n == 0) {
            return;
        }
        
        // use a specialization of A* algorithm to apply transformation to 
        // contours for best cost solutions (does not compute all possible 
        // solutions).
        HeapNode minCost = solve();
        
        if (minCost == null) {
            return;
        }
        
        TransformationPair obj = (TransformationPair)minCost.getData();
        
        float shift = (float)obj.getShift();
        float scale = (float)obj.getScale();
     
        solutionShift = shift;
        solutionScale = scale;
        solutionCost = (double)(minCost.getKey()/heapKeyFactor);
  
        NextContour nc = obj.getNextContour();
                
        solutionMatchedContours1 = nc.getMatchedContours1();
        
        solutionMatchedContours2 = nc.getMatchedContours2();
        
    }
    
    public List<CurvatureScaleSpaceContour> getSolutionMatchedContours1() {
        return solutionMatchedContours1;
    }
    
    public List<CurvatureScaleSpaceContour> getSolutionMatchedContours2() {
        return solutionMatchedContours2;
    }
    
    /**
       a specialization of the A* search pattern is used to refine the initial
       solution of best parameters.
       The current best solution is extracted from the min heap as the 
       min cost node.

       The "neighbor" to be visited is a candidate contour from list 1,
       chosen from contours not yet searched for it.  There are rules for 
       selecting the candidate contour.
       The cost of the mid cost node is modified by the results of the 
       application of the transformation parameters to the candidate contour.
        
       The process is repeated until there are no more admissable contours
       for an extracted min cost node.
      
     * @return 
     */
    private HeapNode solve() {
                
        HeapNode u = heap.extractMin();
        
        while (u != null) {
            
            TransformationPair obj = (TransformationPair)u.getData();
        
            NextContour nc = obj.getNextContour();
            
            CurvatureScaleSpaceContour c = c1.get(obj.getContourIndex1());
            int curveIndex = c.getEdgeNumber();
            
            CurvatureScaleSpaceContour contour1s = 
                nc.findTallestContourWithinAScaleSpace(curveIndex);
     
            if ((contour1s == null) || 
                nc.getMatchedContours1().contains(contour1s)) {
                
                //TODO: refactor to improve handling of these indexes to remove
                // possibilities of using them incorrectly
                
                int contourIndex = nc.origContours.indexOf(c);
                
                PairInt target = new PairInt(curveIndex, contourIndex);
                
                contour1s = nc.findTheNextSmallestUnvisitedSibling(target);
                
            }
            
            if (contour1s == null) {                
                return u;
            }
            
            float shift = (float)obj.getShift();
            float scale = (float)obj.getScale();
            
            /*
            t2 = kScale * t1 + dShift
            sigma2 = kScale * sigma1
            */
            double sigma2 = scale * contour1s.getPeakSigma();
            double t2 = (scale * contour1s.getPeakScaleFreeLength()) + shift;
            //double t2 = contour1s.getPeakScaleFreeLength() + shift;
            if (t2 < 0) {
                t2 = 1 + t2;
            } else if (t2 > 1) {
                t2 = t2 - 1;
            }

            CurvatureScaleSpaceContour contour2s
                = findMatchingC2(contour1s.getEdgeNumber(), sigma2, t2, nc);

            if (contour2s != null) {
                nc.addMatchedContours(contour1s, contour2s);
            }
            
            double cost2 = calculateCost(contour2s, sigma2, t2);

            /*
            NOTE: Not sure about applying this penalty for this cost.  
            Empirically validating with tests currently...
            [last paragraph, section IV. A. of Mokhtarian & Macworth 1896]
            "Since it is desirable to find a match corresponding to the coarse
            features of the curves, there is a penalty associated with starting
            a match with a small contour.  
            This penalty is a linear function of the difference in height
            of that contour and the tallest contour of the same scale space
            image and is added to the cost of the match computed when a node
            is created.
            */
            double penalty = c1.get(0).getPeakSigma() 
                - contour1s.getPeakSigma();
            //cost2 += penalty;
            
            u.setData(obj);
            
            u.setKey(u.getKey() + (long)(cost2 * heapKeyFactor));
            
            heap.insert(u);
            
            u = heap.extractMin();
        }
        
        return u;
    }
    
    /**
     * calculate the cost as the as the straight line difference between
     * the closest found contour and the model's sigma and peak.
     * @param contour
     * @param sigma
     * @param scaleFreeLength
     * @return 
     */
    private double calculateCost(CurvatureScaleSpaceContour contour, 
        double sigma, double scaleFreeLength) {
        
        if (contour == null) {
            return sigma;
        }
        /*
        It's not clear whether the cost should include the scale free length
        and sigma or just sigma.
        From Mokhatarian & Mackworth 1986, Section IV, middle of column 1:
        "The average distance between two contours is the average of the 
        distances between the peaks, the right branches, and the left branches. 
        The cost of matching two contours is defined to be the averaged 
        distance between them after one of them has been transformed."
        */

        double ds = sigma - contour.getPeakSigma();
       
        double dt = scaleFreeLength - contour.getPeakScaleFreeLength();
        double len = Math.sqrt(ds*ds + dt*dt);
        
        return len;
    }

    private CurvatureScaleSpaceContour findMatchingC2(
        int edgeNumber1, double sigma2, double t2, NextContour nc) {
        
        int expectedEdgeNumber2 = nc.getMatchedEdgeNumber2(edgeNumber1);

        int idx2;
        
        if (expectedEdgeNumber2 == -1) {
            
            // choose from all of c2
            idx2 = findClosestC2MatchOrderedSearch(sigma2, t2, 
                nc.getMatchedContours2());
            
        } else {
            
            // choose from contours of expectedEdgeNumber2
            List<Integer> indexes = curveIndexToC2.get(
                Integer.valueOf(expectedEdgeNumber2));
            
            idx2 = findClosestC2MatchOrderedSearch(sigma2, t2, 
                indexes, nc.getMatchedContours2());
        }
        
        if (idx2 == -1) {
            return null;
        }

        return c2.get(idx2);
            
    }

    private int findClosestC2MatchOrderedSearch(double sigma, 
        double scaleFreeLength, List<CurvatureScaleSpaceContour> exclude) {
        
        //TODO: use 0.1*sigma? current sigma factor peak center error
        double tolSigma = 0.04*sigma;
        if (tolSigma < 1E-2) {
            tolSigma = 0.1;
        }
        
        // consider wrap around searches too, for scaleFreeLength > 0.5 or 
        // scaleFreeLength < 0.5
        double wrapScaleFreeLength = (scaleFreeLength > 0.5) ?
            scaleFreeLength - 1 : 1 + scaleFreeLength;
        
        double minDiffS = Double.MAX_VALUE;
        double minDiffT = Double.MAX_VALUE;
        int idx = -1;
        
        double minDiff2T = Double.MAX_VALUE;
        int minDiff2TIdx = -1;
        
        double minDiffLen = Double.MAX_VALUE;
        int minDiffLenIdx = -1;
        
        for (int i = 0; i < c2.size(); i++) {
        
            CurvatureScaleSpaceContour c = c2.get(i);
            
            if (exclude.contains(c)) {
                continue;
            }
             
            double diffS = Math.abs(c.getPeakSigma() - sigma);
            double diffT = Math.abs(c.getPeakScaleFreeLength() - 
                scaleFreeLength);
            
            double len = Math.sqrt(diffS * diffS + diffT * diffT);
            if (len < minDiffLen) {
                minDiffLen = len;
                minDiffLenIdx = i;
            }
            
            if ((diffS <= (minDiffS + tolSigma)) && (diffT <= minDiffT)) {
                minDiffS = diffS;
                minDiffT = diffT;
                idx = i;                
            } else if ((diffT < minDiff2T) && (diffS <= (minDiffS + 3*tolSigma))
                ) {
                minDiff2T = diffT;
                minDiff2TIdx = i;
            }
            
            diffT = Math.abs(c.getPeakScaleFreeLength() - 
                wrapScaleFreeLength);
            
            len = Math.sqrt(diffS * diffS + diffT * diffT);
            if (len < minDiffLen) {
                minDiffLen = len;
                minDiffLenIdx = i;
            }
            
            if ((diffS <= (minDiffS + tolSigma)) && (diffT <= minDiffT)) {
                minDiffS = diffS;
                minDiffT = diffT;
                idx = i;                
            } else if ((diffT < minDiff2T) && (diffS <= (minDiffS + 3*tolSigma))
                ) {
                minDiff2T = diffT;
                minDiff2TIdx = i;
            }
        }
        
        if (minDiffS > 0.2*sigma) {
            return minDiffLenIdx;
        }
        
        if (minDiffT <= minDiff2T) {
            return idx;
        }
        
        return minDiff2TIdx;
    }
    
    private int findClosestC2MatchOrderedSearch(double sigma, 
        double scaleFreeLength, List<Integer> c2Indexes,
        List<CurvatureScaleSpaceContour> exclude) {
        
        //TODO: use a percentage of sigma
        double tolSigma = 0.04*sigma;
        if (tolSigma < 1E-2) {
            tolSigma = 0.1;
        }
        
        // consider wrap around searches too, for scaleFreeLength > 0.5 or 
        // scaleFreeLength < 0.5
        double wrapScaleFreeLength = (scaleFreeLength > 0.5) ?
            scaleFreeLength - 1 : 1 + scaleFreeLength;
        
        double minDiffS = Double.MAX_VALUE;
        double minDiffT = Double.MAX_VALUE;
        int idx = -1;
        
        double minDiff2T = Double.MAX_VALUE;
        int minDiff2TIdx = -1;
        
        double minDiffLen = Double.MAX_VALUE;
        int minDiffLenIdx = -1;
        
        for (Integer i : c2Indexes) {
        
            CurvatureScaleSpaceContour c = c2.get(i.intValue());
            
            if (exclude.contains(c)) {
                continue;
            }
            
            double diffS = Math.abs(c.getPeakSigma() - sigma);
            double diffT = Math.abs(c.getPeakScaleFreeLength() - 
                scaleFreeLength);
            
            double len = Math.sqrt(diffS * diffS + diffT * diffT);
            if (len < minDiffLen) {
                minDiffLen = len;
                minDiffLenIdx = i;
            }
            
            if ((diffS <= (minDiffS + tolSigma)) && (diffT <= minDiffT)) {
                minDiffS = diffS;
                minDiffT = diffT;
                idx = i.intValue();                
            } else if ((diffT < minDiff2T) && (diffS <= (minDiffS + 3*tolSigma))
                ) {
                minDiff2T = diffT;
                minDiff2TIdx = i.intValue();
            }
            
            diffT = Math.abs(c.getPeakScaleFreeLength() - 
                wrapScaleFreeLength);
            
            len = Math.sqrt(diffS * diffS + diffT * diffT);
            if (len < minDiffLen) {
                minDiffLen = len;
                minDiffLenIdx = i;
            }
            
            if ((diffS <= (minDiffS + tolSigma)) && (diffT <= minDiffT)) {
                minDiffS = diffS;
                minDiffT = diffT;
                idx = i.intValue();                
            } else if ((diffT < minDiff2T) && (diffS <= (minDiffS + 3*tolSigma))
                ) {
                minDiff2T = diffT;
                minDiff2TIdx = i.intValue();
            }
        }
        
        if (minDiffS > 0.2*sigma) {
            return minDiffLenIdx;
        }
        
        if (minDiffT <= minDiff2T) {
            return idx;
        }
        
        return minDiff2TIdx;
    }
    
}
