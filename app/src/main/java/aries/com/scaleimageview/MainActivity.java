package aries.com.scaleimageview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.aries.scaleimageview.ScaleImageView;


public class MainActivity extends AppCompatActivity {

    private  ScaleImageView scaleImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scaleImageView = (ScaleImageView)findViewById(R.id.ivScaleImg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_portrait) {
            loadImage(R.drawable.test_471_766);
            return true;
        }

        if (id == R.id.action_landscape) {
            loadImage(R.drawable.test_1024_768);
            return true;
        }

        if (id == R.id.action_rotation){
            scaleImageView.rotate(180);
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadImage (int resId){
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        bitmap = BitmapFactory.decodeResource(getResources(), resId, options);
        scaleImageView.setImageBitmap(bitmap);
    }
}
