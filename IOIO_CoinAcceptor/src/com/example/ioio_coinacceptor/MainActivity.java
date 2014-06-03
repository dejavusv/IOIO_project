package com.example.ioio_coinacceptor;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	String cost="";
	Button Twentycost,Thirtycost;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Twentycost = (Button) findViewById(R.id.Twentycost);
		Thirtycost = (Button) findViewById(R.id.Thirtycost);
		cost = getIntent().getStringExtra("Moneyresult");
		if(cost != null){
			if(cost.equalsIgnoreCase("30")){
				Toast.makeText(getApplicationContext(), 
						"success", Toast.LENGTH_SHORT).show();
			}else if(cost.equalsIgnoreCase("60")){
				Toast.makeText(getApplicationContext(), 
						"success", Toast.LENGTH_SHORT).show();
			}
		}
		
		Twentycost.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
				 Intent i = new Intent(v.getContext(), CoinAcceptor.class);
				 i.putExtra("money", "20");
				 //i.putExtra("Language", getIntent().getExtras().getString("Language"));
				 startActivity(i); 
				 finish();
            }
        });
		
		Thirtycost.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
				Intent i = new Intent(v.getContext(), CoinAcceptor.class);
				i.putExtra("money", "30");
				//i.putExtra("Language", getIntent().getExtras().getString("Language"));
				startActivity(i);  
				finish();
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
