package golw.jh.com.gollivewallpaper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.Random;


public class GOLWallpaperService extends WallpaperService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static GOLState state = null;
    private static Paint alive,dead;
    private static int refresh_ms;
    private static int base_factor;
    private static int base_gen_prob;
    private static boolean redrawEverything = true;

    private static final int DEFAULT_FACTOR = 16;
    private static final int DEFAULT_REFRESH_RATE = 5;
    private static final int DEFAULT_GEN_PROB = 5;

    private static final int DEFAULT_BACK_COLOR = Color.BLACK;
    private static final int DEFAULT_CELL_COLOR = Color.rgb(0x33, 0x66, 0x11);

    static {
        alive = new Paint();
        dead = new Paint();

        refresh_ms = DEFAULT_REFRESH_RATE;
        base_factor = DEFAULT_FACTOR;
        base_gen_prob = DEFAULT_GEN_PROB;

        //alive.setColor(Color.DKGRAY);
        dead.setColor(DEFAULT_BACK_COLOR);
        alive.setColor(DEFAULT_CELL_COLOR);
    }




    private static void setDeadPaint(int color)
    {
        dead.setColor(color);

        redrawEverything = true;
    }
    private static void setCellPaint(int color)
    {
        alive.setColor(color);

        redrawEverything = true;
    }

    private static void setRefreshMS(int ms)
    {
        if( ms < 0 )
            ms = 1;
        else
        if( ms > 20 )
            ms = 20;

        refresh_ms = 1000 / ms;
    }

    private static void setGenProb(String _prob)
    {
        //android.os.Debug.waitForDebugger();
        int p;
        try {
            p = Integer.parseInt(_prob);
        } catch(Exception e) {
            p = DEFAULT_GEN_PROB;
        }

        if( p < 0 )
            p = DEFAULT_GEN_PROB;

        base_gen_prob = p;
    }

    private static void setFactor(String _factor)
    {
        int f;
        try {
            f = Integer.parseInt(_factor);
        } catch(Exception e) {
            e.printStackTrace();
            f = DEFAULT_FACTOR;
        }

        if( f < 0 )
            f = DEFAULT_FACTOR;

        base_factor = f;
        state = null;
    }




    private SharedPreferences prefs;

    @Override
    public Engine onCreateEngine()
    {
        //
        state = null;

        setRefreshMS( prefs.getInt("pref_max_fps", DEFAULT_REFRESH_RATE) );
        setFactor( prefs.getString("pref_draw_factor", "" + DEFAULT_FACTOR) );
        setGenProb(prefs.getString("pref_gen_prob", "" + DEFAULT_GEN_PROB));
        setDeadPaint(prefs.getInt("pref_color_back", DEFAULT_BACK_COLOR));
        setCellPaint(prefs.getInt("pref_color_cell", DEFAULT_CELL_COLOR));

        return new WallpaperEngine();
    }


    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        super.onCreate();

        prefs = PreferenceManager.getDefaultSharedPreferences(GOLWallpaperService.this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s)
    {
        if( s != null )
        {
            if( s.equals("pref_max_fps") )
            {
                setRefreshMS( prefs.getInt("pref_max_fps", DEFAULT_REFRESH_RATE) );
            }

            else
            if( s.equals("pref_draw_factor") )
            {
                setFactor( prefs.getString("pref_draw_factor", "" + DEFAULT_FACTOR) );
            }

            else
            if( s.equals("pref_gen_prob") )
            {
                setGenProb(prefs.getString("pref_gen_prob", "" + DEFAULT_GEN_PROB));
            }

            else
            if( s.equals("pref_color_back") )
            {
                setDeadPaint(prefs.getInt("pref_color_back", DEFAULT_BACK_COLOR));
            }

            else
            if( s.equals("pref_color_cell") )
            {
                setCellPaint(prefs.getInt("pref_color_cell", DEFAULT_CELL_COLOR));
            }
        }
    }

    private static class GOLState {
        public byte curState[][] = null;
        public byte _temp[][] = null;


        private Bitmap offScreenBitmap;
        private Canvas offScreenCanvas;



        public final int W, H;
        public final int FACTOR;

        private int X_START=0,Y_START=0;


        public GOLState(int w, int h, int factor)
        {
            //android.os.Debug.waitForDebugger();
            int x,y;

            this.FACTOR = ( factor <= 1 ) ? 1 : factor ;

            // use w and h before scalling
            offScreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            offScreenCanvas = new Canvas(offScreenBitmap);

            w /= FACTOR;
            h /= FACTOR;

            this.W = w;
            this.H = h;

            curState = new byte[h][w];
            _temp = new byte[h][w];

            Random r = new Random(System.currentTimeMillis());

            offScreenCanvas.drawPaint(dead);

            for(y=0; y < h; y++)
            {
                for(x=0; x < w; x++) {
                    // has possibility of 1/5 to be alive
                   curState[y][x] = (byte)( ( r.nextInt(base_gen_prob) == 0 ) ? 1 : 0 );
                }
            }
        }

        public void incXStart(int inc)
        {
            if( inc == 0 )
                return;

            X_START = ( X_START + inc ) % W ;

            if( X_START < 0 )
                X_START += W;

            redrawEverything = true;
        }
        public void incYStart(int inc)
        {
            if(inc == 0)
                return;

            Y_START = ( Y_START + inc ) % H ;

            if( Y_START < 0 )
                Y_START += H;

            redrawEverything = true;
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
            int neigh,y,x,drawY,drawX,
                yy_start = Y_START,
                xx_start = X_START;
            byte cur,oldS;
            boolean startRedrawState = redrawEverything;
            Rect rect = new Rect();

            for( y=0; y < H; y++ )
            {
                drawY = FACTOR * ( (y + yy_start) % H ) ;

                for( x=0 ; x < W ; x++ )
                {
                    cur = oldS = isAlive( x, y );
                    neigh = countNeigh( x, y );

                    if( cur == 1 ) // is alive
                        cur = (byte) ( ( neigh == 2 || neigh == 3 ) ? 1 : 0 );

                    else // is dead
                         cur = (byte) ( ( neigh == 3 ) ? 1 : 0 );

                    _temp[y][x] = cur;

                    if( redrawEverything || cur != oldS )
                    {
                        drawX = FACTOR * ( (x + xx_start) % W );
                        //rect.set(drawX, drawY, drawX + FACTOR, drawY + FACTOR);

                        offScreenCanvas.drawRect(drawX, drawY, drawX + FACTOR, drawY + FACTOR, ( cur == 1 ) ? alive : dead);

//                        if( cur == 1 ) {
//                            offScreenCanvas.drawLine(rect.left, rect.top, rect.right, rect.top, border);
//                            offScreenCanvas.drawLine(rect.left, rect.bottom-1, rect.right, rect.bottom-1, border);
//
//                            offScreenCanvas.drawLine(rect.left, rect.top, rect.left, rect.bottom-1, border);
//                            offScreenCanvas.drawLine(rect.right-1, rect.top, rect.right-1, rect.bottom-1, border);
//                        }
                    }
                }
            }

            if( redrawEverything && startRedrawState )
                redrawEverything = false;

            // keep allocated memory and swap buffers
            _oldState = curState;
            curState = _temp;
            _temp = _oldState;

            canvas.drawBitmap(offScreenBitmap, 0, 0, null);
        }
    }


    private class WallpaperEngine extends Engine {


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

            if( holder == null )
                return;

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {

                    if( state == null )
                        state = new GOLState(c.getWidth(), c.getHeight(), base_factor);

                    state.computeNextGen(c);
                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mUpdateDisplay);
            if (mVisible) {
                mHandler.postDelayed(mUpdateDisplay, refresh_ms);
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
            state = null;
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