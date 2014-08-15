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
        public byte curState[][] = null;
        public byte _temp[][] = null;

        private Paint alive,dead;

        public final int W, H;
        public final int FACTOR;

        private int X_START=0,Y_START=0;

        public GOLState(int w, int h, int factor)
        {
            this.FACTOR = ( factor <= 1 ) ? 1 : factor ;

            w /= FACTOR;
            h /= FACTOR;

            this.W = w;
            this.H = h;

            curState = new byte[h][w];
            _temp = new byte[h][w];

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
                    curState[y][x] = (byte) (( r.nextInt(6) == 0 ) ? 1 : 0);
                    //_temp   [y][x] = 0;
                }
            }
        }

        public void incXStart(int inc)
        {
            X_START = ( X_START + inc ) % W ;

            if( X_START < 0 )
                X_START += W;
        }
        public void incYStart(int inc)
        {
            Y_START = ( Y_START + inc ) % H ;

            if( Y_START < 0 )
                Y_START += H;
        }

        private int countNeigh(int x, int y)
        {
            return    isAlive(x - 1, y - 1)
                    + isAlive(x    , y - 1)
                    + isAlive(x + 1, y - 1)

                    + isAlive(x - 1, y    )
                    + isAlive(x + 1, y    )

                    + isAlive(x - 1, y + 1)
                    + isAlive(x    , y + 1)
                    + isAlive(x + 1, y + 1);
        }

        private byte isAlive(int x, int y)
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

            return curState[y][x];
        }

        public void computeNextGen(Canvas canvas)
        {
            byte _oldState[][];
            int neigh,y,x,xF,yF,drawY,drawX;
            boolean cur;

            for(y=0; y < H; y++)
            {
                drawY = (y + Y_START) % H;

                for(x=0; x < W; x++) {

                    cur = ( isAlive(x,y) == 1 );
                    neigh = countNeigh(x,y);

                    if( cur ) {
                        cur = ( neigh == 2 || neigh == 3 );
                        _temp[y][x] = (byte) (cur ? 1 : 0);
                    }
                    else {
                        cur = ( neigh == 3 );
                        _temp[y][x] = (byte) (cur ? 1 : 0);
                    }

                    if( FACTOR > 1 ) {

                        drawX = (x + X_START) % W;

                        xF = drawX*FACTOR;
                        yF = drawY*FACTOR;
                        canvas.drawRect(xF,yF, xF + FACTOR, yF + FACTOR, cur ? alive : dead);
                    }
                    else
                        canvas.drawPoint(x, y, cur ? alive : dead );
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

        private float xStart = 0,yStart=0;


        @Override
        public void onTouchEvent(MotionEvent event) {

            switch(event.getAction()) {

                case  MotionEvent.ACTION_UP:

                    if (lastTouchUp == 0 || (System.currentTimeMillis() - lastTouchUp) < 300) {
                        lastTouchUp = System.currentTimeMillis();

                        touchCount++;

                        if (touchCount >= 2) {
                            state = null;
                            touchCount = 0;
                        }
                    } else {
                        touchCount = 0;
                        lastTouchUp = 0;
                    }

                    break;

                case MotionEvent.ACTION_DOWN:

                    xStart = event.getX();
                    yStart = event.getY();

                    break;

                case MotionEvent.ACTION_MOVE:

                    int delta_factor;
                    GOLState state = GOLWallpaperService.this.state; // just in case it gets unset during calculations

                    if( state == null )
                        break;

                    float delta = event.getX() - xStart;


                    if( delta >= state.FACTOR || delta <= state.FACTOR )
                    {
                        delta_factor = (int) (delta/state.FACTOR);

                        xStart += ( delta_factor*state.FACTOR );

                        if( xStart < 0 )
                            xStart = 0;

                        state.incXStart(delta_factor);
                    }


                    delta = event.getY() - yStart;

                    if( delta >= state.FACTOR || delta <= state.FACTOR )
                    {
                        delta_factor = (int) (delta/state.FACTOR);

                        yStart += ( delta_factor*state.FACTOR );

                        if( yStart < 0 )
                            yStart = 0;

                        state.incYStart(delta_factor);
                    }

                    break;
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