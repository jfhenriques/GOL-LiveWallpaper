package golw.jh.com.gollivewallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.Random;


public class GOLWallpaperService extends WallpaperService {

    private GOLState state = null;

    @Override
    public Engine onCreateEngine()
    {
        state = null;
        return new DemoWallpaperEngine();
    }


    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        super.onCreate();
    }

    private static class GOLState {
        public boolean curState[][] = null;
        public boolean _temp[][] = null;

        private Paint alive,dead;

        private final int W, H;
        private final int FACTOR;
        private final int HALF_FACTOR;

        public GOLState(int w, int h, int factor)
        {
            this.FACTOR = ( factor <= 1 ) ? 1 : factor ;

            w /= FACTOR;
            h /= FACTOR;

            HALF_FACTOR = FACTOR/2;

            this.W = w;
            this.H = h;

            curState = new boolean[h][w];
            _temp = new boolean[h][w];

            Random r = new Random(System.currentTimeMillis());

            alive = new Paint();
            dead = new Paint();

            //alive.setColor(Color.DKGRAY);
            dead.setColor(Color.BLACK);
            alive.setColor(Color.rgb(0x33, 0x66, 0x11));

            int x,y;

            for(y=0; y < h; y++)
            {
                for(x=0; x < w; x++) {
                    // has possibility of 1/5 to be alive
                    curState[y][x] = ( r.nextInt(6) == 0 ) ;
                    //_temp   [y][x] = 0;
                }
            }
        }

        private int countNeigh(int x, int y)
        {
            return    isAlive(x - 1, y - 1)
                    + isAlive(x, y - 1)
                    + isAlive(x + 1, y - 1)

                    + isAlive(x - 1, y)
                    + isAlive(x + 1, y)

                    + isAlive(x - 1, y + 1)
                    + isAlive(x, y + 1)
                    + isAlive(x + 1, y + 1);
        }

        private int isAlive(int x, int y)
        {
            if( x < 0 )
                x = W-1;
            else
            if( x >= W )
                x = 0;

            if( y < 0 )
                y = H-1;
            else
            if( y >= H )
                y = 0;

            return curState[y][x] ? 1 : 0;
        }

        public void computeNextGen(Canvas canvas)
        {
            boolean _oldState[][];
            int neigh,y,x,xF,yF;
            boolean cur;

            for(y=0; y < H; y++)
            {
                for(x=0; x < W; x++) {

                    cur = isAlive(x,y) == 1;
                    neigh = countNeigh(x,y);

                    if( cur )
                        _temp[y][x] = (neigh == 2 || neigh == 3) ;

                    else
                        _temp[y][x] = (neigh == 3) ;


                    if( FACTOR > 1 ) {
                        xF = x*FACTOR;
                        yF = y*FACTOR;
                        canvas.drawRect(xF,yF, xF + FACTOR, yF + FACTOR, _temp[y][x] ? alive : dead);
                        //canvas.drawCircle(xF+ HALF_FACTOR, yF + HALF_FACTOR, HALF_FACTOR, _temp[y][x] ? alive : dead);
                    }
                    else
                        canvas.drawPoint(x, y, _temp[y][x] ? alive : dead );
                }
            }

            // Save allocated memory and shift
            _oldState = curState;
            curState = _temp;
            _temp = _oldState;
        }
    }


    private class DemoWallpaperEngine extends Engine {


        private boolean mVisible = false;
        private final Handler mHandler = new Handler();
        private final Runnable mUpdateDisplay = new Runnable() {
            @Override
            public void run() {
                draw();
            }};
        private long lastTouchUp = 0;
        private int touchCount = 0;


        @Override
        public void onTouchEvent(MotionEvent event) {

            if( event.getAction() == MotionEvent.ACTION_UP )
            {
                //System.out.println("------------------------" + (System.currentTimeMillis() - lastTouchUp) );
                if( lastTouchUp == 0 || ( System.currentTimeMillis() - lastTouchUp)  < 400 )
                {
                    lastTouchUp = System.currentTimeMillis();

                    touchCount++;

                    if( touchCount >= 2 )
                    {
                        state = null;
                        touchCount = 0;
                    }

                    //System.out.println("------------------------------------" + touchCount);
                }
                else {
                    touchCount = 0;
                    lastTouchUp = 0;
                }
            }
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {

                    if( state == null )
                        state = new GOLState(c.getWidth(), c.getHeight(), 16);

                    state.computeNextGen(c);
                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mUpdateDisplay);
            if (mVisible) {
                mHandler.postDelayed(mUpdateDisplay, 150);
            }
        }
        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                draw();
            } else {
                mHandler.removeCallbacks(mUpdateDisplay);
            }
        }
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            draw();
        }
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mUpdateDisplay);
        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            mVisible = false;
            mHandler.removeCallbacks(mUpdateDisplay);
        }
    }
}