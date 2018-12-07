import java.util.*;
/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

			int[] counts = readForCounts(in);
			HuffNode root = makeTree(counts);
			Map<Integer, String> codings = makeCodingsFromTree(root);
			writer(root, out);
			in.reset();
			writeCompressedBits(in, codings, out);
		}
	
	
	
	private int[] readForCounts(BitInputStream in) {
		
		int[] answer = new int[256];
		Map<Integer,Integer> meMap = new HashMap<>();
		
		while(true) {
			int current = in.readBits(BITS_PER_WORD);
			if(current==-1)
				break;
			if(!meMap.containsKey(current))
				meMap.put(current, 1);
			else
				meMap.put(current, meMap.get(current)+1);
		}

		for(int key:meMap.keySet()) {
			answer[key]=meMap.get(key);
		}
		
		return answer;
	}
	

	
 private HuffNode makeTree(int[] counts) {
	PriorityQueue<HuffNode> pq = new PriorityQueue<>();
	
	for(int k=0; k<256; k++) {
		if(counts[k]!=0)
			pq.add(new HuffNode(k, counts[k]));
	}
	
	pq.add(new HuffNode(PSEUDO_EOF,1));

	while(pq.size()>1) {
		HuffNode left = pq.remove();
		HuffNode right = pq.remove();
		HuffNode t = new HuffNode(-1, (left.myWeight + right.myWeight),
									left, right);
		pq.add(t);
	}
	HuffNode root = pq.remove();
	
	return root;
}
 
 
 
 
 private Map<Integer, String> makeCodingsFromTree(HuffNode root) {	
	 	Map<Integer, String> cod = new TreeMap<>();		
		makeCodingsFromTree(root, "", cod);
		return cod;
	}

	private void makeCodingsFromTree(HuffNode root, String path, Map<Integer, String> myMap) {
		if(root==null)
	 		return;
		 if(root.myLeft == null && root.myRight==null) {
			 myMap.put(root.myValue, path);
			 return;
		 }
		 
		 makeCodingsFromTree(root.myLeft, path+"0", myMap);
		 makeCodingsFromTree(root.myRight, path+"1", myMap);
	}
	
	
	
	
	
	private void writer(HuffNode root, BitOutputStream out) {
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, out);
		
	}
	private void writeTree(HuffNode root, BitOutputStream out) {
		if(root==null)
			return;

		if(root.myLeft == null && root.myRight==null) {
			out.writeBits(1, 1);

			out.writeBits(BITS_PER_WORD+1, root.myValue);
		}
		else {
			out.writeBits(1, 0);
			writeTree(root.myLeft, out);
			writeTree(root.myRight, out);
		}
	}
	
	
	
	
	private void writeCompressedBits(BitInputStream in, Map<Integer, String> codings, BitOutputStream out) {
		while(true) {
			int cur =  in.readBits(BITS_PER_WORD);
			if(cur==-1)
				break;
			String encode = codings.get(cur);
			out.writeBits(encode.length(), Integer.parseInt(encode,2));
		}
			String eof = codings.get(PSEUDO_EOF);
			out.writeBits(eof.length(), Integer.parseInt(eof,2));
		}
	
	
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int bits = in.readBits(BITS_PER_INT);
		if(bits != HUFF_TREE)
			throw new HuffException("Illegal header starts with "+bits);
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}
	

	private HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
		if(bit==0) {
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in);
			return new HuffNode(0,0,left,right);
		}
		else {
			return new HuffNode(in.readBits(BITS_PER_WORD+1), 0, null, null);
		}
		
	}
	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {

		HuffNode current = root;
		
		while(true) {

			int bits = in.readBits(1);
			if(bits == -1) {
				throw new HuffException("Bad input, no PSEUDO_EOF");
			}
			else {
				if (bits == 0) 
					current = current.myLeft;
				else
					current = current.myRight;
				if(current.myLeft == null && current.myRight == null) { 
					if(current.myValue==PSEUDO_EOF)
						break;
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}

				}	
			}	
		}
	}
	
	
	
	
}











