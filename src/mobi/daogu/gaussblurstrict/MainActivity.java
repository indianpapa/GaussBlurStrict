package mobi.daogu.gaussblurstrict;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {
    private long mElapse;
    private GaussBlur mBlurMaker;

    /** Time stamp helper class.*/
    public final static class TimeStamp {
        private static long baseTime;

        /** Reset base time to current time to start calculate time elapse.*/
        public static void reset() {baseTime = System.currentTimeMillis();}

        /** Get time elapse base on the base time.
         * @param log : The message you would like logged.
         * @return Time elapse base on the time you reset the {@link TimeStamp}.*/
        public static long elapse() {
            long e = (System.currentTimeMillis() - baseTime);
            return e;
        }
    }

    private final class ProcessTask extends AsyncTask<Bitmap, Integer, Bitmap> {
        @Override
        protected void onPostExecute(Bitmap result) {
            ((ImageView)findViewById(R.id.imv)).setImageBitmap(result);
            ((TextView)findViewById(R.id.txtv_elapse)).setText("cost:" + mElapse + "ms");
        }

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            Bitmap ret = null;
            try {
                TimeStamp.reset();
                ret = mBlurMaker.generateStrict(params[0]);
                mElapse = TimeStamp.elapse();
            } catch (Exception e) {e.printStackTrace();}
            return ret;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mElapse = 0L;
        Bitmap src = BitmapFactory.decodeResource(getResources(), R.drawable.im_balloon);
        mBlurMaker = new GaussBlur(this, 20, src.getWidth(), src.getHeight());
        new ProcessTask().execute(src);
    }

    @Override
    protected void onDestroy() {
        mBlurMaker.release();
        super.onDestroy();
    }
}
