package algorithms.imageProcessing;

import algorithms.compGeometry.PointInPolygon;
import algorithms.compGeometry.convexHull.GrahamScan;
import algorithms.compGeometry.convexHull.GrahamScanTooFewPointsException;
import algorithms.misc.Histogram;
import algorithms.misc.Misc;
import algorithms.misc.MiscDebug;
import algorithms.misc.MiscMath;
import algorithms.util.PairInt;
import algorithms.util.PairIntArray;
import algorithms.util.PairIntArrayDescendingComparator;
import algorithms.util.ResourceFinder;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

public class ShapeMatcher {

    private Logger log = Logger.getLogger(this.getClass().getName());
    
    public ShapeMatcher() {
    }

    /**
    method to extract general shapes from the images and compare them in order to 
    match points.  It returns a fit to a rough Euclidean transformation.
    NOTE that the images may need pre-processing steps before using this.  For example,
    the Brown & Lowe 200? panoramic images of a mountain need to have the sky masked
    out of the image first.
     * @param image1
     * @param image2
     * @param outputMatched1
     * @param outputMatched2
    */
    public TransformationPointFit findMatchingShapes(ImageExt image1, ImageExt image2,
    PairIntArray outputMatched1, PairIntArray outputMatched2) throws 
        IOException, NoSuchAlgorithmException {
        
        GreyscaleImage img1Grey = image1.copyToGreyscale();
        GreyscaleImage img2Grey = image2.copyToGreyscale();
        
        ImageProcessor imageProcessor = new ImageProcessor();
        
        final boolean performBinning = true;
        int binFactor1 = 1;
        
        int kN = 4;
        boolean performBinarySegmentation = true;
        if (performBinarySegmentation) {
            kN = 2;
        }
        
        /*
        one could start with essentially no limits here and then
        looks at the distribution of resulting contiguous group
        sizes to decide the range to keep.
        For now, choosing limits.
        */
        int smallestGroupLimit = 100;
        //TODO: consider scaling this by image size or by size and res if one
        //  day that information is passed to this method
        int largestGroupLimit = 5000;

        ImageExt img1Cp = (ImageExt)image1.copyImage();
        ImageExt img2Cp = (ImageExt)image2.copyImage();
        
        ImageStatistics stats1 = ImageStatisticsHelper.examineImage(img1Grey, true); 
        ImageStatistics stats2 = ImageStatisticsHelper.examineImage(img2Grey, true); 

        log.info("stats1=" + stats1.toString());
        log.info("stats2=" + stats2.toString());
        
        boolean performHistEq = false;        
        double median1DivMedian2 = stats1.getMedian()/stats2.getMedian();
        double meanDivMedian1 = stats1.getMean()/stats1.getMedian();        
        double meanDivMedian2 = stats2.getMean()/stats2.getMedian();
        if (
            ((median1DivMedian2 > 1) && ((median1DivMedian2 - 1) > 0.2)) ||
            ((median1DivMedian2 < 1) && (median1DivMedian2 < 0.8))) {
            performHistEq = true;
        } else if (
            ((meanDivMedian1 > 1) && ((meanDivMedian1 - 1) > 0.2)) ||
            ((meanDivMedian1 < 1) && (meanDivMedian1 < 0.8))) {
            performHistEq = true;
        } else if (
            ((meanDivMedian2 > 1) && ((meanDivMedian2 - 1) > 0.2)) ||
            ((meanDivMedian2 < 1) && (meanDivMedian2 < 0.8))) {
            performHistEq = true;
        }
        if (performHistEq) {
            HistogramEqualization hEq = new HistogramEqualization(img1Grey);
            hEq.applyFilter();
            hEq = new HistogramEqualization(img2Grey);
            hEq.applyFilter();
            /*HistogramEqualizationForColor hEqC = new HistogramEqualizationForColor(img1Cp);
            hEqC.applyFilter();
            hEqC = new HistogramEqualizationForColor(img2Cp);
            hEqC.applyFilter();*/
        }
        
        if (performBinning) {
            binFactor1 = (int) Math.ceil(
                Math.max((float)img1Grey.getWidth()/200.f,
                (float)img2Grey.getHeight()/200.));
            smallestGroupLimit /= (binFactor1*binFactor1);
            largestGroupLimit /= (binFactor1*binFactor1);
            
            log.info("binFactor1=" + binFactor1);
            
            // prevent from being smaller than needed for a convex hull
            if (smallestGroupLimit < 4) {
                smallestGroupLimit = 4;
            }
            img1Grey = imageProcessor.binImage(img1Grey, binFactor1);
            img2Grey = imageProcessor.binImage(img2Grey, binFactor1);
            img1Cp = imageProcessor.binImage(img1Cp, binFactor1);
            img2Cp = imageProcessor.binImage(img2Cp, binFactor1);
        }
        imageProcessor.applyImageSegmentation(img1Grey, kN);
        imageProcessor.applyImageSegmentation(img2Grey, kN);

        // == contiguous regions within size limits become blobs of interest,
        //    indexed by their intensity levels
        
        Map<Integer, Integer> freqMap1 = Histogram.createAFrequencyMap(img1Grey);
        Map<Integer, Integer> freqMap2 = Histogram.createAFrequencyMap(img2Grey);
        
        Map<Integer, List<PairIntArray>> contigMap1 
            = new HashMap<Integer, List<PairIntArray>>();
        Map<Integer, List<PairIntArray>> contigMap2 
            = new HashMap<Integer, List<PairIntArray>>();
        
        Map<Integer, List<GrahamScan>> hulls1 = 
            new HashMap<Integer, List<GrahamScan>>();
        Map<Integer, List<GrahamScan>> hulls2 = 
            new HashMap<Integer, List<GrahamScan>>();
        
        MiscellaneousCurveHelper curveHelper = new MiscellaneousCurveHelper();
        
        Map<Integer, PairIntArray> hullCentroids1Map = 
            new HashMap<Integer, PairIntArray>();
        Map<Integer, PairIntArray> hullCentroids2Map = 
            new HashMap<Integer, PairIntArray>();
        PairIntArray allHullCentroids1 = new PairIntArray();
        PairIntArray allHullCentroids2 = new PairIntArray();
        
        for (int im = 0; im < 2; ++im) {
            
            Map<Integer, Integer> freqMap = freqMap1;
            Map<Integer, List<PairIntArray>> contigMap = contigMap1;
            Map<Integer, List<GrahamScan>> hulls = hulls1;
            GreyscaleImage imgGrey = img1Grey;
            PairIntArray hullCentroids = allHullCentroids1;
            Map<Integer, PairIntArray> hullCentroidsMap = hullCentroids1Map;
            if (im == 1) {
                freqMap = freqMap2;
                contigMap = contigMap2;
                hulls = hulls2;
                hullCentroids = allHullCentroids2;
                imgGrey = img2Grey;
                hullCentroidsMap = hullCentroids2Map;
            }
 
            for (Entry<Integer, Integer> entry : freqMap.entrySet()) {

                Integer pixValue = entry.getKey();

                DFSContiguousValueFinder finder = new DFSContiguousValueFinder(
                    imgGrey);
                finder.setMinimumNumberInCluster(smallestGroupLimit);
                finder.findGroups(pixValue.intValue());

                int nGroups = finder.getNumberOfGroups();
                List<PairIntArray> list = new ArrayList<PairIntArray>();
                for (int i = 0; i < nGroups; i++) {
                    PairIntArray xy = finder.getXY(i);
                    if (xy.getN() < largestGroupLimit) {
                        list.add(xy);
                    }
                }
                Collections.sort(list, new PairIntArrayDescendingComparator());
                
                // storing the centroids for this intensity level hulls separateley
                PairIntArray pvHullCentroids = new PairIntArray();
                
                // remove hulls with large area on image bounds
                List<Integer> rm = new ArrayList<Integer>();
                
                List<GrahamScan> listHulls = new ArrayList<GrahamScan>();
                for (int i = 0; i < list.size(); ++i) {
                    
                    PairIntArray xy = list.get(i);
                    
                    GrahamScan scan = new GrahamScan();
                    try {
                        float[] x = new float[xy.getN()];
                        float[] y = new float[x.length];
                        for (int ii = 0; ii < x.length; ++ii) {
                            x[ii] = xy.getX(ii);
                            y[ii] = xy.getY(ii);
                        }
                        
                        double[] centroidXY = 
                            curveHelper.calculateXYCentroids(x, y);

                        scan.computeHull(x, y);
                        
                        // if more than one hull point touches the bounds of the
                        // image, the hull is removed because it may be
                        // incomplete.  may need to change this rule later
                        // if it makes the solution too shallow in terms of 
                        // very close points
                        
                        int nBounds = 0;
                        for (int ii = 0; ii < scan.getXHull().length; ++ii) {
                            float xh = scan.getXHull()[ii];
                            float yh = scan.getYHull()[ii];
                            if ((xh == 0) || xh == (imgGrey.getWidth() - 1) ||
                                (yh == 0) || yh == (imgGrey.getHeight() - 1)) {
                                nBounds++;
                            }
                        }
                        if (nBounds > 1) {
                            
                            rm.add(Integer.valueOf(i));
                            
                        } else {
                            
                            listHulls.add(scan);
                        
                            int xh = (int)Math.round(centroidXY[0]);
                            int yh = (int)Math.round(centroidXY[1]);
                            
                            hullCentroids.add(xh, yh);
                            pvHullCentroids.add(xh, yh);
                        }
                        
                    } catch (GrahamScanTooFewPointsException e) {
                        log.severe(e.getMessage());
                    }
                }
                
                for (int i = (rm.size() - 1); i > -1; --i) {
                    int rmIdx = rm.get(i).intValue();
                    list.remove(rmIdx);
                }
                
                contigMap.put(pixValue, list);
                hulls.put(pixValue, listHulls);
                hullCentroidsMap.put(pixValue, pvHullCentroids);
            }
        }
        
        MiscDebug.writeHullImages(img1Grey, hulls1, "1_binned");
        MiscDebug.writeHullImages(img2Grey, hulls2, "2_binned");
        MiscDebug.writeImage(img1Cp, "1_binned_clr");
        MiscDebug.writeImage(img2Cp, "2_binned_clr");
       
        // make corners
        
        if (!performBinning) {
            imageProcessor.blur(img1Grey, 2);
            imageProcessor.blur(img2Grey, 2);
        }
        
        CurvatureScaleSpaceCornerDetector detector = new
            CurvatureScaleSpaceCornerDetector(img1Grey);
        detector.doNotPerformHistogramEqualization();
        detector.findCorners();
        PairIntArray corners1 = detector.getCornersInOriginalReferenceFrame();

        CurvatureScaleSpaceCornerDetector detector2 = new
            CurvatureScaleSpaceCornerDetector(img2Grey);
        detector2.doNotPerformHistogramEqualization();
        detector2.findCorners();
        PairIntArray corners2 = detector2.getCornersInOriginalReferenceFrame();
        
        log.info("n1Corners=" + corners1.getN() + " n2Corners2=" 
            + corners2.getN());
         
        MiscDebug.plotCorners(img1Grey, corners1, "1_corners");
        MiscDebug.plotCorners(img2Grey, corners2, "2_corners");
        
        /*
        If make an assumption that the histogram equalization and then color
        segmentation leaves the images in consistent state w.r.t. similar 
        colors and intensities being displayed similarly, then selection of
        blobs in this way should lead to comparable lists (which are
        subsets of the total, that is the blobs found for intensity level i0
        in image 1 are the ones to match to similar intensity level in image 2).
        
        Using the blobs, that is the hulls above, has reduced the number of
        regions to compare.
        
        Using the centroids of the blobs is appealling, but these will likely
        be in regions free of large gradients so are difficult to distinguish.
        
        Corners filtered to be only those that cross those blob hulls, however,
        should be good features to use.
        
        The problem is that the corners filtered to those lists are still too
        many to use pairwise transformation calculation from every possible
        pair (the process is approx n^4).
        
        So feature matching using patches surrounding the corners is needed and 
        this is an N^2 process but accompanied by many steps for the comparisons.
        
        Note that feature matching instead of straight pairwise transformation
        calculation unfortunately needs an assumption made about the
        scale.
        
        So if one does have a reduced list of corners from the intersection
        with the blob hulls, that is smaller than 30 or 40 in each list
        (especially if near a dozen), then one should prefer to use pairwise
        calculations over feature matching, but feature matching
        can still be used to verify the results.
        */
        
        Map<Integer, PairIntArray> filteredCorners1 = filterToHulls(
            corners1, hulls1);
        
        Map<Integer, PairIntArray> filteredCorners2 = filterToHulls(
            corners1, hulls2);
        
        /*
        use feature description to find similar looking features within
        image2 filtered corners.
        */
        
        /*
        key = intensity level of image 1 contiguous region
        value = map with
                key = point in filteredCorners1
                value = list of similar looking points in filteredCorners2
                        for the same intensity level.
        */
        Map<Integer, Map<PairInt, List<FeatureComparisonStat>>> similarCorners
            = new HashMap<Integer, Map<PairInt, List<FeatureComparisonStat>>>();
        
        // iterate over intensity keys to find the closest matching in intensities
        // to compare those lists
        for (Entry<Integer, PairIntArray> entry : filteredCorners1.entrySet()) {
            
            Integer pixValue1 = entry.getKey();
            
            // find the closest value to it in the image2 map.  should be very
            // similar if image content is similar and pre-processing the same.
            // else, may need to use allHullCentroids1 and allHullCentroids2
            // in one calculation
            
            Integer pixValue2 = null;
            Iterator<Entry<Integer, PairIntArray>> iter = 
                filteredCorners2.entrySet().iterator();
            while (iter.hasNext()) {
                Integer pV = iter.next().getKey();
                if (pixValue2 == null) {
                    pixValue2 = pV;
                } else {
                    int diff = Math.abs(pixValue1.intValue() - pixValue2.intValue());
                    int diffV = Math.abs(pV.intValue() - pixValue2.intValue());
                    if (diffV < diff) {
                        pixValue2 = pV;
                    }
                }
            }
            
            // sets with keys pixValue1 and pixValues should hold common blobs
            // and details
            PairIntArray filtered1 = entry.getValue();
            PairIntArray filtered2 = filteredCorners2.get(pixValue2);
            
            Map<PairInt, List<FeatureComparisonStat>> similar = 
                findSimilarFeatures(img1Cp, img2Cp, filtered1, filtered2);
                        
            similarCorners.put(pixValue1, similar);
        }
        
if (true) {
    return null;
}        
        
        /*
        if need to calculate for all hull centroids, can use the rotation 
        and translation grid based search followed by downhill simplex:
        
        boolean useGreedyMatching = true;
        boolean searchScaleToo = true;
        float scale = 1;
        TransformationPointFit[] starterPoints = pointMatcher.preSearch0Small(
            allHullCentroids1, allHullCentroids2, scale,
            0, 359, useGreedyMatching);
        TransformationPointFit[] fits2 =
            pointMatcher.refineTransformationWithDownhillSimplexWrapper(
            starterPoints, allHullCentroids1, allHullCentroids2, searchScaleToo, 
            useGreedyMatching);
        
        log.info("fit3=" + fits2[0].toString());
        */
        
        TransformationPointFit bestFit00 = null;

        float toleranceTransX = 20;//30;
        float toleranceTransY = toleranceTransX;
        boolean useGreedyMatching = true;
        boolean earlyConvergeReturn = true;
        boolean setsAreMatched = false;
        
        log.info("nAllHulls1=" + allHullCentroids1.getN() 
            + " nAllHulls2=" + allHullCentroids2.getN());
        
        PointMatcher pointMatcher = new PointMatcher();
        
        if ((bestFit00 == null) ||
            (((float)bestFit00.getNumberOfMatchedPoints()
            /(float)bestFit00.getNMaxMatchable()) < 0.5)) {
                        
            // unfortunately, the quicker solution didn't work so need to try
            // to solve it using all corners (though the internal implementation
            // does not try all permutations).
            
            long t0 = System.currentTimeMillis();
            List<TransformationPointFit> fits
                = pointMatcher.calculateEuclideanTransformationUsingPairs(
                    corners1, corners2, toleranceTransX, toleranceTransY,
                    earlyConvergeReturn, useGreedyMatching);
            long t1 = System.currentTimeMillis();
            log.info("calculateEuclideanTransformationUsingPairs seconds="
                + ((t1 - t0) * 1e-3));
            
            // the evaluation has already been tried against all corners
            // so only need to compare the fits.
            
            // this finds best that is usually a scale that is not 1, so
            // if one knows that the scale should be '1', should use the
            // method that solves mostly vertical transformations specifically
            
            for (TransformationPointFit fit : fits) {
                if (pointMatcher.fitIsBetter(bestFit00, fit)) {
                    bestFit00 = fit;
                }
            }
            log.info("best from all corners=" + bestFit00.toString());
        }
        
        // refine the solution 
        float rotHalfRangeInDegrees = 20;
        float rotDeltaInDegrees = 2;
        float transXHalfRange = 40; 
        float transXDelta = 4;
        float transYHalfRange = transXHalfRange; 
        float transYDelta = transXDelta;
        
        long t0 = System.currentTimeMillis();
        TransformationPointFit bestFit = pointMatcher.refineTheTransformation(
            bestFit00.getParameters(), corners1, corners2,
            rotHalfRangeInDegrees, rotDeltaInDegrees,
            transXHalfRange, transXDelta,
            transYHalfRange, transYDelta,
            useGreedyMatching);
        long t1 = System.currentTimeMillis();
        
        log.info(((t1-t0)*1e-3) + "seconds) refined solution=" + bestFit.toString());
        
        //TODO: use the transformation to make matching corner lists, 
        //   perhaps from larger images
        
        return bestFit;
    }

    /**
     * match the given point lists by comparing rotated and dithered blocks
     * surrounding points in the first list to blocks around points in the
     * second list.
     * The runtime complexity is O(N1 * N2), but there are many steps for
     * the rotation and dither operations.
     * @param img1
     * @param img2
     * @param points1
     * @param points2
     * @return 
     */
    protected Map<PairInt, List<FeatureComparisonStat>> findSimilarFeatures(
        ImageExt img1, ImageExt img2, PairIntArray points1, 
        PairIntArray points2) {
        
        if (points1 == null) {
            throw new IllegalArgumentException("points1 cannot be null");
        }
        if (points2 == null) {
            throw new IllegalArgumentException("points2 cannot be null");
        }
        if (points1.getN() != points2.getN()) {
            throw new IllegalArgumentException(
            "points1 must be the same size as points2");
        }
        
        //TODO: NOTE changing this to use gradients in the statistics
        
        /*
         matchFeatures can be used to filter corners to ambigious or unambiguous
        degenerate matches and then pairwise calcs can distiguish between them
        */
        
        /*
        data structure to store partial results:
        
        key = img1 feature coordinates
        value = map with
                key = img2 feature coordinates
                value = map with
                        key = rotation angle
                        value = best of stats for dithering around the primary
                                key and transforming the block to compare to
                                the img2 block for the key rotation
        */
        
        Map<PairInt, Map<PairInt, Map<Float, FeatureComparisonStat>>>
            comparisonMap = new HashMap<PairInt, Map<PairInt, 
            Map<Float, FeatureComparisonStat>>>();
          
        Transformer transformer = new Transformer();
        
        float[][] offsets0 = createNeighborOffsets();

        for (float rotInDeg = 0; rotInDeg < 360; rotInDeg += 22.5f) {

            float[][] offsets = offsets0;
            
            if (rotInDeg > 0) {
                offsets = transformer.transformXY(rotInDeg, offsets);
            }
            
            for (int idx1 = 0; idx1 < points1.getN(); ++idx1) {
                int x1 = points1.getX(idx1);
                int y1 = points1.getY(idx1);
                PairInt p1 = new PairInt(x1, y1);

                for (int idx2 = 0; idx2 < points2.getN(); ++idx2) {
                    int x2 = points2.getX(idx2);
                    int y2 = points2.getY(idx2);
                    PairInt p2 = new PairInt(x2, y2);

                    // dither around (x1, y1) to find best stat
                    FeatureComparisonStat best = null;
                    for (int x1d = (x1 - 1); x1d <= (x1 + 1); ++x1d) {
                        if ((x1d < 0) || (x1d > (img1.getWidth() - 1))) {
                            continue;
                        }
                        for (int y1d = (y1 - 1); y1d <= (y1 + 1); ++y1d) {
                            if ((y1d < 0) || (y1d > (img1.getHeight() - 1))) {
                                continue;
                            }

                            FeatureComparisonStat stat = calculateStat(img1, 
                                img2, x1d, y1d, x2, y2, offsets, offsets0);

                            if (stat != null) {
                                if (best == null) {
                                    best = stat;
                                } else {
                                    if (best.avgDiffPix > stat.avgDiffPix) {
                                        best = stat;
                                    } else if (Math.abs(best.avgDiffPix - stat.avgDiffPix) < 0.1) {
                                        if (best.stDevDiffPix > stat.stDevDiffPix) {
                                            best = stat;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    storeInMap(comparisonMap, p1, p2, rotInDeg, best);                        
                }
            }
        }
        
        //TODO: process the results and simplify them above
        
        return null;
    }
    
    /**
     * create x and y offsets for the neighbor points within 2 pixel radius.
     * The result is a two-dimensional array of length 25 with the first
     * dimension being the x array and the 2nd dimension being the y array.
     * Note that the offset of (0,0) is the first given.
     * @return 
     */
    public float[][] createNeighborOffsets() {
        
        float[][] xyout = new float[25][];
        xyout[0] = new float[2];
        
        int d = 2;
        
        int count = 1;
        for (int x = -d; x <= d; ++x) {
            for (int y = -d; y <= d; ++y) {
                if (x == 0 && y == 0) {
                    continue;
                }
                
                xyout[count] = new float[]{x, y};
                
                count++;
            }
        }
    
        return xyout;
    }

    FeatureComparisonStat calculateStat(Image img1, Image img2, 
        final int x1, final int y1, final int x2, final int y2, 
        final float[][] offsets1, final float[][] offsets2) {
    
        /*
        TODO: this may need to change to use gradients similarly to 
        best practices.
        */
        
        int count = 0;
        float[] rDiff = new float[offsets1.length];
        float[] gDiff = new float[offsets1.length];
        float[] bDiff = new float[offsets1.length];
        float[] rDiv = new float[offsets1.length];
        float[] gDiv = new float[offsets1.length];
        float[] bDiv = new float[offsets1.length];
        for (int i = 0; i < offsets1.length; ++i) {
            int x1P = Math.round(x1 + offsets1[i][0]);
            int y1P = Math.round(y1 + offsets1[i][1]);
            if ((x1P < 0) || (x1P > (img1.getWidth() - 1)) || (y1P < 0) ||
                (y1P > (img1.getHeight() - 1))) {
                continue;
            }
            
            int x2P = Math.round(x2 + offsets2[i][0]);
            int y2P = Math.round(y2 + offsets2[i][1]);
            if ((x2P < 0) || (x2P > (img2.getWidth() - 1)) || (y2P < 0) ||
                (y2P > (img2.getHeight() - 1))) {
                continue;
            }
            
            int idx1 = img1.getInternalIndex(x1P, y1P);
            int idx2 = img1.getInternalIndex(x2P, y2P);
            
            float r1 = img1.getR(idx1);
            float g1 = img1.getG(idx1);
            float b1 = img1.getB(idx1);
            
            float r2 = img2.getR(idx2);
            float g2 = img2.getG(idx2);
            float b2 = img2.getB(idx2);
            
            rDiff[count] = Math.abs(r1 - r2);
            gDiff[count] = Math.abs(g1 - g2);
            bDiff[count] = Math.abs(b1 - b2);
            
            //TODO: reconsider whether to keep the value if all are zero,
            //    because it may be a dead pixel.  
            
            rDiv[count] = (r2 == 0) ? 255 : r1/r2;
            gDiv[count] = (g2 == 0) ? 255 : g1/g2;
            bDiv[count] = (b2 == 0) ? 255 : b1/b2;
            
            count++;
        }
        
        float[] rDiffAvgStDev = MiscMath.getAvgAndStDev(rDiff, count);
        float[] gDiffAvgStDev = MiscMath.getAvgAndStDev(gDiff, count);
        float[] bDiffAvgStDev = MiscMath.getAvgAndStDev(bDiff, count);
        
        float[] rDivAvgStDev = MiscMath.getAvgAndStDev(rDiv, count);
        float[] gDivAvgStDev = MiscMath.getAvgAndStDev(gDiv, count);
        float[] bDivAvgStDev = MiscMath.getAvgAndStDev(bDiv, count);
        
        float avgDiffPix = (rDiffAvgStDev[0] + gDiffAvgStDev[0] + 
            bDiffAvgStDev[0])/3.f;
        
        float avgDivPix = (rDivAvgStDev[0] + gDivAvgStDev[0] + 
            bDivAvgStDev[0])/3.f;
        
        // std deviations add in quadrature
        float stDevDiffPix = (float)Math.sqrt(
            (rDiffAvgStDev[1]*rDiffAvgStDev[1] + 
            gDiffAvgStDev[1]*gDiffAvgStDev[1] + 
            bDiffAvgStDev[1]*bDiffAvgStDev[1])/2.f);
        
        float stDevDivPix = (float)Math.sqrt(
            (rDivAvgStDev[1]*rDivAvgStDev[1] + 
            gDivAvgStDev[1]*gDivAvgStDev[1] + 
            bDivAvgStDev[1]*bDivAvgStDev[1])/2.f);
        
        FeatureComparisonStat stat = new FeatureComparisonStat();
        stat.img1Point = new PairInt(x1, y1);
        stat.img2Point = new PairInt(x2, y2);
        stat.avgDiffPix = avgDiffPix;
        stat.stDevDiffPix = stDevDiffPix;
        stat.avgDivPix = avgDivPix;
        stat.stDevDivPix = stDevDivPix;
        
        return stat;
    }

    private void storeInMap(Map<PairInt, Map<PairInt, Map<Float, 
        FeatureComparisonStat>>> comparisonMap, PairInt p1, PairInt p2, 
        float rotInDeg, FeatureComparisonStat best) {
        
        if (best == null) {
            return;
        }
        
        if (comparisonMap == null) {
            throw new IllegalArgumentException("comparisonMap cannot be null");
        }
        
        Map<PairInt, Map<Float, FeatureComparisonStat>> p1Map = 
            comparisonMap.get(p1);
        
        if (p1Map == null) {
            p1Map = new HashMap<PairInt, Map<Float, FeatureComparisonStat>>();
            comparisonMap.put(p1, p1Map);
        }
        
        Map<Float, FeatureComparisonStat> p2Map = p1Map.get(p2);
        
        if (p2Map == null) {
            p2Map = new HashMap<Float, FeatureComparisonStat>();
            p1Map.put(p2, p2Map);
        }
        
        Float key = Float.valueOf(rotInDeg);
        
        p2Map.put(key, best);
        
    }

    private Map<Integer, PairIntArray> filterToHulls(PairIntArray corners, 
        Map<Integer, List<GrahamScan>> hulls) {
        
        PointInPolygon poly = new PointInPolygon();
        
        Map<Integer, PairIntArray> filteredCorners = new HashMap<Integer, PairIntArray>();
                
        for (Entry<Integer, List<GrahamScan>> entry : hulls.entrySet()) {
            
            Integer pixValue = entry.getKey();
            
            List<GrahamScan> hullList = entry.getValue();
           
            PairIntArray cornersH = new PairIntArray();
            
            for (GrahamScan scan : hullList) {
                
                for (int i = 0; i < corners.getN(); ++i) {
                    
                    int x = corners.getX(i);
                    int y = corners.getY(i);
                    
                    if (poly.isInSimpleCurve(x, y, scan.getXHull(), 
                        scan.getYHull(), scan.getXHull().length)) {
                        
                        cornersH.add(x, y);
                    }
                }
            }
        
            // because convex hulls are used, there may be some points that are
            // in more than one map entry
            filteredCorners.put(pixValue, cornersH);
        }
        
        return filteredCorners;
    }
    
    public static class FeatureComparisonStat {
        PairInt img1Point;
        PairInt img2Point;
        float avgDiffPix;
        float stDevDiffPix;
        float avgDivPix;
        float stDevDivPix;
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("p1=").append(img1Point.toString()).append(" ")
            .append("p2=").append(img2Point.toString()).append("\n")
            .append("avgDiffPix=").append(avgDiffPix)
            .append(" stDevDiffPix=").append(stDevDiffPix)
            .append(" avgDivPix=").append(avgDivPix)
            .append(" stDevDivPix=").append(stDevDivPix);
            return sb.toString();
        }
    }
    
}
