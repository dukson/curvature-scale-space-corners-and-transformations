package algorithms.imageProcessing;

import algorithms.util.PairIntArray;
import algorithms.util.PairIntArrayWithColor;

/**
 *
 * @author nichole
 */
public class Kernel1DHelper {
   
    /**
     * convolve point[xyIdx] with the kernel g.  if calcX is true, the 
     * point xPoints[xyIdx] is used, else yPoints[xyIdx].
     * minPointsValue is used for 'mu', the location parameter in the
     * Gaussian during the convolution.  Note that for extremely large
     * range of point values, minPointsValue should probably the median
     * of the points.
     * @param curve
     * @param cIndex
     * @param g
     * @param calcX
     * @return 
     */
    public double convolvePointWithKernel(PairIntArray curve, int cIndex,
        float[] g, final boolean calcX) {

        int h = g.length >> 1;

        double sum = 0;

        int curveLength = curve.getN();
        
        boolean isClosedCurved = (curve instanceof PairIntArrayWithColor)
            && (((PairIntArrayWithColor) curve).getColor() == 1);
            
        /*
        correct for the edges.
        Can try:
            (1) wrap around - If this is a closed curve, use the points on the
                 other end to continue matching the kernel point for point.
                 ** this is what will use for closed curves **
            (2) expand/pad - expand to fill the area large enough to match
                the entire filter by:
                    -- replicating the last point
                    -- or reflecting the previous points around this boundary
                       ** this is the one will use for open curves **
                    -- or pad with zeroes.  (this one does NOT work well  it's
                       the same as 'crop the area')
            (3) crop the area.  this involves re-normalization of the percentage
                 of the kernel which was used and can introduce large errors
                 thru division by a very small number for example, so don't use
                 it if possible.
        */
                
        for (int gIdx = 0; gIdx < g.length; gIdx++) {

            float gg = g[gIdx];

            if (gg == 0) {
                continue;
            }
            
            int x = gIdx - h;

            int curveIdx = cIndex + x;

            if (curveIdx < 0) {
                
                if (isClosedCurved) {
                    // wrap around
                    /*
                    n-4 n-3 n-2 n-1  0   1   2   3   .   .   .  n-1
                     _   _   _   _   @   @   @   @   @   @   @   @
                    */
                    // for the rare case when the kernel is so much larger than
                    //   the edge, will need to iterate
                    while (curveIdx < 0) {
                        curveIdx = curve.getN() + curveIdx;
                    }
                } else {
                    //TODO: revisit this for range of kernel sizes vs edge sizes
                    // replicate
                    curveIdx = -1*curveIdx - 1;
                    if (curveIdx > (curveLength - 1)) {
                        curveIdx = curveLength - 1;
                    }
                }
            } else if (curveIdx >= (curveLength)) {
                if (isClosedCurved) {
                    // wrap around
                    /*
                    0   1   2   3   .   .   .  n-1  0   1   2   3   4
                    @   @   @   @   @   @   @   @   _   _   _   _   _
                    */
                    // for the rare case when the kernel is so much larger than
                    //   the edge, will need to iterate
                    while (curveIdx >= (curveLength)) {
                        curveIdx = curveIdx - curve.getN();
                    }
                } else {
                    //TODO: revisit this for range of kernel sizes vs edge sizes
                    int diff = curveIdx - curveLength;
                    curveIdx = curveLength - diff - 1;
                    if (curveIdx < 0) {
                        curveIdx = 0;
                    }
                }
            }
            
            float point;
            
            if (calcX) {
                point = curve.getX(curveIdx);
            } else {
                point = curve.getY(curveIdx);
            }

            sum += (point * gg);
        }

        return sum;
    }

    /**
     * convolve point[xyIdx] with the kernel g along a column if calcX is true.
     * @param img
     * @param col
     * @param row
     * @param g
     * @param calcX
     * @return 
     */
    public double convolvePointWithKernel(final GreyscaleImage img, int col, 
        int row, float[] g, final boolean calcX) {

        int h = g.length >> 1;

        double sum = 0;

        int len = calcX ? img.getWidth() : img.getHeight();
                
        for (int gIdx = 0; gIdx < g.length; gIdx++) {

            float gg = g[gIdx];

            if (gg == 0) {
                continue;
            }
            
            int idx = gIdx - h;

            int cIdx = calcX ? (col + idx) : (row + idx);

            if (cIdx < 0) {
                // replicate
                cIdx = -1*cIdx - 1;
                if (cIdx > (len - 1)) {
                    cIdx = len - 1;
                }
            } else if (cIdx >= (len)) {
                //TODO: revisit this for range of kernel sizes vs edge sizes
                int diff = cIdx - len;
                cIdx = len - diff - 1;
                if (cIdx < 0) {
                    cIdx = 0;
                }
            }
            
            float point;
            
            if (calcX) {
                // keep row constant
                point = img.getValue(cIdx, row);
            } else {
                // keep col constant
                point = img.getValue(col, cIdx);
            }

            sum += (point * gg);
        }

        return sum;
    }
}
