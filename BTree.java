//Jackson Mishuk

import java.io.*;
import java.util.*;

public class BTree {
	private RandomAccessFile f;
	private int order;
	private int blockSize;
	private long root;
	private long free;
	
	private Stack<BTreeNode> path;//Stores the path when using search
	private int minKeys;
	
	private class BTreeNode {
		private int count;
		private int keys[];
		private long children[];
		private long address;//the addressN of the node in the file
		private long addressN; //(This meant to say the addressN of the next leaf Node)
		
		//constructors and other method
		
		//constructor for row
		private BTreeNode(long addr, long a) {
			
			this.address = addr;
			
			this.keys = new int[order];
			this.children = new long[keys.length + 1];
			
			this.count = 0;
			
			//If Node is a non-leaf, then a = -1;
			this.addressN = a;
				
		}
		
		//creates row from file
		private BTreeNode(long addr) {
			try {
				f.seek(addr);
				
				address = addr;
				
				count = f.readInt();
			
				keys = new int[order];
				
				int h = 0;
				
				for(; h<Math.abs(count); h++) {
					keys[h] = f.readInt();
				}
				for(; h < keys.length-1; h++) {
					keys[h] = f.readInt();
				}
			
				
				int temp; int temp1;
/*Leaf*/			if(count < 0){temp = (order)-1; temp1 = Math.abs(count);}
/*non-Leaf*/		else {temp = order; temp1 = count + 1;}
				
				h = 0;
				
				children = new long[keys.length + 1];
				for(; h<temp1; h++) {
					children[h] = f.readLong();
				}
				for(; h<temp; h++) {
					f.readLong();
				}
				if(count < 0) {
					addressN = f.readLong();
				}
				else addressN = -1L;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//writes row to file
		private void writeBTreeNode(long addr) {
			
			try {
				f.seek(addr);
			
				f.writeInt(count);
			
				int h = 0;
			
				for(; h<Math.abs(count); h++) {
					f.writeInt(keys[h]);
				}
				for(;h < order-1; h++) {
					f.writeInt(-1);
				} 
				
				int i = 0;
				
				int tempCnt = count;
				if(tempCnt < 0)
					tempCnt *= -1;
				else
					tempCnt++;
				
				for(; i<tempCnt; i++) {
					f.writeLong(children[i]);
				}
				
				int temp;
				
				if(count < 0 || (count == 0 && addressN!=-1))
					temp = order-1;
				else
					temp = order;
				
				for(;i < temp ; i++) {
					f.writeLong(-1);
				}
			
				if(addressN != -1)
					f.writeLong(addressN);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//adds k and addr to the keys and children arrays respectively at spot i.
		//also changes count value.
		public boolean addToArr(int key, long addr, long l) {

			int i = 0;
			
			for(; i < Math.abs(count); i++) {
				if(keys[i] > key) 
					break;
				else if(keys[i] == key)
					return false;
			}
			
			//if leaf
			if(count < 0 || (count == 0 && addressN != -1)) {
				for(int j = -count-1; j>=i; j--) {
					keys[j+1] = keys[j];
					children[j+1] = children[j];
				}
				keys[i] = key;
				children[i] = addr;
				count--;
			
			//if non-leaf
			}else {
				int j = count-1;
				for(; j>=i; j--) {
					keys[j+1] = keys[j];
					children[j+2] = children[j+1];
				}
				keys[i] = key;
				children[i+1] = addr;
				count++;
			}
			if(Math.abs(count) < order)
				writeBTreeNode(l);
			
			return true;
		}

		//inputs a value and removes it if in the tree. r is true if you want to remove a non-leaf's right
		public boolean remFromArr(int key, long l, boolean r) {
			
			int i = 0;//holds the spot of the value we want removed
			int j;
			
			for(; i < Math.abs(count); i++) {
				if(keys[i] == key) 
					break;	
				else if(keys[i] > key)
					return false;
			}
			if(count == 0)
				return false;
			//if leaf
			if(count < 0 || (count == 0 && addressN != -1)) {
				for(j = i; j<-count-1; j++) {
					keys[j] = keys[j+1];
					children[j] = children[j+1];
				}
				count++;
			//if non-leaf
			}else {
				if(r) {
					for(j = i; j<count-1; j++) {
						keys[j] = keys[j+1];
						children[j+1] = children[j+2];
					}	
						count--;
				}else {
					for(j = 0; j<count-1; j++) {
						keys[j] = keys[j+1];
						children[j] = children[j+1];
					}	
					children[j] = children[j+1];
					count--;
				}
			}
			if(l != 0) 
				writeBTreeNode(l);
			return true;
			
			}
		}
	public BTree(String filename, int bsize) {
		//bsize is the block size. This value is used to calculate the order
		//of the B+Tree
		//all B+Tree nodes will use bsize bytes
		//makes a new B+tree
		try {
			File path = new File(filename + "Tree");
		
			if(path.exists())
				path.delete();
			
			f = new RandomAccessFile(path, "rw");
			
			root = 0;
			free = 0;
		
			f.writeLong(root);
			f.writeLong(free);
		
			f.writeInt(bsize);
			order = bsize/12;
			
			minKeys = (order+1)/2-1;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public BTree(String filename) {
		//open an existing B+Tree
		try {
			File path = new File(filename + "Tree");
			f = new RandomAccessFile(path, "rw");
		
			f.seek(0);
			root = f.readLong();
			free = f.readLong();
		
			blockSize = f.readInt();
			order = blockSize/12;
			
			minKeys = (order+1)/2-1;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*This method helps to find the next spot in memory 
	 * at which a new row would be added. Is called from insert*/
	private long getFree(){
		
		try {
			if(free == 0) {
				return f.length();
			}
			
			long temp = free;
			
			f.seek(free);
			
			long temp2 = f.readLong();
		
			free = temp2;
			
			f.seek(8);
			
			f.writeLong(free);
			
			return temp;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public boolean insert(int key, long addr) {
		/*
 	If key is not a duplicate add key to the B+tree
 	addr (in DBTable) is the addressN of the row that contains the key
 	return true if the key is added
 	return false if the key is a duplicate
		 */
		BTreeNode x;
		BTreeNode newnode;
		boolean split;
		long loc = 0;
		int val = 0;

		try {
		
			if(root == 0) {
				
				x = new BTreeNode(1, 0);/*1, 0 Because it is a leaf and it is the only leaf*/
				
				long fr = getFree();
				x.addToArr(key, addr, fr);
				
				root = fr;
				f.seek(0);
				f.writeLong(fr);
				
				return true;
			}

			
			if(search(key) != 0)
				return false;
			
			x = path.pop();
			
			if(Math.abs(x.count) + 1 < order) {
				//If no split will be needed on the leaf
				if(x.addToArr(key, addr, x.address)) {
					x = new BTreeNode(x.address);
					split = false;
				}else
					return false;
			}else {
				//If a split will be needed on the leaf
				newnode = new BTreeNode(1 , 0);/*1 Because it is a leaf*/

				//split
				x.addToArr(key, addr, x.address);

				int i = Math.abs(x.count)/2;
				int j = i;
				
				for(; i < order; i++) {
					newnode.keys[i-j] = x.keys[i];
					newnode.children[i-j] = x.children[i];
				}
				
				x.count = -j;
				newnode.count = -(i-j);
				val = newnode.keys[0];
				
				loc = getFree();
				newnode.addressN = x.addressN;
				x.addressN = loc;
				
				x.writeBTreeNode(x.address);
				newnode.writeBTreeNode(loc);
				
				split = true;
			}
			
			while(!path.empty() && split) {
				
				x = path.pop();
				
				if(x.count + 1 < order) {
					//If no split will be needed on the leaf
					x.addToArr(val, loc, x.address);
					split = false;
				}else {		
					//If a split will be needed on the non-leaf
					newnode = new BTreeNode(-1, -1);/*-1 because it is a non-leaf*/
					
					x.addToArr(val, loc, x.address);
					
					int newVal = x.keys[(order-1)/2];
					newnode.children[0] = x.children[(order+1)/2];
					
					x.count++;
						
					int temp = x.count;
					int i = x.count/2;
					
					for(; i<x.count-1; i++) {
						newnode.keys[i - (x.count/2-1)-1] = x.keys[i]; 
						newnode.children[i - x.count/2+1] = x.children[i+1];
						newnode.count++;
						temp--;
					}
					
					x.count = temp-2;
					loc = getFree();
					
					x.writeBTreeNode(x.address);
					newnode.writeBTreeNode(loc);
					
					val = newVal;
				}
			}
			
			if(split) {
				
				newnode = new BTreeNode(-1, -1);/*-1 because it is a non-leaf*/
				newnode.children[0] = root;
				
				root = getFree();
				newnode.addToArr(val, loc, root);
				
				f.seek(0);
				f.writeLong(root);
			}
			
		}catch(IOException e){
			e.printStackTrace();
		}
		return true;
	}
	
	//returns a boolean based on if it borrowed
	private boolean borrow(BTreeNode child, BTreeNode x) {
		
		boolean tempBool = false, right = false;
		long tempLong;
		BTreeNode n, tempBorrow = null;
		
		//Must check to see if a borrow is possible
		tempLong = rightNeighbor(child, x);
		if(tempLong != 0) {
			tempBorrow = new BTreeNode(tempLong);
			if(Math.abs(tempBorrow.count) - 1 >= minKeys) {
				tempBool = true;
				right = true;
			}
			
		}
		if(!tempBool) {
			tempLong = leftNeighbor(child, x);
			if(tempLong != 0) {
				tempBorrow = new BTreeNode(tempLong);
				if(Math.abs(tempBorrow.count) - 1 >= minKeys)
					tempBool = true;
			}
		}
		if(!tempBool)
			return false;
		else {
			n = new BTreeNode(tempLong);
			//If it can borrow
			if(!right) {
				//if it wants to steal from the left
				if(child.count < 0 || (child.count == 0 && child.addressN != -1)) {
					//if child is a leaf
					for(int i = 0; i<x.count; i++) {
						if(x.children[i] == n.address) {
							x.keys[i] = n.keys[-n.count - 1];
							x.children[i+1] = child.address;
							i=x.count;
						}
					}
					x.writeBTreeNode(x.address);
					child.addToArr(n.keys[-n.count - 1], n.children[-n.count - 1], child.address);
					n.remFromArr(n.keys[-n.count - 1], n.address, true);
				}else {
					//if child is a non-leaf
					for(int i = 0; i < x.count; i++) {
						if(x.children[i] == n.address) {
							for(int j = child.count; j>=0; j--) {
								child.children[j+1] = child.children[j];
								child.keys[j+1] = child.keys[j];
							}
							child.children[0] = n.children[n.count];
							child.keys[0] = x.keys[i];
							child.count++;
							int tempKey = x.keys[i];
							
							x.addToArr(n.keys[n.count-1], child.address, x.address);
							x.remFromArr(tempKey, x.address, true);
							child.writeBTreeNode(child.address);
						}

					}
					n.remFromArr(n.keys[n.count-1], n.address, true);
				}
			}else {
				//if it wants to steal from the right
				if(child.count < 0 || (child.count == 0 && child.addressN != -1)) {
				//if child is a leaf
					for(int i = 0; i<x.count; i++) {
						if(x.children[i] == child.address) {
							x.remFromArr(x.keys[i], x.address, true);
							i=x.count;
						}
					}
					x.addToArr(n.keys[1], n.address, x.address);
					child.addToArr(n.keys[0], n.children[0], child.address);
					n.remFromArr(n.keys[0], n.address, true);
				}else {
					//if child is a non-leaf
					for(int i = 0; i < x.count; i++) {
						if(x.children[i] == child.address) {
							child.keys[child.count] = x.keys[i];
							child.children[child.count+1] = n.children[0];
							child.count++;
							child.writeBTreeNode(child.address);
							
							x.keys[i] = n.keys[0];//
							
							
							i = x.count;
						}
					}
					x.writeBTreeNode(x.address);
					n.remFromArr(n.keys[0], n.address, false);
				}
			}
		}
		return true;
	}
	
	//returns the address of the right neighbor 
	//If there is no rightNeighbor returns 0
	private long rightNeighbor(BTreeNode child, BTreeNode x) {

		if(child.address == x.children[Math.abs(x.count)]) 
			return 0;
		else {
			int j;
			for(j = 0; j<Math.abs(x.count); j++) {
				if(child.address == x.children[j])//if child is in x's children's j spot
					break;
			}
			x = new BTreeNode(x.children[j+1]);
				
			return x.address;
		}
	}
	
	private long leftNeighbor(BTreeNode child, BTreeNode x) {
		
		
		if(child.address == x.children[0]) 
			return 0;
		else {
			int j;
			for(j = 0; j<Math.abs(x.count); j++) {
				if(child.address == x.children[j])
					break;
			}
			x = new BTreeNode(x.children[j-1]);

			return x.address;
		}
	}
	
	//will always put values into left
	private BTreeNode combine(BTreeNode child, BTreeNode x) {
		
		long rightN;
		BTreeNode n, tempNode;//tempNode is only used to switch nodes if child is on the right of n.
		
		rightN = rightNeighbor(child, x);
		
		n = rightN == 0 ? new BTreeNode(leftNeighbor(child, x)) : new BTreeNode(rightN);
		
		if(rightN == 0) {
			tempNode = n;
			n = child;
			child = tempNode;
		}
		
		child.addressN = n.addressN;
		if(child.count < 0) {
			//leaf
			for(int i = 0; i < x.count; i++) {
				if(child.address == x.children[i]) {
					x.remFromArr(x.keys[i], x.address, true);
					for(int j = 0; j < -n.count; j++) {
						child.addToArr(n.keys[j], n.children[j], child.address);
					}
					i=x.count;
				}
			}
		}else {
			//non-leaf
			for(int i = 0; i<x.count; i++) {
				if(child.address == x.children[i]) {
					child.addToArr(x.keys[i], n.children[0], child.address);
					for(int j = 0; j < n.count; j++) {
						child.addToArr(n.keys[j], n.children[j+1], child.address);
					}
					x.remFromArr(x.keys[i], x.address, true);
					
					i=x.count;
				}
			}
		}
		x.writeBTreeNode(x.address);
		addFree(n.address);
		
		return child;
	}

	/*This method adds a new spot into the free list.
	 * It is used in the remove method
	 */
	private void addFree(long l) {
		
		try {
		
			long temp = free;
			
			free = l;
			f.seek(8);
			f.writeLong(l);
			
			f.seek(l);
			f.writeLong(temp);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public long remove(int key) {
		/*
 	If the key is in the Btree, remove the key and return the address of the row
 	return 0 if the key is not found in the B+tree
		 */
		
		BTreeNode x;
		
		long r = search(key);
		if(path.size() - 1 == 0) {
			x = new BTreeNode(root);
			x.remFromArr(key, x.address, true);
			if(x.count==0){//root is empty
				root = 0;
				addFree(x.address);
			}
			return r;
		}
		if(r == 0) 
			return 0;
		
		BTreeNode child;
		boolean borrow;
		boolean tooSmall = false;
		x = path.pop();

		x.remFromArr(key, x.address, true);
		if(Math.abs(x.count) < minKeys)
			tooSmall = true;
		while(!path.empty() && tooSmall) {
			
			child = x;//node that is too small

			x = path.pop();//parent
			borrow = borrow(child, x);//returns false if cannot borrow
			
			if(borrow)
				tooSmall = false;
			else{
				child = combine(child, x);
				x = new BTreeNode(x.address);
				
				if(Math.abs(x.count) >= minKeys) 
					tooSmall = false;
				else
					tooSmall = true;
			}
		}
		if(x.count==0){//root is empty
			root = x.children[0];
			addFree(x.address);
		}
		
		return r;
	}

	public long search(int k) {
		/*
 	This is an equality search
 	If the key is found return the addressN of the row with the key
 	otherwise return 0
		 */
		path = new Stack<>();
		return search(k, root);
	}
	
	private long search(int k, long r) {
		
		
		
		BTreeNode x = new BTreeNode(r);
		
		path.add(x);
		
		if(x.count > 0) {
			int i;
			for(i = 0; i<x.count; i++) {
				if(x.keys[i] > k) {
					//recurse to the corresponding child
					break;
				}
			}
			r = search(k, x.children[i]);
		}else {
			for(int i = 0; i<Math.abs(x.count); i++) {
				if(x.keys[i] > k) {
					//recurse to the corresponding child
					return 0;
				}else if(x.keys[i] == k) {
					return x.children[i];
				}
			}
			return 0;
		}
		return r;
	}
	
	public LinkedList<Long> rangeSearch(int low, int high){
	//PRE: low <= high

		/*
 	return a list of row addresses for all keys in the range low to high inclusive
 	the implementation must use the fact that the leaves are linked
 	return an empty list when no keys are in the range
	 */
		LinkedList<Long> LL = new LinkedList<>();
		if(root != 0) {
			search(low);
		
			BTreeNode x = path.pop();
		
			boolean bool = false;
		
			while(!bool) {
				for(int i = 0; i < -x.count && !bool; i++) {
					if(x.keys[i] >= low && x.keys[i] <= high){
						LL.add(x.children[i]);
					}else if(x.keys[i] > high) {
						bool = true;
						continue;
					}	
				}
				if(x.addressN != 0 && x.addressN != -1)
					x = new BTreeNode(x.addressN);
				else
					bool = true;
			}
		}
		return LL;
	}
	
	public void print() {
		//print the B+Tree to standard output
		//print one node per line
		//This method can be helpful for debugging

		print(root);
	}
	
	private void print(long r) {
		
		if(r != 0) {
		
			BTreeNode x = new BTreeNode(r);
			int i;
			
			System.out.printf("Address: %d\t Count %d\t ", x.address, x.count);
			
			
			if(x.count < 0) {
				System.out.print("Keys: ");
				for(i = 0; i < -x.count; i++) {
					System.out.printf("[%d]:%d\t", i, x.keys[i]);
				}
				for(; i<order; i++) {
					System.out.printf("\t");
				}
				System.out.print("Children: ");
				for(i = 0; i < -x.count; i++) {
					System.out.printf("[%d]:%d\t", i, x.children[i]);
				}
				for(; i<order; i++) {
					System.out.printf("\t");
				}
				System.out.printf("Next Address: %d", x.addressN);

				
				System.out.println();
				
			}else {
				System.out.print("Keys: ");
				for(i = 0; i < x.count; i++) {
					System.out.printf("[%d]: %d\t", i, x.keys[i]);
				}
				for(; i<order; i++) {
					System.out.printf("\t");
				}
				System.out.print("Children: ");
				for(i = 0; i < x.count+1; i++) {
					System.out.printf("[%d]:%d\t", i, x.children[i]);
				}
				for(; i<order; i++) {
					System.out.printf("\t");
				}

				System.out.println();
				
				
				for(i = 0; i<x.count+1;i++)
					print(x.children[i]);
			}
		}	
	}
	public void close() throws IOException {
		//close the B+tree. The tree should not be accessed after close is called
		f.seek(0);
		
		f.writeLong(root);
		f.writeLong(free);
		
		
		f.close();
	}
}