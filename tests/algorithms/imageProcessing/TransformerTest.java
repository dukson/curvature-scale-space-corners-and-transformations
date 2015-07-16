package algorithms.imageProcessing;

import algorithms.util.PairFloatArray;
import algorithms.util.PairIntArray;
import java.security.SecureRandom;
import junit.framework.TestCase;
import static org.junit.Assert.*;

/**
 *
 * @author nichole
 */
public class TransformerTest extends TestCase {
    
    public TransformerTest() {
    }
    
    public void testSwapReferenceFramesWRTOrigin() throws Exception {
        
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        long seed = System.currentTimeMillis();
        sr.setSeed(seed);
        
        MatchedPointsTransformationCalculator tc = 
            new MatchedPointsTransformationCalculator();
        
        Transformer transformer = new Transformer();
        
        int n = 100;
        for (int i = 0; i < n; ++i) {
            int x = sr.nextInt(1000);
            int y = sr.nextInt(1000);
            float scale = sr.nextFloat();
            if (sr.nextBoolean()) {
                scale = 1.f/scale;
            }
            int rotDeg = sr.nextInt(360);
            float transX = sr.nextInt(1000);
            float transY = sr.nextInt(1000);
            
            TransformationParameters params = new TransformationParameters();
            params.setRotationInDegrees(rotDeg);
            params.setScale(scale);
            params.setTranslationX(transX);
            params.setTranslationY(transY);
            
            TransformationParameters revParams = tc.swapReferenceFrames(params);
            
            double[] xyTransformed = transformer.applyTransformation(params, 
                x, y);
            
            double[] xyTransformedRevTransformed = 
                transformer.applyTransformation(
                revParams, (float)xyTransformed[0], (float)xyTransformed[1]);
            
            assertTrue(Math.abs(xyTransformedRevTransformed[0] - x) < 1);
            
            assertTrue(Math.abs(xyTransformedRevTransformed[1] - y) < 1);
        }

    }

    public void testApplyTransformation2_0() {
        
        float rotInDegrees, scale, transX, transY;
        TransformationParameters params;
        int image1Width, image1Height;
        
        rotInDegrees = 0;
        scale = 1;
        transX = 0;
        transY = 0;
        image1Width = 100;
        image1Height = 100;
        params = new TransformationParameters();
        params.setRotationInDegrees(rotInDegrees);
        params.setScale(scale);
        params.setTranslationX(transX);
        params.setTranslationX(transY);
        
        PairIntArray set1 = new PairIntArray();
        for (int i = 0; i < 10; ++i) {
            set1.add(i, i);
        }
        
        Transformer transformer = new Transformer();
        
        PairFloatArray transformed1 = 
            transformer.applyTransformation2(rotInDegrees*Math.PI/180.,
                scale, transX, transY,
                image1Width >> 1, image1Height >> 1, set1);
        
        PairIntArray set2 = new PairIntArray();
        for (int i = 0; i < transformed1.getN(); ++i) {
            set2.add(Math.round(transformed1.getX(i)), Math.round(transformed1.getY(i)));
        }
        
        PointMatcher pm = new PointMatcher();
        
        TransformationPointFit checkFit = 
            pm.evaluateFitForUnMatchedTransformedOptimal(
            params, transformed1,
            set2, 2, 2);
        
        int nMaxMatchable = Math.max(transformed1.getN(), set2.getN());
        
        assertTrue(
            Math.abs(checkFit.getParameters().getRotationInRadians()
                - params.getRotationInRadians()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getScale()
                - params.getScale()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationX()
                - params.getTranslationX()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationY()
                - params.getTranslationY()) < 0.1);
        assertTrue(checkFit.getNumberOfMatchedPoints()
            == nMaxMatchable);
        assertTrue(checkFit.getMeanDistFromModel() < 0.1);
        assertTrue(checkFit.getStDevFromMean() < 0.1);
        
        //--------------------------------
        
        set2.reverse();
        
        checkFit = 
            pm.evaluateFitForUnMatchedTransformedOptimal(
            params, transformed1,
            set2, 2, 2);
                
        assertTrue(
            Math.abs(checkFit.getParameters().getRotationInRadians()
                - params.getRotationInRadians()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getScale()
                - params.getScale()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationX()
                - params.getTranslationX()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationY()
                - params.getTranslationY()) < 0.1);
        assertTrue(checkFit.getNumberOfMatchedPoints()
            == nMaxMatchable);
        assertTrue(checkFit.getMeanDistFromModel() < 0.1);
        assertTrue(checkFit.getStDevFromMean() < 0.1);
    }
    
    public void testApplyTransformation2_1() {
        
        float rotInDegrees, scale, transX, transY, rotInRadians;
        TransformationParameters params;
        int image1Width, image1Height;
        
        rotInDegrees = 10;
        rotInRadians = (float)(rotInDegrees * Math.PI/180.f);
        scale = 1;
        transX = 0;
        transY = 0;
        image1Width = 100;
        image1Height = 100;
        params = new TransformationParameters();
        params.setRotationInDegrees(rotInDegrees);
        params.setScale(scale);
        params.setTranslationX(transX);
        params.setTranslationX(transY);
        
        PairIntArray set1 = new PairIntArray();
        for (int i = 0; i < 10; ++i) {
            set1.add(i, i);
        }
        
        int centroidX = image1Width >> 1;
        int centroidY = image1Height >> 1;
        
        PairIntArray set2 = new PairIntArray();
        for (int i = 0; i < 10; ++i) {
           int x = set1.getX(i);
           int y = set1.getY(i);
           
           double xr = centroidX + (x - centroidX) * Math.cos(rotInRadians)
               + (y - centroidY) * Math.sin(rotInRadians);
           
           double yr = centroidY 
               + ((-(x - centroidX) * Math.sin(rotInRadians)) 
               + ((y - centroidY) *  Math.cos(rotInRadians)));
           
           set2.add((int)Math.round(xr), (int)Math.round(yr));
        }
        
        Transformer transformer = new Transformer();
        
        PairFloatArray transformed1 = 
            transformer.applyTransformation2(rotInRadians,
                scale, transX, transY,
                image1Width >> 1, image1Height >> 1, set1);
        
        PointMatcher pm = new PointMatcher();
        
        TransformationPointFit checkFit = 
            pm.evaluateFitForUnMatchedTransformedOptimal(
            params, transformed1,
            set2, 2, 2);
        
        int nMaxMatchable = Math.max(transformed1.getN(), set2.getN());
        
        assertTrue(
            Math.abs(checkFit.getParameters().getRotationInRadians()
                - params.getRotationInRadians()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getScale()
                - params.getScale()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationX()
                - params.getTranslationX()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationY()
                - params.getTranslationY()) < 0.1);
        assertTrue(checkFit.getNumberOfMatchedPoints()
            == nMaxMatchable);
        assertTrue(checkFit.getMeanDistFromModel() < 1);
        assertTrue(checkFit.getStDevFromMean() < 0.5);
        
        //--------------------------------
        
        set2.reverse();
        
        checkFit = 
            pm.evaluateFitForUnMatchedTransformedOptimal(
            params, transformed1,
            set2, 2, 2);
                
        assertTrue(
            Math.abs(checkFit.getParameters().getRotationInRadians()
                - params.getRotationInRadians()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getScale()
                - params.getScale()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationX()
                - params.getTranslationX()) < 0.1);
        assertTrue(
            Math.abs(checkFit.getParameters().getTranslationY()
                - params.getTranslationY()) < 0.1);
        assertTrue(checkFit.getNumberOfMatchedPoints()
            == nMaxMatchable);
        assertTrue(checkFit.getMeanDistFromModel() < 1);
        assertTrue(checkFit.getStDevFromMean() < 0.5);
    }

}
