package algorithms.compGeometry.clustering;

import algorithms.imageProcessing.GreyscaleImage;
import java.security.SecureRandom;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author nichole
 */
public class KMeansPlusPlusTest extends TestCase {
    
    protected Logger log = Logger.getLogger(this.getClass().getName());
    
    public KMeansPlusPlusTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testChooseRandomlyFromNumbersPresentByProbability() throws 
        Exception {

        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        long seed = System.currentTimeMillis();
        System.out.println("seed=" + seed);
        sr.setSeed(seed);

        int[] distOfSeeds = new int[]{5, 25, 125};
        int[] indexOfDistOfSeeds = new int[] {0,1,2};

        int[] indexesAlreadyChosen = new int[0];
        int nIndexesAlreadyChosen = 0;
        
        KMeansPlusPlus instance = new KMeansPlusPlus();

        // wanting to check that the frequency of returned values resembles probability

        int nThrows = 1000;
        int[] chosenIndexes = new int[nThrows];
        int[] countsOfIndexes = new int[distOfSeeds.length];

        for (int i = 0; i < nThrows; i++) {
            
            int chosenIndex = instance.chooseRandomlyFromNumbersPresentByProbability(
                distOfSeeds, indexOfDistOfSeeds, sr, 
                indexesAlreadyChosen, nIndexesAlreadyChosen);

            chosenIndexes[i] = chosenIndex;

            countsOfIndexes[chosenIndex]++;
        }
        
        /*
         *                                | df |^2               | df |^2         df   df
         *      (sigma_f)^2 =  (sigma_a)^2|----|   +  (sigma_b)^2|----|    +  2 * -- * -- * cov_ab
         *                                | da |                 | db |           da   db
         * 
         *      For uncorrelated variables the covariance terms are zero.
         * 
         *      If f = a / b, and a and b are not correlated, we have:
         *          sigma^2  =  aError^2*(1/b)^2  +  bError^2*(-a/b^2)^2 
                             =  aError^2*(1/b^2)  +  bError^2*(a^2/b^4)
        
                     for aError, approximating with sqrt(a)
                                 ...similar for bError
        */
        // roughly check proportions
        int totalChosenCount = 0;
        float totalDist = 0;
        for (int i = 0; i < distOfSeeds.length; i++) {
            totalChosenCount += countsOfIndexes[i];
            totalDist += distOfSeeds[i];
        }

        for (int i = 0; i < distOfSeeds.length; i++) {

            /*
            float a = (float)countsOfIndexes[i];
            float b = (float)totalChosenCount;            
            float t = a/b;
            double errsq = (a*1./(b*b)) + (a*a/Math.pow(b, 3));            
            double se = Math.sqrt(errsq);
            */
            
            float a = (float)distOfSeeds[i];
            float b = totalDist;
            double expected = a/b;
            double errsq = (a*1./(b*b)) + (a*a/Math.pow(b, 3));
            double se = Math.sqrt(errsq);
            
            float t = (float)countsOfIndexes[i]/(float)totalChosenCount;
            
            log.info("expected proportion=" + expected + " result=" + t 
                + " standard err=" + se);
            
            // 95% confidence intervals:
            double upper = expected + (1.96 * se);
            double lower = expected - (1.96 * se);
            
            assertTrue((t >= lower) && (t <= upper));
        }
    }

    public void test0() throws Exception {

        /*
        make k clusters in range 0 to 255, with dist from center < 10
        25
        100
        150
        180
        */
        int[] v = new int[]{25, 26, 24, 30, 20, 21, 29, 25, 
            100, 101, 99, 102, 98, 103, 97, 100, 100, 100,
            150, 149, 151, 148, 152, 147, 153, 150,
            180, 179, 181, 178, 182, 177, 183, 176, 184, 180
        };
        
        int[] expected = new int[]{25, 100, 150, 180};

        GreyscaleImage img = new GreyscaleImage(6, 6);
        int idx = 0;
        for (int row = 0; row < img.getHeight(); row++) {
            for (int col = 0; col < img.getWidth(); col++) {
                img.setValue(col, row, v[idx]);
                idx++;
            }
        }
        
        int k = 4;

        KMeansPlusPlus instance = new KMeansPlusPlus();
        instance.computeMeans(k, img);

        float[] std = instance.getStandardDeviationsFromCenters();
        int[] centers = instance.getCenters();
        int[] nPerSeed = instance.getNumberOfPointsPerSeedCell();
        
        assertNotNull(std);
        assertNotNull(centers);
        assertNotNull(nPerSeed);
       
        for (int i = 0; i < centers.length; i++) {
            log.info(String.format("centers[%d]=%d std=%f  expected=%d", 
                i, centers[i], std[i], expected[i]));
            
            // 95% confidence intervals:
            double upper = centers[i] + 2*std[i];
            double lower = centers[i] - 2*std[i];
            
            assertTrue((expected[i] >= lower) && (expected[i] <= upper));
        }
        
        int count = 0;
        for (int i = 0; i < nPerSeed.length; i++) {
            count += nPerSeed[i];
            log.info(i + ") " + nPerSeed[i]);
        }
        assertTrue(count == v.length);

    }

}
