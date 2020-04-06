package android.net.hooktest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    //博客地址
    //https://www.jianshu.com/p/2d6ccb7a1864

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view)
    {

        switch (view.getId())
        {
            case R.id.button2:
            {
                Intent intent = new Intent();
                intent.setClass(this,Main2Activity.class);
                startActivity(intent);
                break;
            }
            case R.id.button3:
            {
                Intent intent = new Intent();
                intent.setClass(this,Main3Activity.class);
                startActivity(intent);
                break;
            }
            case R.id.button4:
            {
                Intent intent = new Intent();
                intent.setClass(this,Main4Activity.class);
                startActivity(intent);
                break;
            }

        }

    }

}
