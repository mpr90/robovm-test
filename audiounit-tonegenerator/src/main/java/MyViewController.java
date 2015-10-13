

import org.robovm.apple.audiotoolbox.AudioSession;
import org.robovm.apple.audiotoolbox.AudioSession.InterruptionListener;
import org.robovm.apple.audiotoolbox.AudioSessionCategory;
import org.robovm.apple.audiotoolbox.AudioSessionInterruptionState;
import org.robovm.apple.audiotoolbox.AudioSessionProperty;
import org.robovm.apple.audiounit.AUMutableRenderActionFlags;
import org.robovm.apple.audiounit.AURenderCallback;
import org.robovm.apple.audiounit.AUScope;
import org.robovm.apple.audiounit.AUTypeOutput;
import org.robovm.apple.audiounit.AudioComponent;
import org.robovm.apple.audiounit.AudioComponentDescription;
import org.robovm.apple.audiounit.AudioUnit;
import org.robovm.apple.coreaudio.AudioBuffer;
import org.robovm.apple.coreaudio.AudioBufferList;
import org.robovm.apple.coreaudio.AudioFormat;
import org.robovm.apple.coreaudio.AudioFormatFlags;
import org.robovm.apple.coreaudio.AudioStreamBasicDescription;
import org.robovm.apple.coreaudio.AudioTimeStamp;
import org.robovm.apple.corefoundation.OSStatusException;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("MyViewController")
public class MyViewController extends UIViewController {
    private UILabel label;

	private AudioUnit toneUnit = null;
	private double sampleRate;
	private int numberOfChannels;
	private AURenderCallback callback;

    @IBOutlet
    public void setLabel(UILabel label) {
        this.label = label;
    }

	private void createToneUnit() throws OSStatusException {
		System.err.println("createToneUnit()");
		AudioComponentDescription defaultOutputDescription = AudioComponentDescription.createOutput(AUTypeOutput.RemoteIO);
		AudioComponent defaultOutput = AudioComponent.findNext(null, defaultOutputDescription);
		toneUnit = AudioUnit.create(defaultOutput);
		
		callback = new AURenderCallback() {

			double theta = 0;
			
			@Override
			public void onRender(AUMutableRenderActionFlags actionFlags, AudioTimeStamp timeStamp, int busNumber,
					int numberFrames, AudioBufferList data) throws OSStatusException {

				final int frequency = 400;

				// Fixed amplitude is good enough for our purposes
				double amplitude = 0.25;

				// Get the tone parameters out of the view controller
				double theta_increment = 2.0 * Math.PI * frequency / sampleRate;

				// This is a mono tone generator so we only need the first buffer
				int channel = 0;
				if (data != null) {
					AudioBuffer audioBuffer = data.getBuffer(channel);
					if (audioBuffer != null) {
						float[] buffer = audioBuffer.getDataAsFloatArray();

						// Generate the samples
						for (int frame = 0; frame < numberFrames; frame++) 
						{
							buffer[frame] = (float) (Math.sin(theta) * amplitude);
							
							theta += theta_increment;
							if (theta > 2.0 * Math.PI)
							{
								theta -= 2.0 * Math.PI;
							}
						}
						data.getBuffer(channel).setData(buffer);
					} else {
						System.err.print('*');
					}
				} else {
					System.err.print('.');
				}
			}
		};
		toneUnit.setRenderCallback(callback, AUScope.Input);
		
		// channel is a discrete track of monophonic audio. A monophonic stream has one channel; a stereo stream has two channels.
		// sample is single numerical value for a single audio channel in an audio stream.
		// frame is a collection of time-coincident samples. For instance, a linear PCM stereo sound file has two samples per frame, one for the left channel and one for the right channel.
		// packet is a collection of one or more contiguous frames. A packet defines the smallest meaningful set of frames for a given audio data format, and is the smallest data unit for which time can be measured. In linear PCM audio, a packet holds a single frame. In compressed formats, it typically holds more; in some formats, the number of frames per packet varies.
		AudioStreamBasicDescription streamFormat = new AudioStreamBasicDescription(
				sampleRate,				// sampleRate
				AudioFormat.LinearPCM,
				AudioFormatFlags.with(AudioFormatFlags.NativeFloatPacked, AudioFormatFlags.LinearPCMFormatFlagIsNonInterleaved),
				numberOfChannels*4,		// bytesPerPacket (4 bytes per float)
				1,						// framesPerPacket
				numberOfChannels*4,		// bytesPerFrame
				numberOfChannels,   	// channelsPerFrame
				4*8);					// bitsPerChannel (4 bytes per float * 8 bits per byte)
		toneUnit.setStreamFormat(streamFormat, AUScope.Input);
	}
	
	public void start() {
		if (toneUnit == null) {
			try {
				createToneUnit();
			
				System.err.println("toneUnit.initialize()");
				// Stop changing parameters on the unit
				toneUnit.initialize();
			
				System.err.println("toneUnit.startOutput()");
				// Start playback
				toneUnit.startOutput();
			} catch (OSStatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
public void stop() {
	if (toneUnit != null) {
		try {
			toneUnit.stopOutput();
			toneUnit.uninitialize();
			toneUnit.dispose();
			toneUnit = null;			
		} catch (OSStatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

public void togglePlay() {
		if (toneUnit == null) {
			start();
		} else {
			stop();
		}
	}
	
	@SuppressWarnings("unused")
	@IBAction
    private void clicked() {
        togglePlay();
        label.setText(toneUnit == null?"Stopped":"Playing");
    }

	@SuppressWarnings("deprecation")
	@Override
	public void viewDidLoad() {

		super.viewDidLoad();

		sampleRate = 44100;
		numberOfChannels = 1;

		try {
			AudioSession.initialize(null, (String)null, new InterruptionListener() {

				@Override
				public void onInterrupt(AudioSessionInterruptionState interruptionState) {
					stop();
				}
				
			});
		
			int value = (int) AudioSessionCategory.MediaPlayback.value();
			AudioSession.setProperty(AudioSessionProperty.AudioCategory, value);
			AudioSession.setActive(true);

		} catch (OSStatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
