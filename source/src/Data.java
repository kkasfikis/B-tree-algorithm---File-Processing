import java.nio.ByteBuffer;
import java.util.Random;

public class Data {
			private byte [] mData;
			private int key;
			private int m;
			public Data(int m,int key){
				this.m=m;
				mData=new byte[m];
				this.key=key;
				allocateData();
			}
			public void allocateData(){ //when a new Data instance is created this method allocates random bytes to mData and then allocate the
										//the four last bytes with the key integer of the data!
				byte [] tmp=ByteBuffer.allocate(4).putInt(key).array();
				new Random().nextBytes(mData);
				for(int j=0;j<4;j++){
					mData[m-4+j]=tmp[j];
				}
			}
			public byte[] getmData() {
				return mData;
			}
			public void setmData(byte[] mData) {
				this.mData = mData;
			}
			public int getKey() {
				return key;
			}
			public void setKey(int key) {
				this.key = key;
			}
			
		}