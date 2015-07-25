package algorithms.imageProcessing;

import algorithms.util.PairFloatArray;
import algorithms.util.PairIntArray;
import algorithms.util.ResourceFinder;
import java.awt.Color;
import java.io.IOException;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.ejml.simple.SimpleMatrix;
import static org.junit.Assert.*;

/**
 *
 * @author nichole
 */
public class RANSACSolverTest extends TestCase {
    
    private Logger log = Logger.getLogger(this.getClass().getName());
    
    public RANSACSolverTest() {
    }

    public void testRANSAC() throws Exception {
    
        //TODO: add the reference for this data here.
        
        PairFloatArray leftTrueMatches = new PairFloatArray();
        PairFloatArray rightTrueMatches = new PairFloatArray();
        getMertonCollege10TrueMatches(leftTrueMatches, rightTrueMatches);
        
        PairFloatArray leftFalseMatches = new PairFloatArray();
        PairFloatArray rightFalseMatches = new PairFloatArray();
        getMertonCollegeFalseMatch1(leftFalseMatches, rightFalseMatches);
        getMertonCollegeFalseMatch2(leftFalseMatches, rightFalseMatches);
        getMertonCollegeFalseMatch3(leftFalseMatches, rightFalseMatches);
        
        PairFloatArray leftTruePlusFalse = leftTrueMatches.copy();
        PairFloatArray rightTruePlusFalse = rightTrueMatches.copy();
        getMertonCollegeFalseMatch1(leftTruePlusFalse, rightTruePlusFalse);
        getMertonCollegeFalseMatch2(leftTruePlusFalse, rightTruePlusFalse);
        getMertonCollegeFalseMatch3(leftTruePlusFalse, rightTruePlusFalse);
        
        PairFloatArray outputLeft = new PairFloatArray(); 
        PairFloatArray outputRight = new PairFloatArray();
        
        RANSACSolver solver = new RANSACSolver();
        
        StereoProjectionTransformerFit fit = solver.calculateEpipolarProjection(
            leftTruePlusFalse, rightTruePlusFalse, outputLeft, outputRight);
        
        assertNotNull(fit);
        
        log.info("fit=" + fit.toString());
        log.info(" fm=" + fit.getFundamentalMatrix().toString());

        String fileName1 = "merton_college_I_001.jpg";
        String fileName2 = "merton_college_I_002.jpg";
        String filePath1 = ResourceFinder.findFileInTestResources(fileName1);
        String filePath2 = ResourceFinder.findFileInTestResources(fileName2);

        Image img1 = ImageIOHelper.readImageAsGrayScale(filePath1);
        int image1Width = img1.getWidth();
        int image1Height = img1.getHeight();
        Image img2 = ImageIOHelper.readImageAsGrayScale(filePath2);
        int image2Width = img2.getWidth();
        int image2Height = img2.getHeight();

        overplotEpipolarLines(fit.getFundamentalMatrix(), outputLeft, outputRight,
            img1, img2, 
            image1Width, image1Height, image2Width, image2Height, 
            Integer.valueOf(0).toString()); 
        
        int n = outputLeft.getN();
        
        assertTrue(n == leftTrueMatches.getN());
        assertTrue(outputRight.getN() == rightTrueMatches.getN());
        
        for (int i = 0; i < outputLeft.getN(); ++i) {
            float xL = outputLeft.getX(i);
            float yL = outputLeft.getY(i);
            float xR = outputRight.getX(i);
            float yR = outputRight.getY(i);
            boolean removed = false;
            for (int j = 0; j < leftTrueMatches.getN(); ++j) {
                float diffXL = Math.abs(xL - leftTrueMatches.getX(j));
                float diffYL = Math.abs(yL - leftTrueMatches.getY(j));
                float diffXR = Math.abs(xR - rightTrueMatches.getX(j));
                float diffYR = Math.abs(yR - rightTrueMatches.getY(j));
                if ((diffXL < 0.1) && (diffYL < 0.1) && (diffXR < 0.1) && (diffYR < 0.1)) {
                    leftTrueMatches.removeRange(j, j);
                    rightTrueMatches.removeRange(j, j);
                    removed = true;
                    break;
                }
            }
            assertTrue(removed);
        }
        assertTrue(leftTrueMatches.getN() == 0);
        assertTrue(rightTrueMatches.getN() == 0);
        
        
        /*
        solution for 7pt epipolar when points are not normalized:
        INFO: fit=Type = dense , numRows = 3 , numCols = 3
         [junit] -0.000  -0.000  -0.006  
         [junit]  0.000  -0.000   0.062  
         [junit]  0.004  -0.071   1.000 
        
        solution when all 10 are given and the first randomly chosen among
        them are used:
                  -0.000  -0.000   0.000  
          [junit]  0.000   0.000   0.036  
          [junit] -0.002  -0.045   1.000 
        */
        
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(0, 0) - 0) < 0.005);
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(1, 0) - 0) < 0.005);
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(2, 0) - 0.004) < 0.01);
        
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(0, 1) - 0) < 0.005);
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(1, 1) - 0) < 0.005);
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(2, 1) - -0.071) < 0.1);
        
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(0, 2) - -0.006) < 0.01);
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(1, 2) - 0.062) < 0.1);
        assertTrue(Math.abs(fit.getFundamentalMatrix().get(2, 2) - 1.0) < 0.005);
    }
    
    protected void getMertonCollege10TrueMatches(PairFloatArray left, 
        PairFloatArray right) {
        
        /*
        58, 103   32, 100
        486, 46   474, 49
        845, 127  878, 151
        949, 430  998, 471
        541, 428  533, 460
        225, 453  213, 498
        49, 509   21, 571
        373, 239  365, 258
        737, 305  762, 335
        84, 273   60, 298
        */
        
        left.add(58, 103);  right.add(32, 100);
        left.add(486, 46);   right.add(474, 49);
        left.add(845, 127);   right.add(878, 151);
        left.add(949, 430);   right.add(998, 471);
        left.add(541, 428);   right.add(533, 460);
        left.add(225, 453);   right.add(213, 498);
        left.add(49, 509);   right.add(21, 571);
        left.add(373, 239);   right.add(365, 258);
        left.add(737, 305);   right.add(762, 335);
        left.add(84, 273);   right.add(60, 298);
    }
    
    protected void getMertonCollege7TrueMatches(PairFloatArray left, 
        PairFloatArray right) {
        
        /*
        58, 103   32, 100
        486, 46   474, 49
        845, 127  878, 151
        949, 430  998, 471
        541, 428  533, 460
        225, 453  213, 498
        49, 509   21, 571
        
        */
        
        left.add(58, 103);  right.add(32, 100);
        left.add(486, 46);   right.add(474, 49);
        left.add(845, 127);   right.add(878, 151);
        left.add(949, 430);   right.add(998, 471);
        left.add(541, 428);   right.add(533, 460);
        left.add(225, 453);   right.add(213, 498);
        left.add(49, 509);   right.add(21, 571);
        
    }
    
    protected void getMertonCollegeFalseMatch1(PairFloatArray left, 
        PairFloatArray right) {
        //765, 487   753, 552
        left.add(765, 487);   right.add(753, 552);
    }
    protected void getMertonCollegeFalseMatch2(PairFloatArray left, 
        PairFloatArray right) {
        //253, 141    256, 229
        left.add(253, 141);   right.add(256, 229);
    }
    protected void getMertonCollegeFalseMatch3(PairFloatArray left, 
        PairFloatArray right) {
        //459, 354  432, 525
        left.add(459, 354);   right.add(432, 525);
    }
    
    private void overplotEpipolarLines(SimpleMatrix fm, PairFloatArray set1,
        PairFloatArray set2, Image img1, Image img2, int image1Width,
        int image1Height, int image2Width, int image2Height, String outfileNumber) 
        throws IOException {

        SimpleMatrix input1 =
            StereoProjectionTransformer.rewriteInto3ColumnMatrix(set1);

        SimpleMatrix input2 =
            StereoProjectionTransformer.rewriteInto3ColumnMatrix(set2);

        for (int ii = 0; ii < input1.numCols(); ii++) {
            double x = input1.get(0, ii);
            double y = input1.get(1, ii);
            ImageIOHelper.addPointToImage((float) x, (float) y, img1, 3,
                255, 0, 0);
        }
        for (int ii = 0; ii < input2.numCols(); ii++) {
            double x2 = input2.get(0, ii);
            double y2 = input2.get(1, ii);
            ImageIOHelper.addPointToImage((float) x2, (float) y2, img2, 3,
                255, 0, 0);
        }

        StereoProjectionTransformer spTransformer = new
            StereoProjectionTransformer();

        Color clr = null;
        for (int ii = 0; ii < input2.numCols(); ii++) {
            clr = getColor(clr);
            SimpleMatrix epipolarLinesInLeft = fm.transpose().mult(input2);
            PairIntArray leftLine = spTransformer.getEpipolarLine(
                epipolarLinesInLeft, image1Width, image1Height, ii);
            ImageIOHelper.addCurveToImage(leftLine, img1, 0,
                clr.getRed(), clr.getGreen(), clr.getBlue());
        }

        clr = null;
        for (int ii = 0; ii < input1.numCols(); ii++) {
            clr = getColor(clr);
            SimpleMatrix epipolarLinesInRight = fm.mult(input1);
            PairIntArray rightLine = spTransformer.getEpipolarLine(
                epipolarLinesInRight, img2.getWidth(), img2.getHeight(), ii);
            ImageIOHelper.addCurveToImage(rightLine, img2, 0,
                clr.getRed(), clr.getGreen(), clr.getBlue());
        }

        String dirPath = ResourceFinder.findDirectory("bin");
        ImageIOHelper.writeOutputImage(
            dirPath + "/tmp_m_1_" + outfileNumber + ".png", img1);
        ImageIOHelper.writeOutputImage(
            dirPath + "/tmp_m_2_" + outfileNumber + ".png", img2);
    }
    
    private Color getColor(Color clr) {
        if ((clr == null) || clr.equals(Color.MAGENTA)) {
            return Color.BLUE;
        }
        if (clr.equals(Color.BLUE)) {
            return Color.PINK;
        } else if (clr.equals(Color.PINK)) {
            return Color.GREEN;
        } else if (clr.equals(Color.GREEN)) {
            return Color.RED;
        } else if (clr.equals(Color.RED)) {
            return Color.CYAN;
        } else if (clr.equals(Color.CYAN)) {
            return Color.MAGENTA;
        } else if (clr.equals(Color.MAGENTA)) {
            return Color.LIGHT_GRAY;
        } else {
            return Color.ORANGE;
        }
    }
}
