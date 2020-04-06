package android.net.hooktest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);
    }


    public void onClick(View view)
    {
        //实现集中式登录
        SharedPreferences share = getSharedPreferences("radish", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putBoolean("login",true);
        editor.apply();
        Toast.makeText(this,"登录了",Toast.LENGTH_SHORT).show();
    }
}
