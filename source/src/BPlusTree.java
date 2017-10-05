import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class BPlusTree {
//---------------------------------------------------C O N S T A N T S-------------------------------------------------------------------
	private final int isLeaf=-300;
	private final int isNotLeaf=-200;
	private final int isEmpty=-100;
	private final int NULL=-1;
//---------------------------------------------------------------------------------------------------------------------------------------	
	private int Root,myRoot;
	private int n; //set n as the number of the keys that a LEAF can hold
	private int N;
	private int m;
	private int diskAccesses;
	private byte [] byteBuffer;
	private int [] buffer;
	private int nodesAdded=0;
	private int internalNodes=0;
	private int keysAdded=0;
	private int LeavesPerPage;
	private int currentPointer=0;
	private DataOutputStream myStream;
	private DataInputStream inStream;
	private RandomAccessFile myFile;
	private File file;
	private final String filename="bplus.bin";
	
	public BPlusTree(int N,int m) throws IOException{
		this.N=N;
		this.m=m;
		myRoot=Root=1;
		n=LeavesPerPage=(int)((N-20)/(m+4));
		if(n%2!=0){
			n=n-1;
			LeavesPerPage=LeavesPerPage-1;
		}
		byteBuffer=new byte[N];
		buffer=new int[N/4];
		file=new File(filename);
		if(file.exists()==true){file.delete();}
		myFile = new RandomAccessFile(file,"rw");
		writeBlankLeaf(Root,0);
	}
	public void delete(int key) throws IOException{  //method to delete a key and its data from the tree
		diskAccesses=0;
		int node=getNodeLeafSearch(1,key);
		int parent=0,i=0;
		boolean check=true;
		int [] keys,childNodes;
		Data [] mData;
		int Next,tmp=0;
		int numKeys;
		while(parent!=-1){
			i=0;
			byteBuffer=read(node);
			keys=getKeys(byteBuffer);
			numKeys=getNumKeys(byteBuffer);
			while(keys[i]!=key&&i<numKeys){
    			i++;
    		}
			if(i>=numKeys){
				System.out.println("There is no such key in the b+  tree");
				parent=-1;
			}
			else if(i==0){
				if(isLeaf(byteBuffer)){
					mData=getData(byteBuffer);
					for(int j=i;j<numKeys-1;j++){
						keys[j]=keys[j+1];
						mData[j]=mData[j+1];
					}
					keys[numKeys-1]=-1;
					mData[numKeys-1]=new Data(m,-1);
					tmp=keys[0];
					numKeys=numKeys-1;
					Next=getNextNode(byteBuffer);
					parent=getParent(byteBuffer);
					writeLeaf(node,numKeys,keys,mData,Next,parent);
					node=parent;
					check=false;
				}
				else{
					keys[0]=tmp;
					childNodes=getChildNodes(byteBuffer);
					parent=getParent(byteBuffer);
					writeInternal(node,keys,childNodes,numKeys,parent);
					node=parent;
				}
			}
			else{
				if(isLeaf(byteBuffer)){
					mData=getData(byteBuffer);
					for(int j=i;j<numKeys-1;j++){
						keys[j]=keys[j+1];
						mData[j]=mData[j+1];
					}
					keys[numKeys-1]=-1;
					mData[numKeys-1]=new Data(m,-1);
					numKeys=numKeys-1;
					
					Next=getNextNode(byteBuffer);
					parent=getParent(byteBuffer);
					writeLeaf(node,numKeys,keys,mData,Next,parent);
					parent=-1;
					check=false;
				}
				else{
					keys[i]=tmp;
					childNodes=getChildNodes(byteBuffer);
					parent=getParent(byteBuffer);
					writeInternal(node,keys,childNodes,numKeys,parent);
					parent=-1;
				}
			}
		}
	}
	public byte [] read(int pointer) throws IOException{  //it is used to read the randomAccessFile adding 1 to the diskAccesses each time this 
														 //method is called
		byte [] buffer=new byte[N];
		diskAccesses++;
		myFile.seek(pointer-1);
		myFile.read(buffer);
		return buffer;
	}
	public void write(int pointer,byte [] buffer) throws IOException{//it is used to write to randomAccessFile adding 1 to the diskAccesses each time this 
																	//method is called
		diskAccesses++;
		myFile.seek(pointer-1);
		myFile.write(buffer);
	}
	public void insert(int key,Data mData) throws IOException{ //this method is used to insert a key to the tree starting from the root 
		diskAccesses=0;
		int numKeys;
		int [] childNode=new int [n+1];
		int [] keys =new int [n];	
		byteBuffer=read(Root);
		numKeys=getNumKeys(byteBuffer);
		if (numKeys==n) {
			currentPointer++;
	    	for(int i=0;i<n;i++){
	    		childNode[i+1]=-1;
	    		keys[i]=-1;
	    	}
	    	childNode[0]=currentPointer*N;
	    	keys[0]=getKeys(byteBuffer)[(n/2)-1];
	    	writeInternal(Root,keys,childNode,1,-1);
	    	split(Root,1,currentPointer*N,byteBuffer);
	    	NonFullNode(Root,key,mData);
	    } 
	    else {
	    	NonFullNode(Root,key,mData);
        }
	}
	public void NonFullNode(int node,int key,Data mData) throws IOException{ //insert a key to a Non Full Node 
		
		int i;
		byte [] buffer;
		int numKeys;
		int [] keys;
		int [] childNodes;
		int parent;
		int NextNode;
		Data [] data;
		byteBuffer=read(node);
		if(isLeaf(byteBuffer)){ 
			numKeys=getNumKeys(byteBuffer);
			i=numKeys;
			keys=getKeys(byteBuffer);
			data=getData(byteBuffer);
			parent=getParent(byteBuffer);
			NextNode=getNextNode(byteBuffer);
			keys[i]=key;
			data[i]=mData;
			writeLeaf(node,numKeys+1,keys,data,NextNode,parent);
		}
		else{ 
			numKeys=getNumKeys(byteBuffer);
			keys=getKeys(byteBuffer);
			parent=getParent(byteBuffer);
			childNodes=getChildNodes(byteBuffer);
            byteBuffer=read(childNodes[numKeys]);
            if (getNumKeys(byteBuffer) == n) {
            	keys[numKeys]=getKeys(byteBuffer)[(n/2)-1];
            	byteBuffer=read(node);
                childNodes=getChildNodes(byteBuffer);
                parent=getParent(byteBuffer);
                numKeys=getNumKeys(byteBuffer);
                writeInternal(node,keys,childNodes,numKeys+1,parent);
                byteBuffer=read(childNodes[numKeys]);
                split(node, numKeys+1,childNodes[numKeys],byteBuffer); 
                byteBuffer=read(node);
                childNodes=getChildNodes(byteBuffer);
                NonFullNode(childNodes[numKeys+1], key, mData);
            }else{NonFullNode(childNodes[numKeys], key, mData);}
		}
	}
	
	public void split(int parentNode,int counter,int previousNode,byte [] buffer) throws IOException{ //when a node is full this method 
																									// creates creates 2 new nodes spliting the keys of the full node
		byte [] hbuffer=new byte[N];
		currentPointer++;
		//----------------------------------NewNode Elements-------------------------------
		int newNode=currentPointer*N;
		int newNodeNumKeys=n/2;
		int newNodeNextNode;
		Data [] newNodeData=new Data[n];
		int [] newNodeChildNodes=new int[n+1];
		int [] newNodeKeys=new int[n];
		//----------------------------------PreviousNode Elements--------------------------
	    int [] previousNodeKeys;
	    Data [] previousNodeData;
	    int previousNodeNextNode;
	    int previousNodeNumKeys=getNumKeys(buffer);
	    int [] previousNodeChildNodes;
		//----------------------------------ParentNode Elements----------------------------
	    hbuffer=read(parentNode);
	    int parentNodeParent=getParent(hbuffer);
		int parentNodeNumKeys=getNumKeys(hbuffer);
	    int [] parentNodeKeys=getKeys(hbuffer);
	    int [] parentNodeChildNodes=getChildNodes(hbuffer);
	    //---------------------------------------------------------------------------------
		if(isLeaf(buffer)){
			writeBlankLeaf(newNode, parentNode);
			for(int i=0;i<n;i++){
				newNodeKeys[i]=-1;
				newNodeData[i]=new Data(m,-1);
			}
			previousNodeKeys=getKeys(buffer);
			previousNodeData=getData(buffer);
			
			previousNodeNextNode=getNextNode(buffer);
			for (int j=0;j<(n/2);j++) { 
                newNodeKeys[j] = previousNodeKeys[j+(n/2)-1];
                newNodeData[j] = previousNodeData[j +(n/2)-1];
			}
			newNodeKeys[n/2]=previousNodeKeys[n-1];
			newNodeData[n/2]=previousNodeData[n-1];
			newNodeNextNode = previousNodeNextNode;
			previousNodeNextNode = newNode;
			for (int j=(n/2)-1;j<previousNodeNumKeys;j++) {
                previousNodeKeys[j]=-1;
                previousNodeData[j]=new Data(m,-1);
			}
			previousNodeNumKeys=(n/2)-1;
			
			parentNodeChildNodes[counter]=newNode;
			writeLeaf(previousNode,(n/2)-1,previousNodeKeys,previousNodeData,previousNodeNextNode,parentNode);
			writeLeaf(newNode,(n/2)+1,newNodeKeys,newNodeData,newNodeNextNode,parentNode);
			writeInternal(parentNode,parentNodeKeys,parentNodeChildNodes,parentNodeNumKeys,parentNodeParent);
		}
		else{
			writeBlankInternal(newNode,parentNode);
			for(int i=0;i<n+1;i++){
				if(i==0){
					newNodeChildNodes[i]=-1;
				}
				else{
					newNodeKeys[i-1]=-1;
					newNodeChildNodes[i]=-1;
				}
			}
			previousNodeKeys=getKeys(buffer);
			previousNodeChildNodes=getChildNodes(buffer);
			for (int j=0;j<(n/2);j++) { 
                newNodeKeys[j] = previousNodeKeys[j+(n/2)-1];
			}
			newNodeKeys[n/2]=previousNodeKeys[n-1];
			for (int j = 0; j < (n/2)+1; j++) {
                newNodeChildNodes[j] = previousNodeChildNodes[j+(n/2)-1];
			}
			newNodeChildNodes[(n/2)+1]=previousNodeChildNodes[n];
			for (int j=n/2;j<=previousNodeNumKeys;j++) {
                previousNodeChildNodes[j] = -1;
			}
			parentNodeChildNodes[counter]=newNode;
			writeInternal(previousNode,previousNodeKeys,previousNodeChildNodes,(n/2)-1,parentNode);
			changeParent(newNodeChildNodes,(n/2)-1,previousNode);
			writeInternal(newNode,newNodeKeys,newNodeChildNodes,(n/2)+1,parentNode);
			changeParent(newNodeChildNodes,(n/2)+1,newNode);
			writeInternal(parentNode,parentNodeKeys,parentNodeChildNodes,parentNodeNumKeys,parentNodeParent);
		}
	}
	
	
	public void changeParent(int [] childNodes,int numKeys,int parent) throws IOException{ //it is used to correct the parent nodes
		byte [] buffer=new byte[N];
		byte [] tmp=IntegerToByte(parent);
		int x;
		for(int i=0;i<numKeys+1;i++){
			x=0;
			buffer=read(childNodes[i]);
			if(isLeaf(buffer)){
				for(int j=16;j<20;j++){
					buffer[j]=tmp[x];
					x=x+1;
				}
			}
			else{
				for(int j=12;j<16;j++){
					buffer[j]=tmp[x];
					x=x+1;
				}
			}
			write(childNodes[i],buffer);
		}
	}
	
	public void writeBlankInternal(int pointer,int parent) throws IOException{
		byte [] buffer=new byte[N];
		byte [] tmp=IntegerToByte(-200);
		int x=0;
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(n);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(0);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(parent);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(-1);//pointer 0
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		x=20;
		for(int i=0;i<n;i++){
			tmp=IntegerToByte(-1);
			for (int j=0;j<4;j++){
				buffer[x]=tmp[j];
				x=x+1;
			}
			tmp=(new Data(m,-1)).getmData();
			for(int j=0;j<m;j++){
				buffer[x]=tmp[j];
				x=x+1;
			}
		}
		write(pointer,buffer);
	}
	public void writeBlankLeaf(int pointer,int parent) throws IOException {
		byte [] buffer=new byte[N];
		byte [] tmp=IntegerToByte(-300);
		int x=0;
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(m);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(0);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(-1);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(parent);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		x=20;
		for(int i=0;i<n;i++){
			tmp=IntegerToByte(-1);
			for (int j=0;j<4;j++){
				buffer[x]=tmp[j];
				x=x+1;
			}
			tmp=(new Data(m,-1)).getmData();
			for(int j=0;j<m;j++){
				buffer[x]=tmp[j];
				x=x+1;
			}
			
		}
		write(pointer,buffer);
	}
	public int getNextNode(byte [] buffer) {
		return ByteToInteger(Arrays.copyOfRange(buffer, 12, 16));
	}
	public int getParent(byte [] buffer){
		if(isLeaf(buffer)){
			return ByteToInteger(Arrays.copyOfRange(buffer, 16, 20));
		}
		else{
			return ByteToInteger(Arrays.copyOfRange(buffer, 12, 16));
		}
	}
	public Data [] getData(byte [] buffer) {
		Data [] mData=new Data[n];
		int tmp=20,key=0;
		int i=0;
		while(i<n){
			key=ByteToInteger(Arrays.copyOfRange(buffer, tmp, tmp+4));
			mData[i]=new Data(m,key);
		    mData[i].setmData(Arrays.copyOfRange(buffer, tmp+4, tmp+4+m));
		    tmp=tmp+4+m;
		    i++;
		}
		return mData;
	}
	public void writeLeaf(int node,int numKeys,int [] keys,Data [] mData,int NextNode,int parent) throws IOException{
		byte [] buffer=new byte[N];
		byte [] tmp;
		int x=0;
		tmp=IntegerToByte(-300);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(m);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(numKeys);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(NextNode);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			
			x=x+1;
		}
		tmp=IntegerToByte(parent);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		for(int i=0;i<n;i++){
			tmp=IntegerToByte(keys[i]);
			for(int j=0;j<4;j++){
				buffer[x]=tmp[j];
				x=x+1;
			}
			for (int z=0;z<m;z++){
				buffer[x]=mData[i].getmData()[z];
				x=x+1;
			}
		}
		write(node,buffer);
	}
	public void writeInternal(int node,int [] keys,int [] childNodes,int NumKeys,int parent) throws IOException{
		byte [] buffer=new byte[N];
		byte [] tmp=IntegerToByte(-200);
		int x=0;
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(n);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(NumKeys);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(parent);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		tmp=IntegerToByte(childNodes[0]);
		for(int j=0;j<4;j++){
			buffer[x]=tmp[j];
			x=x+1;
		}
		for(int i=0;i<keys.length;i++){
			tmp=IntegerToByte(keys[i]);
			for(int j=0;j<4;j++){
				buffer[x]=tmp[j];
				x=x+1;
			}
			tmp=IntegerToByte(childNodes[i+1]);
			
			for (int z=0;z<4;z++){
				buffer[x]=tmp[z];
				x=x+1;
			}
		}
		write(node,buffer);
	}
	public int getNumKeys(byte [] buffer){
		return ByteToInteger(Arrays.copyOfRange(buffer, 8, 12));		
	}
    public boolean isLeaf(byte [] buffer) { //returns false if page is not a leaf and true if page is leaf  
    	byte [] tmpVal = Arrays.copyOfRange(buffer, 0, 4);
    	if(ByteToInteger(tmpVal)==-200){
    		return false;
    	}
    	else{
    		return true;
    	}
    }
    public int [] getKeys(byte [] buffer){
    	int [] keys=new int [n];
    	int i=0,temp=0,temp1;
    	if (isLeaf(buffer)){
    		temp1=20;
    		for(int j=0;j<n;j++){
				keys[j]=-1;
			}
    		while(i<getNumKeys(buffer)){
    			keys[i]=ByteToInteger(Arrays.copyOfRange(buffer, temp1, temp1+4));
    			temp1=temp1+m+4;
    			i=i+1;
    		}
    	}
    	else{
    		temp1=20;
    		for (int j=0;j<n;j++){
    			keys[j]=-1;
    		}
    		while(i<getNumKeys(buffer)){
    			keys[temp]=ByteToInteger(Arrays.copyOfRange(buffer, temp1, temp1+4));
    			temp1=temp1+8;
    			temp=temp+1;
    			i=i+1;
    		}
    	}
    	return keys;
    	
    }
    public int [] getChildNodes(byte [] buffer) {
    	int [] childNodes=new int[n+1];
    	int temp=0,i=0,x;
    	x=16;
    	for (i=0;i<n+1;i++){
    		childNodes[i]=-1;
    	}
    	i=0;
		while(i<getNumKeys(buffer)+1){
			childNodes[temp]=ByteToInteger(Arrays.copyOfRange(buffer, x, x+4));
			i=i+1;
			temp=temp+1;
			x=x+8;
		}
    	return childNodes;
    }
    public Data [] rangeSearch(int node,int min,int max) throws IOException{
    	boolean check=true;
    	diskAccesses=0;
    	node=getNodeLeafSearch(node,min);
    	Data [] mData=new Data[(max-min)+1];
    	int i=0;
    	int counter=0;
    	int Next=0;
    	Data [] finalData=null;
    	byteBuffer=read(node);
        Next=getNextNode(byteBuffer);
    	while(getKeys(byteBuffer)[i]<min&&i<getNumKeys(byteBuffer)){i++;}
    	while(Next!=-1){
    		if(check){
    			check=false;
    		}
    		else{
    			byteBuffer=read(Next);
    		}
    		while(i<getNumKeys(byteBuffer)&&max>=getKeys(byteBuffer)[i]){
    			mData[counter]=(new Data(m,-1));
    			
    			mData[counter].setmData(getData(byteBuffer)[i].getmData());
    			mData[counter].setKey(getData(byteBuffer)[i].getKey());
    			counter++;
    			i++;
    		}
    		if(i>=getNumKeys(byteBuffer)){
    			i=0;
    			Next=getNextNode(byteBuffer);
    		}
    		else{
    			Next=-1;
    		}
    	}
    	finalData=new Data[counter];
    	for(int j=0;j<counter;j++){
    		finalData[j]=mData[j];
    	}
    	return finalData;
    }
    
    public int getNodeLeafSearch(int node,int key) throws IOException{
    	byteBuffer=read(node);
    	int i=0;
    	if(isLeaf(byteBuffer)){
    		while(getKeys(byteBuffer)[i]!=key&&i<getNumKeys(byteBuffer)){
    			i++;
    		}
    		return node;
    	}
    	else{
    		while(i<getNumKeys(byteBuffer)&&key>=getKeys(byteBuffer)[i]){
    			i++;
    		}
    		return getNodeLeafSearch(getChildNodes(byteBuffer)[i],key);
    	
    	}
    }
    public byte [] search(int node, int key,boolean firstTime) throws IOException {
    	byteBuffer=read(node);
    	if(firstTime){
    		diskAccesses=0;
    	}
    	int i=0;
    	if(isLeaf(byteBuffer)){
    		while(getKeys(byteBuffer)[i]!=key&&i<getNumKeys(byteBuffer)){
    			i++;
    			
    		}
    		return getData(byteBuffer)[i].getmData();
    	}
    	else{
    		while(i<getNumKeys(byteBuffer)&&key>=getKeys(byteBuffer)[i]){
    			i++;
    		}
    		return search(getChildNodes(byteBuffer)[i],key,false);
    	
    	}
    }
    
    
    public int ByteToInteger(byte [] bytes){
		return ByteBuffer.wrap(bytes).getInt();
	}

	public byte [] IntegerToByte(int element){
		return ByteBuffer.allocate(4).putInt(element).array();
	}
	public RandomAccessFile getMyFile(){
		return myFile;
	}
	public int getDiskAccesses(){
		return diskAccesses;
	}
}
