//Jackson Mishuk

import java.io.*;
import java.util.*;

public class DBTable {
	private RandomAccessFile rows; //the file that stores the rows in the table
	private long free; //head of the free list space for rows
	private int numOtherFields;
	private int otherFieldLengths[];
	
	private BTree Tree;//holds reference to the BTree
	
	//add other instance variables as needed
	private class Row {
		private int keyField;
		private char otherFields[][];
		/*
 	Each row consists of unique key and one or more character array fields.
 	Each character array field is a fixed length field (for example 10
 	characters).
 	Each field can have a different length.
 	Fields are padded with null characters so a field with a length of
 	of x characters always uses space for x characters.
		 */
		//Constructors and other Row methods
		
		//constructor for row
		private Row(int k, char[][] oF) {
			this.keyField = k;
			this.otherFields = oF;
		}

		//creates row from file
		private Row(long addr) {
			
			try {
				rows.seek(addr);
				keyField = rows.readInt();
			
				otherFields = new char[numOtherFields][];
				for(int i = 0; i<otherFields.length; i++) {
					otherFields[i] = new char[otherFieldLengths[i]];
					for(int j = 0; j<otherFieldLengths[i]; j++) {
						otherFields[i][j] = rows.readChar();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//writes row to file
		private void writeRow(long addr) {
			try {
				rows.seek(addr);
				rows.writeInt(keyField);
			
				int i;
				int j;
				
				for(i = 0; i<otherFields.length; i++) {
					if(otherFields[i] != null) {
						j = 0;
						for(; j<otherFields[i].length; j++) {
							rows.writeChar(otherFields[i][j]);
						}
						for(; j<otherFieldLengths[i]; j++) {
							rows.writeChar('\0');
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public DBTable(String filename, int fL[], int bsize ) {
		/*
 	Use this constructor to create a new DBTable.
 	filename is the name of the file used to store the table
 	fL is the lengths of the otherFields
 	fL.length indicates how many other fields are part of the row
 	bsize is the block size. It is used to calculate the order of the B+Tree
 	A B+Tree must be created for the key field in the table

 	If a file with name filename exists, the file should be deleted before the
 	new file is created.
		 */
		try {
			File path = new File(filename);
		
			if(path.exists())
				path.delete();
			
			rows = new RandomAccessFile(path, "rw");
			
			free = 0;
		
			rows.writeInt(fL.length);
			otherFieldLengths = fL;
			numOtherFields = fL.length;
			for(int i = 0; i<numOtherFields; i++)
				rows.writeInt(fL[i]);
		
			rows.writeLong(free);	
		
			Tree = new BTree(filename, bsize);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DBTable(String filename) {
		//Use this constructor to open an existing DBTable
		try {
			File path = new File(filename);
		
			rows = new RandomAccessFile(path, "rw");
		
			rows.seek(0);
		
			otherFieldLengths = new int[rows.readInt()];
			numOtherFields = otherFieldLengths.length;
		
			for(int i = 0; i < numOtherFields; i++) {
				otherFieldLengths[i] = rows.readInt();
			}
		
			Tree = new BTree(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/*This method helps to find the next spot in memory 
	 * at which a new row would be added.
	 * It is used in conjunction with removeFree() (unless 
	 * the row is already in the table) and is called from insert*/
	private long getFree(){
		
		try {
			if(free == 0) {
				return rows.length();
			}
			
			return free;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/*This method removes the first spot in the free list.
	 * It is used in conjunction with getFree() and is called from insert*/
	private void removeFree() {
		try {
			rows.seek(free);
			long temp2 = rows.readLong();
			rows.seek((numOtherFields + 1) * 4);
		
			rows.writeLong(temp2);
		
			free = temp2;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*This method adds a new spot into the free list.
	 * It is used in the remove method*/
	private void addFree(long l) {
		
		try {
		
			long temp = free;
			
			free = l;
			rows.seek((numOtherFields + 1) * 4);
			rows.writeLong(l);
			
			rows.seek(l);
			rows.writeLong(temp);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public boolean insert(int key, char fields[][]) throws IOException {
		//PRE: the length of each row is fields matches the expected length
		/*
 	If a row with the key is not in the table, the row is added and the method
 	returns true otherwise the row is not added and the method returns false.
 	The method must use the B+tree to determine if a row with the key exists.
 	If the row is added the key is also added into the B+tree.
		 */
		long insertAddr = getFree();
		
		boolean bool = Tree.insert(key, insertAddr);
		
		if(!bool) 
			return bool;
		if(insertAddr != rows.length())
			removeFree();
		
		Row x = new Row(key, fields);
		
		x.keyField = key;
		x.otherFields = fields;

		x.writeRow(insertAddr);
		
		
		return bool;
	}

	public boolean remove(int key) {
		/*
 	If a row with the key is in the table it is removed and true is returned
 	otherwise false is returned.
 	The method must use the B+Tree to determine if a row with the key exists.

 	If the row is deleted the key must be deleted from the B+Tree
		 */
		long remNode = Tree.remove(key);
		
		if(remNode == 0) 
			return false;
		
		addFree(remNode);
		
		return true;
	}

	public LinkedList<String> search(int key) {
		/*
 	If a row with the key is found in the table return a list of the other fields in
 	the row.
 	The string values in the list should not include the null characters.
 	If a row with the key is not found return an empty list
 	The method must use the equality search in B+Tree
		 */

		/*equality search*/long l = Tree.search(key);

		LinkedList<String> Ret = new LinkedList<>();
		if(l != 0) {
			Row x = new Row(l);
				
			for(int i = 0; i < x.otherFields.length; i++) {
				String stg = "";
				for(int j = 0; j < x.otherFields[i].length; j++) {
					if(x.otherFields[i][j] == '\0')
						break;
					stg += String.valueOf(x.otherFields[i][j]);
				}
				Ret.add(stg);
			}
		}
		return Ret;
	}

	
	//2D linked List. Key and other string Fields.
	public LinkedList<LinkedList<String>> rangeSearch(int low, int high) {
		//PRE: low <= high
		/*
 	For each row with a key that is in the range low to high inclusive a list
 	of the fields (including the key) in the row is added to the list
	returned by the call.
 	If there are no rows with a key in the range return an empty list
 	The method must use the range search in B+Tree
		 */

		LinkedList<Long> TreeList = Tree.rangeSearch(low, high);
		
		LinkedList<LinkedList<String>> LL1 = new LinkedList<>(); 
		
		for(int i = 0; i < TreeList.size(); i++) {
			Row x = new Row(TreeList.get(i));
			LinkedList<String> LLS = new LinkedList<>();
			
			LLS.add(String.valueOf(x.keyField));
			for(int j = 0; j <numOtherFields; j++) {
				String stg = "";
				for(int h = 0; h <numOtherFields; h++) {
					stg+= x.otherFields[j][h];
				}
				LLS.add(stg);
			}
			LL1.add(LLS);
		}
		return LL1;	
	}
	
	//do not use print in B+ tree. use rangeSearch
	public void print() {
		//Print the rows to standard output is ascending order (based on the keys)
		//Include the key and other fields
		//print one row per line
		LinkedList<Long> LL = Tree.rangeSearch(Integer.MIN_VALUE,Integer.MAX_VALUE);
		
		while(!LL.isEmpty()) {
			long lng = LL.pop();
			Row r = new Row(lng);
			
			System.out.printf("Key:  %d,\t Address: %d\t OtherFields: ", r.keyField, lng);
			
			int j;
			int i;
			
			for(i = 0; i<numOtherFields; i++) {
				
				String stg = "";
				
				j = 0;
				for(; j<otherFieldLengths[i]; j++) {
					if(r.otherFields[i][j] != '\0') {
						stg += r.otherFields[i][j];
					}
				}
				System.out.printf("%s, ", stg);
			}
			System.out.println();
			
		}
		
	}
	public void close() {
		//close the DBTable. The table should not be used after it is closed
		try {
			Tree.close();
			rows.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} 
	
	//Remove later
	public void treePrint() {
		Tree.print();
	}
}