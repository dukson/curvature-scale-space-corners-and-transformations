package algorithms.imageProcessing;

public class HeapNode {
    
	/* fields needed for node in circular, doubly linked list */
	private long key = DoubleLinkedCircularList.noValue;
    private HeapNode right = null;
    private HeapNode left = null;

    /* fields needed for a Fibonacci Heap Node */
    private HeapNode parent;
    private DoubleLinkedCircularList children;
    
    /**
     * a.k.a. degree
     */
    private int numberOfChildren = 0;
    private boolean mark = false;

    /* field to carry data  */
    private Object data = null;

    public HeapNode() {
    }
    
    public HeapNode(long key) {
        this.key = key;
    }

    /**
     * add node to list of children.  numberOfChildren is incremented.
     *
     * note that this method is not for use with multiple threads.  
     * it's fast and meant to be
     * accessed synchronously only (children are not protected by mutex).
     *
     * @param child
     */
    public void addChild(HeapNode child) {
    	if (children == null) {
    		children = new DoubleLinkedCircularList();
    	}
    	children.insert(child);
    	child.parent = this;
    	numberOfChildren++;
    }
    
    public void removeChildren() {
        this.children = null;
    }

    /** note that this method is not for use with multiple threads.  
     * it's fast and meant to be
     * accessed synchronously only (children are not protected by mutex)
     *
     * @param child
     */
    public void removeChild(HeapNode child) {
    	if (children == null) {
    		return;
    	}
    	child.parent = null;
    	children.remove(child);
    	numberOfChildren--;
    }

    /** note that this method is not for use with multiple threads.  
     * it's fast and meant to be
     * accessed synchronously only (children are not protected by mutex)
     *
     * @param childKey
     */
    public void removeChild(int childKey) {
    	if (children == null) {
    		return;
    	}
    	children.remove(childKey);
    	numberOfChildren--;
    }

    /** note that this method is not for use with multiple threads.  
     * it's fast and meant to be
     * accessed synchronously only (children are not protected by mutex 
     * and are not copied out)
     * 
     * @return
     */
    public DoubleLinkedCircularList getChildren() {
    	if (children == null) {
    		children = new DoubleLinkedCircularList();
    	}
    	return children;
    }

    /**
     * @return the key
     */
    public long getKey() {
        return key;
    }

    /**
     * @return the right
     */
    public HeapNode getRight() {
        return right;
    }

    /**
     * @return the left
     */
    public HeapNode getLeft() {
        return left;
    }

    /**
     * @return the parent
     */
    public HeapNode getParent() {
        return parent;
    }

    /**
     * @return the numberOfChildren
     */
    public int getNumberOfChildren() {
        return numberOfChildren;
    }

    /**
     * @return the mark
     */
    public boolean isMark() {
        return mark;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return data;
    }

    /**
     * @param key the key to set
     */
    public void setKey(long key) {
        this.key = key;
    }

    /**
     * @param right the right to set
     */
    public void setRight(HeapNode right) {
        this.right = right;
    }

    /**
     * @param left the left to set
     */
    public void setLeft(HeapNode left) {
        this.left = left;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(HeapNode parent) {
        this.parent = parent;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(DoubleLinkedCircularList children) {
        this.children = children;
    }

    /**
     * @param numberOfChildren the numberOfChildren to set
     */
    public void setNumberOfChildren(int numberOfChildren) {
        this.numberOfChildren = numberOfChildren;
    }

    /**
     * @param data the data to set
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * @param mark the mark to set
     */
    public void setMark(boolean mark) {
        this.mark = mark;
    }
}
