package com.gtug.speechtempo;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class Recorder extends Activity implements OnClickListener {
	private static final String TAG = "Recorder";
	private boolean isRunning;
	//Audio Configuration
	private static final int frequency = 8000;
	private static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private int recordBufferSize = AudioRecord.getMinBufferSize(frequency,
			channelConfiguration, audioEncoding);
	private int playBufferSize = AudioTrack.getMinBufferSize(frequency,
			channelConfiguration, audioEncoding);
	private AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
			frequency, channelConfiguration, audioEncoding, recordBufferSize);
	private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
			frequency, channelConfiguration, audioEncoding, playBufferSize,
			AudioTrack.MODE_STREAM);
	//Layout
	ImageButton recordButton;
	Visualizer mVisualizer;
	VisualizerView mVisualizerView;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		recordButton = (ImageButton) findViewById(R.id.imageButton1);
		recordButton.setOnClickListener(this);
		setupVisualizerFxAndUI();
		isRunning = false;
		
	}

	public void onClick(View v) {
		Log.d(TAG, "onClick: " + v);
		if (isRunning) {
			stop();
		} else {
			record();
		}
	}

	public void record() {
		
		isRunning = true;
		audioRecord.startRecording();
		audioTrack.play();
		
		Toast.makeText(this, "recording started", Toast.LENGTH_LONG).show();
		
		Thread thread = new Thread(new Runnable() {
			
			public void run() {
				Log.d("AudioStream", "Recording" );
				mVisualizer.setEnabled(true);
				while (isRunning) {
					
					byte[] buffer = new byte[recordBufferSize];
					int bufferReadResult = audioRecord.read(buffer, 0, recordBufferSize);
					audioTrack.write(buffer, 0, bufferReadResult);					
				}
			}
		});
		thread.start();
	}

	public void stop() {
		isRunning = false;
		audioRecord.stop();
		audioTrack.stop();
		Toast.makeText(this, "recording stopped", Toast.LENGTH_LONG).show();
	}

	private void setupVisualizerFxAndUI() {
		// Create a VisualizerView (defined below), which will render the
		// simplified audio wave form to a Canvas.
		mVisualizerView = (VisualizerView)findViewById(R.id.view1);

		// Create the Visualizer object and attach it to our media player.
		mVisualizer = new Visualizer(audioTrack.getAudioSessionId());
		mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
		mVisualizer.setDataCaptureListener(
				new Visualizer.OnDataCaptureListener() {
					
					private long past_time = 0;
					private long wait_time = 5000;
					private long time = 5000;
					
					public void onWaveFormDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
					
							if(time > wait_time && isRunning){
								time = 0;
								past_time = System.currentTimeMillis();
								mVisualizerView.updateVisualizer(bytes);
							}else{
								time = System.currentTimeMillis()-past_time;
							}
						
					}

					public void onFftDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
					}
				}, Visualizer.getMaxCaptureRate() / 2, true, false);
	}

}

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
class VisualizerView extends View {
	
	final private static int sampring_max = 100;
	
	private ArrayList<byte[]> savewave;
	private byte[] mBytes;
	private float[] mPoints;
	private Rect mRect = new Rect();
	private Paint mForePaint = new Paint();
	private int view_width, view_height;
	private int digree = 0;
	private int digree_sum = 0;
	public VisualizerView(Context context, AttributeSet attrs) {   
	    super(context, attrs);  
	    init();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		view_width = getWidth();
		view_height = getHeight();
		
	}

	private void init() {
		
		mBytes = null;
		mForePaint.setStrokeWidth(1f);
		mForePaint.setAntiAlias(true);
		savewave = new ArrayList<byte[]>();
		//mForePaint.setColor(Color.rgb(0, 128, 255));
		this.setBackgroundColor(Color.WHITE);
	}

	public void updateVisualizer(byte[] bytes) {
		mBytes = bytes;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mBytes == null) {
			return;
		}	
		savewave.add(mBytes);
		for(int i = 0; i < savewave.size(); i++){
			byte[] drawline = savewave.get(i);
			digree_sum = 0;
			for(int j = 0 ; j < sampring_max; j++){
				int colorvalue = 255 - (128-Math.abs(drawline[j]) * 20);
				if(colorvalue < 0) colorvalue = 0;
				mForePaint.setColor(Color.rgb(colorvalue, colorvalue, colorvalue));
				digree = (view_height - digree_sum) / (sampring_max -j);
				for(int k =0; k < digree ; k++){
					canvas.drawPoint(view_width - savewave.size()+i, view_height - digree_sum - k, mForePaint); //x: max y: max
				}
				digree_sum = digree_sum + digree;
			}
		}	
	}
}