package algorithms.imageProcessing;

/**
 *
 * @author nichole
 */
public class GreyscaleImage {
    
    private int[] a;
    
    private int width;
    
    private int height;
    
    private int nPixels;
    
    /**
     * @param theWidth
     * @param theHeight
     */
    public GreyscaleImage (int theWidth, int theHeight) {
        
        nPixels = theWidth * theHeight;
        
        width = theWidth;
        
        height = theHeight;
        
        a = new int[nPixels];
    }
    
    public void setValue(int col, int row, int value) {
        
        if ((col < 0) || (col > (width - 1))) {
            throw new IllegalArgumentException("col is out of bounds: col=" 
                + col + " width=" + width);
        }
        if ((row < 0) || (row > (height - 1))) {
            throw new IllegalArgumentException("row is out of bounds: row=" 
                + row + " height=" + height);
        }
        
        int idx = (row * width) + col;
        
        if ((idx < 0) || (idx > (a.length - 1))) {
            throw new IllegalArgumentException(
                "col and/or are out of bounds");
        }
        
        /*
        int rPix = (value >> 16) & 0xFF;
        int gPix = (value >> 8) & 0xFF;
        int bPix = value & 0xFF;
        */
       
        a[idx] = value;
    }
        
    public int getValue(int col, int row) {
        
        if ((col < 0) || (col > (width - 1))) {
            throw new IllegalArgumentException("col is out of bounds: col=" 
                + col + " width=" + width);
        }
        if ((row < 0) || (row > (height - 1))) {
            throw new IllegalArgumentException("row is out of bounds: row=" 
                + row + " height=" + height);
        }
        
        int idx = (row * width) + col;
       
        if (idx > a.length) {
            throw new IllegalArgumentException("coords are out of bounds");
        }
        /*
        int rgb = (int)(((r[idx] & 0x0ff) << 16) 
            | ((g[idx] & 0x0ff) << 8) | (b[idx] & 0x0ff));
        */
        return a[idx];
    }
    
    /**
     * get pixel using the internal index of 0 through width x height pixels.
     * 
     * @param internalIndex
     * @return 
     */
    public int getValue(int internalIndex) {
        
        if (internalIndex > a.length) {
            throw new IllegalArgumentException("internalIndex is out of bounds");
        }
        
        return a[internalIndex];
    }
    
    public int[] getValues() {
        return a;
    }
 
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public GreyscaleImage copyImage() {
       
        GreyscaleImage img2 = new GreyscaleImage(width, height);
        
        System.arraycopy(a, 0, img2.a, 0, nPixels);
        
        return img2;
    }
    
    public Image copyImageToGreen() {
        
        Image img2 = new Image(width, height);
        
        System.arraycopy(a, 0, img2.g, 0, nPixels);
        
        return img2;
    }
    
    public void resetTo(final GreyscaleImage copyThis) {
        
        if (copyThis.nPixels == nPixels) {
            
            System.arraycopy(copyThis.a, 0, a, 0, nPixels);
            
        } else {
            
            a = copyThis.a;
    
            width = copyThis.width;
    
            height = copyThis.height;
    
            nPixels = copyThis.nPixels;
        }
        
    }

    /**
     * @return the nPixels
     */
    public int getNPixels() {
        return nPixels;
    }
}
