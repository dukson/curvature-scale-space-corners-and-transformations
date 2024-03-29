package algorithms.imageProcessing;

import algorithms.util.PairIntArray;
import java.util.logging.Logger;

/**
 *
 * @author nichole
 */
public class Transformer {
    
    protected Logger log = Logger.getLogger(this.getClass().getName());
    
    /**
     * transform the given edges using the given parameters.
     * 
     * @param params
     * @param edges
     * @param centroidX the horizontal center of the reference frame for edges.  
     * this should be the center of the image if edges points are in the
     * original image reference frame.
     * @param centroidY the vertical center of the reference frame for edges.  
     * this should be the center of the image if edges points are in the
     * original image reference frame.
     * @return 
     */
     public PairIntArray[] applyTransformation(TransformationParameters params,
        PairIntArray[] edges, double centroidX, double centroidY) {
        
        double rotInRadians = params.getRotationInRadians();
        double scale = params.getScale();        
        double translationX = params.getTranslationX();
        double translationY = params.getTranslationY();
        
        return applyTransformation(rotInRadians, scale, translationX,
            translationY, centroidX, centroidY, edges);
     }
     
     /**
     * transform the given edges using the given parameters.
     * 
     * @param params
     * @param edge
     * @param centroidX the horizontal center of the reference frame for edges.  
     * this should be the center of the image if edges points are in the
     * original image reference frame.
     * @param centroidY the vertical center of the reference frame for edges.  
     * this should be the center of the image if edges points are in the
     * original image reference frame.
     * @return
     */
     public PairIntArray applyTransformation(TransformationParameters params,
        PairIntArray edge, double centroidX, double centroidY) {
        
        double rotInRadians = params.getRotationInRadians();
        double scale = params.getScale();        
        double translationX = params.getTranslationX();
        double translationY = params.getTranslationY();
        
        return applyTransformation(rotInRadians, scale, translationX,
            translationY, centroidX, centroidY, edge);
     }
    
     /**
      * transform the given edges using the given parameters. 
      * 
      * @param rotInRadians rotation in radians
      * @param scale
      * @param translationX translation along x axis in pixels
      * @param translationY translation along y axis in pixels
      * @param centroidX the horizontal center of the reference frame for edges.  
      * this should be the center of the image if edges points are in the
      * original image reference frame.
      * @param centroidY the vertical center of the reference frame for edges.  
      * this should be the center of the image if edges points are in the
      * original image reference frame.
      * @param edges
      * @return 
      */
    public PairIntArray[] applyTransformation(double rotInRadians,
        double scale, double translationX, double translationY,
        double centroidX, double centroidY, PairIntArray[] edges) {
        
        PairIntArray[] transformedEdges = new PairIntArray[edges.length];

        for (int ii = 0; ii < edges.length; ii++) {

            PairIntArray edge = edges[ii];

            PairIntArray te = applyTransformation(rotInRadians,
                scale, translationX, translationY,
                centroidX, centroidY, edge);

            transformedEdges[ii] = te;
        }
        
        return transformedEdges;
    }
    
     /**
      * transform the given edge using the given parameters. 
      * 
      * @param rotInRadians rotation in radians
      * @param scale
      * @param translationX translation along x axis in pixels
      * @param translationY translation along y axis in pixels
      * @param centroidX the horizontal center of the reference frame for edges.  
      * this should be the center of the image if edges points are in the
      * original image reference frame.
      * @param centroidY the vertical center of the reference frame for edges.  
      * this should be the center of the image if edges points are in the
      * original image reference frame.
      * @param edge
      * @return 
      */
    public PairIntArray applyTransformation(double rotInRadians,
        double scale, double translationX, double translationY,
        double centroidX, double centroidY, PairIntArray edge) {
        
        double cos = Math.cos(rotInRadians);
        double sin = Math.sin(rotInRadians);
                
        /*
        scale, rotate, then translate.
        */
        
        PairIntArray te = new PairIntArray();

        for (int i = 0; i < edge.getN(); i++) {

            double x = edge.getX(i);
            double y = edge.getY(i);

            double xr = centroidX * scale + (((x - centroidX) *scale * cos) 
                + ((y - centroidY) * scale * sin));

            double yr = centroidY * scale 
                + ((-(x - centroidX) * scale * sin) 
                + ((y - centroidY) * scale * cos));

            double xt = xr + translationX;
            double yt = yr + translationY;

            int xte = (int) Math.round(xt);
            int yte = (int) Math.round(yt);
            
            te.add(xte, yte);
        }
          
        return te;
    }
    
    public GreyscaleImage applyTransformation(final GreyscaleImage input, 
        TransformationParameters params, int outputWidth, int outputHeight) {
        
        double rotInRadians = params.getRotationInRadians();
        double cos = Math.cos(rotInRadians);
        double sin = Math.sin(rotInRadians);
        
        float scale = params.getScale();
        
        float translationX = params.getTranslationX();
        float translationY = params.getTranslationY();
        
        double centroidX = input.getWidth() >> 1;
        double centroidY = input.getHeight() >> 1;
        
        GreyscaleImage output = new GreyscaleImage(outputWidth, 
            outputHeight);
        
        for (int x = 0; x < input.getWidth(); x++) {
            for (int y = 0; y < input.getHeight(); y++) {
                
                int pix = input.getValue(x, y);
                
                if (pix != 0) {
                    
                    double xr = centroidX * scale 
                        + (((x - centroidX) *scale * cos) 
                        + ((y - centroidY) * scale * sin));

                    double yr = centroidY * scale
                        + ((-(x - centroidX) * scale * sin)
                        + ((y - centroidY) * scale * cos));

                    double xt = xr + translationX;
                    double yt = yr + translationY;
                
                    int x2 = (int)Math.round(xt);
                    int y2 = (int)Math.round(yt);
                    
                    if ((x2 > -1) && (x2 < (output.getWidth() - 1) &&
                        (y2 > -1) && (y2 < (output.getHeight() - 1)))) {
                        
                        output.setValue(x2, y2, pix);
                    }
                }
            }
        }
        
        return output;
    }
    
    public Image applyTransformation(Image input, 
        TransformationParameters params, int outputWidth, int outputHeight) {
        
        double rotInRadians = params.getRotationInDegrees() * Math.PI/180.f;
        double cos = Math.cos(rotInRadians);
        double sin = Math.sin(rotInRadians);
        
        float scale = params.getScale();
        
        float translationX = params.getTranslationX();
        float translationY = params.getTranslationY();
        
        double centroidX = input.getWidth() >> 1;
        double centroidY = input.getHeight() >> 1;
        
        Image output = new Image(outputWidth, outputHeight);
        
        for (int x = 0; x < input.getWidth(); x++) {
            for (int y = 0; y < input.getHeight(); y++) {
                
                int r = input.getR(x, y);
                int g = input.getG(x, y);
                int b = input.getB(x, y);
                
                double xr = centroidX * scale 
                    + (((x - centroidX) * scale * cos)
                    + ((y - centroidY) * scale * sin));

                double yr = centroidY * scale
                    + ((-(x - centroidX) * scale * sin)
                    + ((y - centroidY) * scale * cos));

                double xt = xr + translationX;
                double yt = yr + translationY;
                
                int x2 = (int)Math.round(xt);
                int y2 = (int)Math.round(yt);

                if ((x2 > -1) && (x2 < (output.getWidth() - 1) &&
                    (y2 > -1) && (y2 < (output.getHeight() - 1)))) {

                    output.setRGB(x2, y2, r, g, b);
                }
            }
        }
        
        return output;
    }
}
