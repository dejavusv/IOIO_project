package com.example.ioio_coinacceptor;

import java.io.IOException;


import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class CoinAcceptor extends IOIOActivity{


	
	TextView wanted,change,inputBank;
	Button backBut;
	int money =0;
	String cost ="";
	boolean isConnect =false;
	Context context = (Context) this;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.coinacceptor);

		//EnEscroBut = (Button)findViewById(R.id.EnEscro);
		backBut = (Button)findViewById(R.id.backMoney);
		wanted = (TextView)findViewById(R.id.wantedMoney);
		inputBank =(TextView)findViewById(R.id.inputCoin);
		change = (TextView)findViewById(R.id.changeText);
		
		cost = getIntent().getStringExtra("money");
		if(cost != null){
			wanted.setText(cost+".00");
		}else{
			wanted.setText("no cost");
		}
		
		backBut.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					Intent i=new Intent(context, MainActivity.class);
					i.putExtra("Moneyresult", "0");
					i.putExtra("Language", getIntent().getExtras().getString("Language"));
					startActivity(i);	
					isConnect= true;
					finish();
					System.out.println("finish");
					
			}
		});
		
		 new CountDownTimer(7000, 1000) {

		     public void onTick(long millisUntilFinished) {
		    	 
		     }

		     public void onFinish() {
		    	 
		    	 if(!isConnect){
		    		 Toast.makeText(getApplicationContext(), 
								"cannot connect UCAES", Toast.LENGTH_SHORT).show();
		    		 Intent i=new Intent(context, MainActivity.class );
					 i.putExtra("Moneyresult", "0");
					 i.putExtra("Language", getIntent().getExtras().getString("Language"));
					 startActivity(i);
					 finish();
					 
					 
		    	 }
		     }
		  }.start();
	
	}
	


	class Looper extends BaseIOIOLooper {
		// Declare pulse input instance
		PulseInput echo;
		private DigitalOutput led_;
		private DigitalOutput inhibit;
		int numberOfPluse =0;
		
		public void readMoney(){
		    byte[] rdBuff;
		    int mymoney =0;
		    boolean IsUse = true;
	
		    while(IsUse){
		         try {
		        	 Thread.sleep(500);
				 } catch (InterruptedException e) {
					 e.printStackTrace();
				 }
		        
		        	 int coin = getcoin();
					 System.out.println("coin :"+coin);
					 if(coin !=  0){
						IsUse = true;
						mymoney += coin;
						int temp = money + mymoney;
						int value = Integer.parseInt(cost);
						if((temp == value)||(temp > value)){
							       Thread rq = new Thread("Thread7") {
								        @Override
								        public void run() {
								            runOnUiThread(new Runnable() {
								                public void run() {     	
								                	change.setText("");
								                	inputBank.setText("");
								                }
								            });

								        }
								    };
								    rq.start();
						       IsUse = false;		
						}else if(temp <= value){
						       money += mymoney;
						      
								Thread r = new Thread("Thread2") {
							        @Override
							        public void run() {
							        	final int temp = Integer.parseInt(cost);
							            runOnUiThread(new Runnable() {
							                public void run() {
							                	 int data = temp -money;
							                	 String datashowing ="";
							                	 String moneyShowing = "";
							                	 if(data == 0){
							                		 datashowing = "0.00";
							                	 }else if(data >= 0){
							                		 datashowing = data+".00";
							                	 }
							                	 if(data == 0){
							                		 moneyShowing = "0.00";
							                	 }else if(data >= 0){
							                		 moneyShowing = money+".00";
							                	 }							                	 
							                	 change.setText(datashowing);
							                	 inputBank.setText(String.valueOf(moneyShowing));
							                }
							            });

							        }
							    };
							    r.start();
						       IsUse = true;
						       mymoney =0;
						}		    			
					}	
					 
	
		  }
		//    sendDataUart(Disable,1);
		    Intent i=new Intent(context, MainActivity.class);
		    System.out.println("cost "+cost);
		    if(cost.equalsIgnoreCase("20")){
		    	i.putExtra("Moneyresult", "20");
		    }else if(cost.equalsIgnoreCase("30")){
		    	i.putExtra("Moneyresult", "30");
		    }
			
			startActivity(i);
    	}
		
		public int getcoin(){
			numberOfPluse =0;
			try {
				echo = ioio_.openPulseInput(47,PulseInput.PulseMode.NEGATIVE);
			} catch (ConnectionLostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {

				float temp =0;
				System.out.println("wait pluse");
				
				temp = echo.waitPulseGetDuration();
				
				System.out.println("get pluse :"+temp);
				numberOfPluse += 1;
				float f =0;
				int count =0;
				boolean data = true;
				while(data){
					Thread.sleep(75);
					f = echo.getDuration();
					
					if(f != temp){
						numberOfPluse +=1;
						temp =f;
						count =0;
					}else{
						if(count == 5){
							data = false;
						}
						count++;		
					}
				}	
				echo.close();

			} catch (ConnectionLostException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(numberOfPluse == 4){numberOfPluse+=1;}
			if(numberOfPluse == 9){numberOfPluse+=1;}
			return numberOfPluse;
		}
		
		protected void setup() throws ConnectionLostException {
	
			
			Thread r = new Thread("Thread2") {
		        @Override
		        public void run() {
		        	readMoney();
		        }
		        
		    };
		    r.start();
		    
			backBut.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					
					AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
		            builder1.setMessage("confirm to cancel");
		            builder1.setCancelable(true);
		            builder1.setPositiveButton("Yes",
		                    new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int id) {
		                	Intent i=new Intent(context, MainActivity.class);
							i.putExtra("Moneyresult", "0");
							startActivity(i);	
		                	dialog.cancel();
		                	finish();
		                }
		            });
		            builder1.setNegativeButton("no",
		                    new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int id) {
		                	dialog.cancel();
		                
		                }
		            });

		            AlertDialog alert11 = builder1.create();
		            alert11.show();

					
				}
			});
			

			runOnUiThread(new Runnable() {
				public void run() {
					// Toast message "Connect"
					// when android device connect with IOIO board
					Toast.makeText(getApplicationContext(), "Connected!",
							Toast.LENGTH_SHORT).show();
					isConnect = true;
				}
			});
		}

		public void loop() throws ConnectionLostException {

		}
	}

	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}

