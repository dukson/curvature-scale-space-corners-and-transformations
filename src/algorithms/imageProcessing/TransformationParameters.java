package algorithms.imageProcessing;
/**
 *
 * @author nichole
 */
public class TransformationParameters {
            
    private float rotationInRadians = 0;
    
    private float translationX = 0;
    
    private float translationY = 0;
    
    private float scale = 1;
    
    private float originX = 0;
    
    private float originY = 0;

    /**
     * @return the rotation in units of degrees
     */
    public float getRotationInDegrees() {
        return (float)(rotationInRadians * 180./Math.PI);
    }

    /**
     * @param rotationInDegrees the rotation to set in units of degrees
     */
    public void setRotationInDegrees(float rotationInDegrees) {
        this.rotationInRadians = (float) (rotationInDegrees * Math.PI/180.);
    }
    
    /**
     * @param theRotationInRadians the rotation to set in units of radians
     */
    public void setRotationInRadians(float theRotationInRadians) {
        this.rotationInRadians = theRotationInRadians;
    }

    /**
     * @return the rotation in units of radians
     */
    public float getRotationInRadians() {
        return rotationInRadians;
    }
    
    /**
     * @return the translationX
     */
    public float getTranslationX() {
        return translationX;
    }

    /**
     * @param translationX the translationX to set
     */
    public void setTranslationX(float translationX) {
        this.translationX = translationX;
    }

    /**
     * @return the translationY
     */
    public float getTranslationY() {
        return translationY;
    }

    /**
     * @param translationY the translationY to set
     */
    public void setTranslationY(float translationY) {
        this.translationY = translationY;
    }

    /**
     * @return the scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public void setOriginX(float theXOrigin) {
        originX = theXOrigin;
    }
    
    public void setOriginY(float theYOrigin) {
        originY = theYOrigin;
    }
   
    public float getOriginX() {
        return originX;
    }
    
    public float getOriginY() {
        return originY;
    }
    
    public TransformationParameters copy() {
        
        TransformationParameters cp = new TransformationParameters();
        cp.setRotationInRadians(rotationInRadians);
        cp.setTranslationX(translationX);
        cp.setTranslationY(translationY);
        cp.setScale(scale);
        cp.setOriginX(originX);
        cp.setOriginY(originY);
        
        return cp;
    }
    
    @Override
    public boolean equals(Object obj) {
        
        if (!(obj instanceof TransformationParameters)) {
            return false;
        }
        
        TransformationParameters other = (TransformationParameters)obj;
        
        return ((other.rotationInRadians == rotationInRadians) &&
            (other.translationX == translationX) && 
            (other.translationY == translationY) &&
            (other.scale == scale) &&
            (other.originX == originX) && (other.originY == originY)
            );
    }

    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("rotationInRadians=").append(rotationInRadians)
            .append(" rotationInDegrees=").append(rotationInRadians*180./Math.PI)
            .append(" scale=").append(scale)
            .append("\ntranslationX=").append(translationX)
            .append(" translationY=").append(translationY)
            .append(" originX=").append(originX)
            .append(" originY=").append(originY)
            ;
        
        return sb.toString();
    }

}
