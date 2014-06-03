package com.ecmxpert.ioio_nfcmodulev3;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends IOIOActivity {
	
	Button version,samconfig,passivettarget,write,read,test16,test24,test32;
	TextView result;
	EditText writetext;
	
	byte[] GetFirmwareVersion = {(byte)0xD4,(byte)0x02};
	byte[] SetParameters = {(byte)0xD4,(byte)0x12,(byte)0x77};//0x96
	byte[] SAMConfig = {(byte)0xD4,(byte)0x14,(byte)0x01,(byte)0x20,(byte)0x00};
	byte[] nack = {(byte)0x00,(byte)0x00,(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x00};
	byte[] PASSIVETARGET={(byte)0xD4,(byte)0x4A,(byte)0x01,(byte)0x00};
	byte[] AuthenticateBlock={(byte)0xD4,(byte)0x40,(byte)0x01,(byte)0x60,(byte)0x03,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
	byte[] InSelect = {(byte)0xD4,(byte)0x54,(byte)0x00};
	byte[] WriteDataBlock = {(byte)0xD4,(byte)0x40,(byte)0x01,(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
	byte[] stop = {(byte)0x73,(byte)0x74,(byte)0x6F,(byte)0x70};
	byte[] ReadDataBlock = 	{(byte)0xD4,(byte)0x40,(byte)0x01,(byte)0x30,(byte)0x00};
	
	int indexBlocknumber = 4;
	int indexWriteBlock =5;
	int indexCardID= 11;
	int indexreadData =9;
	int indexftL = 7;
	int indexANSType = 8;
	int state =0;
	int startWriteBlock = 16; // first index block for write data
	int stopWriteBlock = 32;  // last index block for write data

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    
    class Looper extends BaseIOIOLooper {
    	
    	// Declare TWI instance
    	TwiMaster twi;
    	
    	// Declare output stream instance
    	OutputStream out;
    	
    	// Declare input stream instance
    	InputStream in;
    	
    	//buffer for get data from NFC
    	byte[] readBuf = new byte[30];
    	//buffer for get ACK From PN532
    	byte[] AckBuf = new byte[7];
    	
        protected void setup() throws ConnectionLostException {

    		write =(Button)findViewById(R.id.write);
    		read = (Button)findViewById(R.id.read);
    		writetext = (EditText)findViewById(R.id.writetext);
    		result = (TextView)findViewById(R.id.ResultText);
        	twi = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_400KHz, false);

        	write.setOnClickListener(new OnClickListener() {
        		public void onClick(View v) {
        			String data = result.getText().toString();
        			if(data.length()> ((stopWriteBlock-1) - startWriteBlock)*16){
        				DisplayResult("data max length : "+((stopWriteBlock-1) - startWriteBlock)*16);
        			}else if(data.length() == 0){
        				DisplayResult("please input data");
        			}else{
        				state = 1;
        			}
        		}
        	});
        	
        	read.setOnClickListener(new OnClickListener() {
        		public void onClick(View v) {
        			//set state for read data in NFC Tag
        			state = 2;
        		}
        	});
        	
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), 
                            "Connected!", Toast.LENGTH_SHORT).show();
                }        
            });
            
            //send command samconfig for setup PN532
            byte[] sampackage = createPackage(SAMConfig);
            int Ackresult = sendPackageAndwaitAck(sampackage);
            System.out.println("sampackage ");
            for(int i=0;i<sampackage.length;i++){
				System.out.print(sampackage[i]+" ");
			}  
            System.out.println("");
            for(int i=0;i<readBuf.length;i++){
				System.out.print(readBuf[i]+" ");
			}  
            //set state 0 for detect NFC TAG 
            state =0;    
            if(Ackresult == 1){
            	DisplayResult("wait for NFC tag");
            }else{
            	DisplayResult("cannot Detect card");
            }
           
        }
        
        public void DisplayResult( String data){
        	final String output = data;
        	Thread rq = new Thread("Thread7") {
		        @Override
		        public void run() {
		            runOnUiThread(new Runnable() {
		                public void run() {
		                	result.setText(output);
		                }
		            });
		        }
		    };
		    rq.start();
        }
        
        //************************ normal function for read write NFC ************************//
        public int[] calculateBlock(int dataLen){
        	int[] indexBlock = new int[2];
        	int blocksize =dataLen / 16;;
        	if(dataLen%16 != 0){
        		blocksize += 1;
        	}
        	//check trailer block in sector
        	for(int i=startWriteBlock;i<=startWriteBlock + blocksize;i++){
        		if(i % 4 == 3){
        			blocksize+= 1;
        		}
        	}
        	indexBlock[0] = startWriteBlock;
        	indexBlock[1] = startWriteBlock + blocksize;
        	return indexBlock;
        }
        
        public String printHEX(byte[] bytes,int start,int last){
        	StringBuilder sb = new StringBuilder();
            for (int i=start;i<last;i++) {
                sb.append(String.format("%02X ", bytes[i]));
                sb.append(" ");
            }
            return sb.toString();
        }
        
        public String printHEX(ArrayList<Byte> bytes){
        	StringBuilder sb = new StringBuilder();
            for(int i=0;i<bytes.size();i++){
            	sb.append(String.format("%02X ", bytes.get(i)));
                sb.append(" ");
            }
            return sb.toString();
        }
        
        public String convertByteToString(ArrayList<Byte> data){
        	byte[] output = new byte[data.size()];
        	for(int i=0;i<data.size();i++){
        		output[i] =  data.get(i);
        	}
        	return new String(output);
        }
        
        public String convertByteToString(byte[] data){
        	return new String(data);
        }
        
        public byte[] convertStringToByte(String data){
        	return data.getBytes();
        }
        
        public int sendPackageAndwaitAck(byte[] packageData ){
        	try {
				if(twi.writeRead(0x48 >> 1  , false, packageData, packageData.length,  new byte[0], 0)){
				
					if(twi.writeRead(0x49 >> 1  , false,  new byte[0], 0, AckBuf, AckBuf.length)){	  
						//check ack frame
						if(AckBuf[0]  == 1){
							if((AckBuf[1]  == 0)&&(AckBuf[2]  == 0)&&(AckBuf[3]  == -1)&&(AckBuf[4]  == 0)&&(AckBuf[5]  == -1)&&(AckBuf[6]  == 0)){
								for(int i=0;i<AckBuf.length;i++){
									AckBuf[i] =0;
								} 
								Thread.sleep(20);
								//read data frame
								if(twi.writeRead(0x49 >> 1  , false,  new byte[0], 0, readBuf, readBuf.length)){
									return 1;
								}else{	
									return 0;
								}
							}
						}else{
							return 0;
						}

					}    
				}else{
					System.out.println("error");	
					return 0;
				}
				
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        	return 0;
        }
            
        public void cleanwriteBuf(){
        	for(int j=0;j<16;j++){
        			WriteDataBlock[indexWriteBlock+j] = 0x00;
        	}
        	
        }
        
        public void clearBuf(){
			for(int i=0;i<readBuf.length;i++){
				readBuf[i] =0;
			}  
        }
        
        
        public byte[] createPackage(byte[] data){
        	int indexByte =0;
    		byte[] packageData = new byte[data.length+7];
        	try {
        		packageData[0] = (byte)0x00;
        		packageData[1] = (byte)0x00;
        		packageData[2] = (byte)0xFF;
        		byte checksumLen = (byte)(data.length);
        		checksumLen = (byte) (~checksumLen +1);
        		packageData[3] = (byte)(data.length);
        		packageData[4] = checksumLen;
        		for(int i=0;i<data.length;i++){
        			packageData[5 + i] = data[i];
        		}
        		indexByte = 5 + data.length;
        		int datasum =0;
        		for(int i=0;i<data.length;i++){
        			datasum += data[i];
        		}
        		
        		byte checksumData = (byte)(datasum);
        		checksumData = (byte)(~checksumData +1);
        		packageData[indexByte] = checksumData;
        		indexByte+=1;
        		packageData[indexByte] = (byte)00;
        		for(int i=0;i<packageData.length;i++){
        			System.out.print(String.valueOf(packageData[i])+" ");
        		}
        		System.out.println("");
        		return packageData;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        	return packageData;
        }
        
      //************************************************************************************//
        

        public void loop() throws ConnectionLostException { 
        	// state 0 : check  NFC if found will show UID of NFC TAG
        	if(state ==0){
        		byte[] target = createPackage(PASSIVETARGET);
                sendPackageAndwaitAck(target);   	
        		if(readBuf[0] == 1){
            		if(readBuf[7]==75){
    					for(int i=0;i<4;i++){
    						AuthenticateBlock[indexCardID+i]=readBuf[14+i];
    					}
    					DisplayResult(printHEX(readBuf,14,18));
    					state = 4;
    				}        			
        		}
        		
        	}else if(state == 1){ // state 1 : Authen block and write data in block
    			String data = writetext.getText().toString();
    			byte[] writeData = convertStringToByte(data);
    			System.out.print("write byte input : ");
    			for(int i=0;i<writeData.length;i++){
    				System.out.print(writeData[i]+" ");
    			}
    			System.out.println("");
    			byte[] ByteData = convertStringToByte(data);
    			int[] blocksize = calculateBlock(writeData.length);
    			int indexblock =0;
    			for(int i=blocksize[0];i<=blocksize[1];i++){
    				AuthenticateBlock[indexBlocknumber] = (byte)i;
    				System.out.print("result authen block "+AuthenticateBlock[indexBlocknumber]+":");
        			byte[] authen = createPackage(AuthenticateBlock);
                    int sendresult = sendPackageAndwaitAck(authen);
                    if((sendresult == 1)&&(readBuf[0] == 1)){
                    	
                       	 if(readBuf[8] == 0){
                       		 System.out.println("success to write data in block");
                       		
                 			//check last block in sector
                 			if(i % 4 != 3){
                     			//set data to write
                     			int startIndex = indexblock*16;
                     			System.out.print("index block");
                     			if(i==blocksize[1]){
                             		cleanwriteBuf(); 
                             		//set stop block
                             		WriteDataBlock[indexWriteBlock] = stop[0];
                             		WriteDataBlock[indexWriteBlock+1] = stop[1];
                             		WriteDataBlock[indexWriteBlock+2] = stop[2];
                             		WriteDataBlock[indexWriteBlock+3] = stop[3]; 
                             		state =3; 
                             	}else{
                             		System.out.print(indexWriteBlock+" ");
                                 	for(int j=0;j<16;j++){
                                 		if(startIndex+j < ByteData.length){
                                 				System.out.println(WriteDataBlock[indexWriteBlock+j]+"="+(indexWriteBlock+j)+","+ByteData[startIndex+j]+"="+(startIndex+j));
                                 				WriteDataBlock[indexWriteBlock+j] = ByteData[startIndex+j];
                                 		}
                                 	}                		
                             	}

                             	indexblock+=1;
                             	//set block
                             	System.out.println("set block "+i);
                     			WriteDataBlock[indexBlocknumber] = (byte)i;
                     			byte[] BlockwriteData = createPackage(WriteDataBlock);
                                 int writeresult = sendPackageAndwaitAck(BlockwriteData);
                              
                                 if(writeresult == 1){
                                 	if(readBuf[8] == 0){
                                 		System.out.println("write success");
                                 	}else{
                                 		state =4;
                                     	DisplayResult("write fail");   
                                     	i+=blocksize[1];                   	
                                 	}                    	
                                 }else{
                                 	state =4;
                                 	DisplayResult("write fail");   
                                 	i+=blocksize[1];      	
                                 }       				
                 			}
                 			cleanwriteBuf(); 

                       	 }else if(readBuf[8] == 20){          		 
                       		 DisplayResult("fail authen error plese write again");
                       		 state = 0;
                       		 break;
                       	 }else{         		
                       		 DisplayResult("fail plese write again");
                       		 state = 0;
                       		 break;
                       	 }
                    }else{
                    	DisplayResult("cannot send package");
                    	 state = 0;
                   		 break;
                    }
    			}
        	}else if(state == 2){ // state 2 : Authen block and read data in block
        		int authenResult =0;
        		ArrayList<Byte> data = new ArrayList<Byte>();
        		for(int i=startWriteBlock;i<=stopWriteBlock;i++){
        			//check last block in sector
        			if(i % 4 != 3){
        				AuthenticateBlock[indexBlocknumber] = (byte)i;
        				System.out.print("result authen block "+AuthenticateBlock[indexBlocknumber]+":");
            			byte[] authen = createPackage(AuthenticateBlock);
                        int sendresult = sendPackageAndwaitAck(authen);
                        if(sendresult == 1){
                        	if(readBuf[0] == 1){
                           	 if(readBuf[8] == 0){
                           		 System.out.println("success");
                           		authenResult =1;
                           	 }else if(readBuf[8] == 20){
                           		 System.out.println("fail authen error");
                           		 DisplayResult("fail authen error plese write again");
                           		authenResult = 0;
                           		 break;
                           	 }else{
                           		 System.out.println("fail");
                           		 DisplayResult("fail plese write again");
                           		authenResult = 0;
                           		 break;
                           	 }
                           }
                        }else{
                        	DisplayResult("cannot send package");
                        	authenResult = 0;
                       		 break;
                        }
                        //read data in block
                        if(authenResult == 1){
                        	
                        	System.out.println("read block "+i);
                			ReadDataBlock[indexBlocknumber] = (byte)i;
                			byte[] readData = createPackage(ReadDataBlock);
                            int readresult = sendPackageAndwaitAck(readData);

                            for(int j=0;j<16;j++){
                        		if(readBuf[9+j] != 0){
                        			System.out.print(readBuf[9+j]+" ");
                        		}
                        	}
                            if((readBuf[0] == 1)&&(readBuf[8] == 0)&&(readresult == 1)){
                            	if((readBuf[9]==stop[0])&&(readBuf[10]==stop[1])&&(readBuf[11]==stop[2])&&(readBuf[12]==stop[3])){
                            		System.out.println("this is stop block");
                            		break;
                            	}else{
                            		for(int j=0;j<16;j++){
                                		if(readBuf[9+j] != 0){
                                			data.add(readBuf[9+j]);
                                		}
                                	}
                            	}		
                            } 
                		}       				
        				
        			}

        		}
        		DisplayResult("data : "+convertByteToString(data));
        		state = 4;
        	}else if(state == 3){//state 3 : wait 1 sec for show response
        		DisplayResult("write success");
        		state =0;
        	}else if(state == 4){ //state 4 : wait 1 sec for show response      	
        		state =0;	
        	}
        	
        	
        	
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}