package algorithms.imageProcessing;

import algorithms.util.ResourceFinder;
import algorithms.util.PairIntArray;
import java.io.IOException;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author nichole
 */
public class InflectionMapperOneObjectTest {
    
    private Logger log = Logger.getLogger(this.getClass().getName());

    public InflectionMapperOneObjectTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testMap() throws Exception {
        
        String[] rotDegreesList = new String[]{"20", "45", "60", "110", "160",
            "135", "180", "210", "225", "255", "280", "315", "335"
        };
        
        for (boolean swapDueToScale : new boolean[]{true, false}) {
        //boolean swapDueToScale = false;
            for (String rotDegrees : rotDegreesList) {

                /*
                if (!rotDegrees.equals("315")) {
                    continue;
                }
                */

                String fileName1 = "closed_curve.png";
                String filePath1 = ResourceFinder.findFileInTestResources(fileName1);

                //String fileName2 = "closed_curve_translate.png";
                //String fileName2 = "closed_curve_translate_scale.png";
                String fileName2 = "closed_curve_translate_scale_rotate" + rotDegrees 
                    + ".png";

                String filePath2 = ResourceFinder.findFileInTestResources(fileName2);

                if (swapDueToScale) {
                    String swap = filePath1;
                    filePath1 = filePath2;
                    filePath2 = swap;
                }
                
                GreyscaleImage img1 = ImageIOHelper.readImageAsGrayScaleG(filePath1);
                GreyscaleImage img2 = ImageIOHelper.readImageAsGrayScaleG(filePath2);
                
                double centroidX1 = img1.getWidth() >> 1;
                double centroidY1 = img1.getHeight() >> 1;

                CurvatureScaleSpaceInflectionMapper mapper = new 
                    CurvatureScaleSpaceInflectionMapper(img1, img2);

                mapper.useLineDrawingLineMode();

                mapper.useDebugMode();

                mapper.setToRefineTransformations();

                TransformationParameters transformationParams = 
                    mapper.createEuclideanTransformation();

                assertNotNull(transformationParams);

                float rotDeg = transformationParams.getRotationInDegrees();

                float scale = transformationParams.getScale();

                int nEdges1 = mapper.getEdges1InOriginalReferenceFrame().size();
                PairIntArray[] edges1 = 
                    mapper.getEdges1InOriginalReferenceFrame().toArray(
                    new PairIntArray[nEdges1]);

                Transformer transformer = new Transformer();
                PairIntArray[] transformedEdges = 
                    transformer.applyTransformation(transformationParams, 
                        edges1, centroidX1, centroidY1);

                img2 = ImageIOHelper.readImageAsGrayScaleG(filePath2);

                debugDisplay(transformedEdges, img2.copyImageToGreen(), rotDegrees);

                double expectedRotDeg = Float.valueOf(rotDegrees).floatValue();

                if (!swapDueToScale) {
                    expectedRotDeg = 360 - expectedRotDeg;
                }

                double foundRotDeg = rotDeg;

                log.info("PARAMS: " + transformationParams.toString() 
                    + "\nEXPECTED=" + rotDegrees + " (" + expectedRotDeg + ")");

                assertTrue(Math.abs(expectedRotDeg - foundRotDeg) < 10.f);

                if (rotDegrees.equals("135")) {
                    assertTrue(Math.abs(scale - 1.0) < 0.15);
                } else {
                    if (swapDueToScale) {
                        assertTrue(Math.abs(scale - (1./1.3)) < 0.15);
                    } else {
                        assertTrue(Math.abs(scale - 1.3) < 0.15);
                    }
                }
            }
        }
    }
   
    private void debugDisplay(PairIntArray[] transformedEdges,
        Image img, String rotDegrees) throws IOException {
        
        for (int i = 0; i < transformedEdges.length; i++) {
            PairIntArray edge = transformedEdges[i];
            debugAddCurveToImage(edge, img, 0, 255, 255, 255);
        }     
        
        String dirPath = ResourceFinder.findDirectory("bin");
        
        ImageIOHelper.writeOutputImage(
            dirPath + "/transformed_edges_" + rotDegrees + ".png", img);
    }
    
    private void debugAddCurveToImage(PairIntArray edge, Image input, 
        int nExtraForDot, int rClr, int gClr, int bClr) {
        
        for (int i = 0; i < edge.getN(); i++) {
            int x = edge.getX(i);
            int y = edge.getY(i);
            for (int dx = (-1*nExtraForDot); dx < (nExtraForDot + 1); dx++) {
                float xx = x + dx;
                if ((xx > -1) && (xx < (input.getWidth() - 1))) {
                    for (int dy = (-1*nExtraForDot); dy < (nExtraForDot + 1); 
                        dy++) {
                        float yy = y + dy;
                        if ((yy > -1) && (yy < (input.getHeight() - 1))) {
                            input.setRGB((int)xx, (int)yy, rClr, gClr, bClr);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        
        try {
            
            InflectionMapperOneObjectTest test = 
                new InflectionMapperOneObjectTest();
                        
            test.testMap();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: " + e.getMessage());
        }
    }
}
