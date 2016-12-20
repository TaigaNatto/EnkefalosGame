package org.t_robop.enkefalosgame;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.nifty.cloud.mb.core.NCMB;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mbaas連携
        NCMB.initialize(this.getApplicationContext(),
                "8b68c184aea27a5d9f047e4282003a3c53babbb748d81a65cbc733039d6480c9",
                "230f6c44049af9a66efadf32a2b8bb497b1310369670677238917a6cac7b971e");
        setContentView(R.layout.activity_main);
    }

    //とべええええええええ
    public void start(View v){
        Intent intent=new Intent(this,ButtleActivity.class);
        startActivity(intent);
    }
}
