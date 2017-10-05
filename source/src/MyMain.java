import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import tuc.ece.cs102.util.StandardInputRead;


public class MyMain {
	private int N=1024;
	private int n;
	private int m=10;
	private int min=1;
	private int max=100000;
	private int LastKeyInserted=0;
	private BPlusTree bplus;
	private boolean repeat=true;
	StandardInputRead sir;
	public MyMain(){}
	public static void main(String [] args) throws IOException {
		MyMain mymain=new MyMain();
		int x;
		mymain.sir=new StandardInputRead();
		mymain.bplus=new BPlusTree(mymain.N,mymain.m);
		System.out.println("==================================F I R S T   P A R T==================================");
		mymain.FirstPart();
		System.out.println("========================================================================================");
		System.out.println("==================================S E C O N D   P A R T==================================");
		mymain.SecondPart();
		System.out.println("========================================================================================");
		System.out.println("==================================B O N U S   P A R T==================================");
		while(mymain.repeat){
			mymain.Menu();
		}
		System.out.println("========================================================================================");
	}
	public void FirstPart() throws IOException{
		int x;
		int sum=0;
		RangeInsert(min,max);
		for(int i=0;i<20;i++){
			sum=sum+Search(getRandomNum(min,max));
		}
		System.out.println("The average number of disk accesses for the search is: "+(sum/20));
		sum=0;
		x=getRandomNum(1,max/2);
		for(int i=0;i<20;i++){
			sum=sum+RangeSearch(x, x+100);
			x=x+100;
		}
		System.out.println("The average number of disk accesses for the range search is: "+(sum/20));
	}
	public void SecondPart() throws IOException{
		int sum=0;
		for(int i=0;i<20;i++){
			sum=sum+delete(getRandomNum(min,max));
		}
		System.out.println("The average number of disk accesses for delete is: "+(sum/20));
	}
	public void insert(int key,boolean enabled) throws IOException{
		bplus.insert(key, new Data(m,key));
		LastKeyInserted=key;
		if(enabled){
			System.out.println("Key added to b+ tree with "+bplus.getDiskAccesses()+" disk accesses.");
		}
	}
	public void RangeInsert(int minKey,int maxKey) throws IOException{
		for (int i=minKey;i<maxKey;i++){
			bplus.insert(i, new Data(m,i));
		}
		System.out.println("Keys from "+minKey+" to "+maxKey+" were successfully inserted to the b+ tree");
		LastKeyInserted=maxKey;
	}
	public int delete(int key) throws IOException{
		bplus.delete(key);
		System.out.println("-------------------------------------------------");
		System.out.println("The "+ key+" key and its data were successfully deleted from this b+ tree with "+bplus.getDiskAccesses()+" disk accesses.");
		System.out.println("-------------------------------------------------");
		return bplus.getDiskAccesses();
	}
	public int Search(int key) throws IOException{
		int x;
		byte[] tmp=bplus.search(1,key,true);
		x=ByteToInteger(Arrays.copyOfRange(tmp, m-4, m));
		System.out.println("-------------------------------------------------");
		System.out.println("The "+x+" key has data:"+tmp.toString()+" and the four last bytes of these data are(converted to integer):"+x+" and it was found with "+bplus.getDiskAccesses()+" disk accesses.");
		System.out.println("-------------------------------------------------");
		return bplus.getDiskAccesses();
	}
	public int RangeSearch(int minKeys,int maxKeys) throws IOException{
	    int x,y;
	    int sum=0;
		Data [] mData=bplus.rangeSearch(1,minKeys,maxKeys);
		System.out.println("-------------------------------------------------");
	    System.out.println("The data range search returned:");
		for (int j=0;j<mData.length;j++){
			y=mData[j].getKey();
			byte [] tmp=mData[j].getmData();
			x=ByteToInteger(Arrays.copyOfRange(tmp,m-4,m));
			sum=sum+bplus.getDiskAccesses();
			System.out.println("The " +y +" key has data:"+tmp.toString()+" and the four last bytes of these data are(converted to integer):"+x);
		}
		System.out.println("-------------------------------------------------");
		return sum/(maxKeys-minKeys);
	}
	public int ByteToInteger(byte [] bytes){
			return ByteBuffer.wrap(bytes).getInt();
	}
	public void Menu() throws IOException{
		int choice;
		int input;
		int input1;
		System.out.println("What would you like to do ?");
		System.out.println("1. Insert key to the existing b+ tree");
		System.out.println("2. Delete key from the existing b+ tree");
		System.out.println("3. Insert a range of keys");
		System.out.println("4. Create new b+ tree with different m,N");
		System.out.println("5. Search a key and get his data");
		System.out.println("6. Search a range of keys");
		System.out.println("7. Exit");
		choice=sir.readPositiveInt ("Enter your choice:");
		switch(choice){
		case 1:
			input=sir.readPositiveInt("Enter key to insert to b+ tree:");
			while(input<=LastKeyInserted){
				System.out.println("Enter a key greater than "+LastKeyInserted);
				input=sir.readPositiveInt("Enter key to insert to b+ tree:");
			}
			insert(input,true);
			break;
		case 2:
			input=sir.readPositiveInt("Enter key to delete from b+ tree:");
			delete(input);
			break;
		case 3:
			input=sir.readPositiveInt("Enter the min key for range insert:");
			input1=sir.readPositiveInt("Enter the max key for range insert:");
			while(input<=LastKeyInserted){
				System.out.println("Enter a key greater than "+LastKeyInserted);
				input=sir.readPositiveInt("Enter the min key for range insert:");
				input1=sir.readPositiveInt("Enter the max key for range insert:");
				RangeInsert(input,input1);
			}
			RangeInsert(input,input1);
			break;
		case 4:
			LastKeyInserted=0;
			bplus.getMyFile().close();
			System.out.println("Enter m,N in order to create a new b+ tree");
			input=sir.readPositiveInt("Enter N:");
			input1=sir.readPositiveInt("Enter m:");
			bplus=new BPlusTree(N,m);
			break;
		case 5:
			input=sir.readPositiveInt("Enter the key you want to search:");
			Search(input);
			break;
		case 6:
			input=sir.readPositiveInt("Enter the min key for range search:");
			input1=sir.readPositiveInt("Enter the max key for range search:");
			RangeSearch(input,input1);
			break;
		case 7:
			bplus.getMyFile().close();
			repeat=false;
			break;
		default:
			System.out.println("You entered an invalid choice!");
		}
	}
	public int getRandomNum(int min,int max){
		Random rand=new Random();
		return rand.nextInt(max - min + 1) + min;
	}
}
