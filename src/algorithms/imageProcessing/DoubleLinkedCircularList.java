package algorithms.imageProcessing;

/**
 * a doubly linked list with a sentinel in between the last and first item.
 * created specifically to hold Fibonacci Heap nodes.
 *
 * <pre>
 * Runtime complexity:
 * insert is O(1)
 * search is O(N)
 * delete if given node is O(1), else O(N)
 *</pre>
 * @author nichole
 */
public class DoubleLinkedCircularList {

    //appears between head and tail
    private final HeapNode sentinel;
    
    public final static int sentinelKey = Integer.MIN_VALUE;
    public final static int noValue = Integer.MIN_VALUE + 1;
    public final static int minValue = Integer.MIN_VALUE + 2;

    protected int number = 0;
    
    public DoubleLinkedCircularList() {
        sentinel = new HeapNode(sentinelKey);
        resetSentinel();
    }

    public HeapNode getSentinel() {
        return sentinel;
    }
    
    protected void resetSentinel() {
        this.sentinel.setLeft(sentinel);
        this.sentinel.setRight(sentinel);
    }

    /**
    * insert new key into circular doubly linked list to the 
    * right<-- change> of the sentinel
    * runtime is O(1)
    *
    * Example:
    *
    *   --------------------------/\
    *   |                          |
    *   -->[sentinel] --> [9] --> [1]
    *   <--           <--     <--
    *
    * @param node
    * @return inserted child node instance
    */
    public HeapNode insert(HeapNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node cannot be null");
        }
        if (node.getKey() == noValue) {
            throw new IllegalArgumentException("node must have key set");
        }
        
        node.setRight(sentinel.getRight());
        sentinel.getRight().setLeft(node);
        sentinel.setRight(node);
        node.setLeft(sentinel);
        number++;
        
        return node;
    }

    public boolean remove(int key) {
        HeapNode cn = search(key);
        remove(cn);
        return (cn != null);
    }
    
    public void remove(HeapNode node) {
    	if (node == null) {
    		return;
    	}
        node.getRight().setLeft(node.getLeft());
        node.getLeft().setRight(node.getRight());
                
        number--;
    }
   
    public HeapNode search(int key) {
        
        HeapNode cn = sentinel.getRight();
        
        while ((cn.getKey() != sentinel.getKey()) && (cn.getKey() != key)) {
            cn = cn.getRight();
        }
        
        return ((cn.getKey() != noValue) &&  (cn.getKey() != sentinel.getKey()))
            ? cn : null;
    }
}