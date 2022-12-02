package net.sourceforge.sox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import android.content.Context;
import android.util.Log;
import com.vtech.audio.helper.SoxCommandLib;

public class SoxController {
    private final static String TAG = "SOX";
    private String soxBin;
    private Context context;
    private ShellCallback callback;


    private NumberFormat mFormatValue;
    private DecimalFormat mFormatTrim;

    public SoxController(Context _context, ShellCallback _callback) throws FileNotFoundException, IOException {
        context = _context;
        callback = _callback;

        soxBin = "sox";


        mFormatValue = NumberFormat.getInstance(Locale.US);
        mFormatValue.setMaximumFractionDigits(1);

        mFormatTrim = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
        mFormatTrim.applyPattern("###.####");
    }


    private class LengthParser implements ShellCallback {
        public double length;
        public int retValue = -1;

        @Override
        public void shellOut(String shellLine) {
            Log.d("sox", shellLine);
            if( !shellLine.startsWith("Length") )
                return;
            String[] split = shellLine.split(":");
            if(split.length != 2) return;

            String lengthStr = split[1].trim();

            try {
                length = Double.parseDouble( lengthStr );
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void processComplete(int exitValue) {
            retValue = exitValue;

        }
    }

    /**
     * Retrieve the length of the audio file
     * sox file.wav 2>&1 -n stat | grep Length | cut -d : -f 2 | cut -f 1
     * @return the length in seconds or null
     */
    public double getLength(String path) {
        checkForInvalidSpaces(path);
        ArrayList<String> cmd = new ArrayList<String>();

        cmd.add(soxBin);
        cmd.add(path);
        cmd.add("-n");
        cmd.add("stat");

        LengthParser sc = new LengthParser();

        int rc = execSox(cmd, callback);
        if( rc != 0 ) {
            Log.e(TAG,"error getting length ");
        }

        return sc.length;
    }


    /**
     * Change audio volume
     * sox -v volume <path> outFile
     * @param volume
     * @return path to trimmed audio
     */
    public String setVolume(String inputFile, float volume, String outputFile) throws IOException {
        checkForInvalidSpaces(inputFile); checkForInvalidSpaces(outputFile);

        ArrayList<String> cmd = new ArrayList<String>();

        File file = new File(inputFile);
        cmd.add(soxBin);
        cmd.add("-v");
        cmd.add(mFormatValue.format(volume));
        cmd.add(inputFile);
        cmd.add(outputFile);

        int rc = execSox(cmd, callback);
        if( rc != 0 ) {
            Log.e(TAG, "trimAudio receieved non-zero return code!");
            return null;
        }

        if (file.exists())
            return outputFile;
        else
            return null;

    }

    /**
     * Discard all audio not between start and length (length = end by default)
     * sox <path> -e signed-integer -b 16 outFile trim <start> <length>
     * @param start
     * @param length (optional)
     * @return path to trimmed audio
     */
    public String trimAudio(String path, double start, double length, float volume) throws IOException {
        checkForInvalidSpaces(path);

        ArrayList<String> cmd = new ArrayList<String>();

        File file = new File(path);
        String outFile = file.getCanonicalPath() + "_trimmed.wav";
        cmd.add(soxBin);
        cmd.add("-v");
        cmd.add(mFormatValue.format(volume));
        cmd.add(path);
        cmd.add("-e");
        cmd.add("signed-integer");
        cmd.add("-b");
        cmd.add("16");
        cmd.add(outFile);
        cmd.add("trim");
        cmd.add(mFormatTrim.format(start));
        if( length != -1 )
            cmd.add(mFormatTrim.format(length));

        int rc = execSox(cmd, callback);
        if( rc != 0 ) {
            Log.e(TAG, "trimAudio receieved non-zero return code!");
            outFile = null;
        }

        if (file.exists())
            return outFile;
        else
            return null;

    }


    public String setStrictSampleRate(String file, String outFile) {
        checkForInvalidSpaces(file); checkForInvalidSpaces(outFile);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);
        cmd.add(file);

        cmd.add("-r");
        cmd.add("44100");

        cmd.add(outFile);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            outFile = null;
        }

        return outFile;
    }


    public String convertMp3ToWav(String file, String outFile) { //mp3, output wav path
        checkForInvalidSpaces(file); checkForInvalidSpaces(outFile);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);

        cmd.add(file); // mp3
        cmd.add(outFile); // wave path


        int rc = execSox(cmd, callback);
        if(rc != 0) {
//            	Log.e(TAG, "combineMix received non-zero return code!");
            outFile = null;
        }

        return outFile;
    }


    //http://stackoverflow.com/questions/5587135/sox-merge-two-audio-files-with-a-pad?rq=1
    public String padBeginingOfAudio(String soundToPad, String outFile, double padSeconds) {
        checkForInvalidSpaces(soundToPad); checkForInvalidSpaces(outFile);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);

        cmd.add(soundToPad);
        cmd.add(outFile);
        cmd.add("pad");
        cmd.add("" + padSeconds);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            outFile = null;
        }

        return outFile;
//		$ sox short.ogg delayed.ogg pad 6
    }

    /**
     * Combine and mix audio files
     * sox -m -v 1.0 file[0] -v 1.0 file[1] ... -v 1.0 file[n] outFile
     * TODO support passing of volume
     * @param files
     * @return combined and mixed file (null on failure)
     */
    public String combineMix(List<String> files, String outFile) {
        for(String file : files){
            checkForInvalidSpaces(file);
        }
        checkForInvalidSpaces(outFile);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);
        cmd.add("-m");

        for(String file : files) {
            cmd.add("-v");
            cmd.add("1.0");
            cmd.add(file);
        }
        cmd.add(outFile);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
//            	Log.e(TAG, "combineMix received non-zero return code!");
            outFile = null;
        }
        return outFile;
    }



    /**
     * Discard all audio not between start and length (length = end by default)
     * sox <path> -e signed-integer -b 16 outFile trim <start> <length>
     * @param start
     * @param length (optional) duration from trim start to trim end
     * @param outPath  this is where the resultant audio will be saved
     * @return path to trimmed audio
     */
    public String trimAudio2(String path, double start, double length, Boolean setStrictSampleRate, String outPath) throws Exception {
        checkForInvalidSpaces(path); checkForInvalidSpaces(outPath);
        ArrayList<String> cmd = new ArrayList<String>();

        File file = new File(path);
        String outFile = outPath;
        cmd.add(soxBin);
        cmd.add(path);

        if(setStrictSampleRate){
            cmd.add("-r");
            cmd.add("44100");
        }

        cmd.add("-e");
        cmd.add("signed-integer");
        cmd.add("-b");
        cmd.add("16");
        cmd.add(outFile);
        cmd.add("trim");
        cmd.add(start+"");
        if( length != -1 )
            cmd.add(length+"");

        int rc = execSox(cmd, callback);
        if( rc != 0 ) {
            outFile = null;
        }

        if (file.exists())
            return outFile;
        else
            return null;

    }



    //http://www.linuxandlife.com/2013/03/how-to-use-sox-audio-editing.html
    public String convertMonoToStereo(String file, int outPutChannels, String outFile) {
        checkForInvalidSpaces(file); checkForInvalidSpaces(outFile);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);

        cmd.add(file);
        cmd.add("-c");
        cmd.add("" + outPutChannels);
        cmd.add(outFile);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            //	Log.e(TAG, "combineMix receieved non-zero return code!");
            outFile = null;
        }
        return outFile;
    }

    public String adjustVolume(String file, double value, String outFile) {
        checkForInvalidSpaces(file);

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);

        cmd.add("-v");
        cmd.add("" + value);
//		cmd.add("1.0");
        cmd.add(file);
        cmd.add(outFile);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            //	Log.e(TAG, "combineMix receieved non-zero return code!");
            outFile = null;
        }

        return outFile;
    }

    public String delayAudio(String path, double start, double length) throws IOException {
        checkForInvalidSpaces(path);
        ArrayList<String> cmd = new ArrayList<String>();

        // a negative start value means we need to add that amount of delay before the sample
        double startDelay = 0;
        if (start < 0) {
            startDelay = Math.abs(start);
        }
        File file = new File(path);
        String outFile = file.getCanonicalPath() + "_delayed.wav";
        cmd.add(soxBin);
        cmd.add(path);
        cmd.add("-e");
        cmd.add("signed-integer");
        cmd.add("-b");
        cmd.add("16");
        cmd.add(outFile);
        cmd.add("delay");
        cmd.add(mFormatTrim.format(startDelay)); // left channel
        cmd.add(mFormatTrim.format(startDelay)); // right channel


        int rc = execSox(cmd, callback);
        if( rc != 0 ) {
            Log.e(TAG, "delayAudio receieved non-zero return code!");
            outFile = null;
        }

        if (file.exists())
            return outFile;
        else
            return null;

    }

    /**
     * Fade audio file
     * sox <path> outFile fade <type> <fadeInLength> <stopTime> <fadeOutLength>
     * @param path
     * @param type
     * @param fadeInLength specify 0 if no fade in is desired
     * @param stopTime (optional)
     * @param fadeOutLength (optional)
     * @return
     */
    public String fadeAudio(String path, String type, double fadeInLength, double stopTime, double fadeOutLength ) throws IOException {
        checkForInvalidSpaces(path);

        final List<String> curves = Arrays.asList( new String[]{ "q", "h", "t", "l", "p"} );

        if(!curves.contains(type)) {
            Log.e(TAG, "fadeAudio: passed invalid type: " + type);
            return null;
        }

        File file = new File(path);
        String outFile = file.getCanonicalPath() + "_faded.wav";

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);
        cmd.add(path);
        cmd.add(outFile);
        cmd.add("fade");
        cmd.add(type);
        cmd.add(mFormatTrim.format(fadeInLength));
        if(stopTime != -1)
            cmd.add(mFormatTrim.format(stopTime));
        if(fadeOutLength != -1)
            cmd.add(mFormatTrim.format(fadeOutLength));

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            Log.e(TAG, "fadeAudio receieved non-zero return code!");
            outFile = null;
        }

        return outFile;
    }

    /**
     * Combine and mix audio files
     * sox -m -v 1.0 file[0] -v 1.0 file[1] ... -v 1.0 file[n] outFile
     * TODO support passing of volume
     * @param files
     * @return combined and mixed file (null on failure)
     */
    public MediaDesc combineMix(List<MediaDesc> files, MediaDesc outFile) {
        for(MediaDesc file : files){
            checkForInvalidSpaces(file.path);
        }

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);
        cmd.add("-m");

        for(MediaDesc file : files) {
            cmd.add("-v");
            cmd.add(mFormatValue.format(file.audioVolume));
            cmd.add(file.path);
        }

        cmd.add(outFile.path);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            Log.e(TAG, "combineMix receieved non-zero return code!");
            outFile = null;
        }

        return outFile;
    }

    /**
     * Simple combiner
     * sox file[0] file[1] ... file[n] <outFile>
     * @param files
     * @param outFile
     * @return outFile or null on failure
     */
    public MediaDesc combine(List<MediaDesc> files, MediaDesc outFile) {
        for(MediaDesc file : files){
            checkForInvalidSpaces(file.path);
        }
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(soxBin);

        for(MediaDesc file : files) {
            cmd.add("-v");
            cmd.add(mFormatValue.format(file.audioVolume));
            cmd.add(file.path);
        }

        cmd.add(outFile.path);

        int rc = execSox(cmd, callback);
        if(rc != 0) {
            Log.e(TAG, "combine received non-zero return code!");
            outFile = null;
        }

        return outFile;
    }

    public void checkForInvalidSpaces(String path){
        if(path.contains(" ")){
            // not really a null pointer exception, I just dont want to force integrated exception handling
            throw new NullPointerException("Invalid path: file path should not contain spaces");
        }

    }
    /**
     * Takes a seconds.frac value and formats it into:
     * 	hh:mm:ss:ss.frac
     * @param seconds
     */
    public String formatTimePeriod(double seconds) {

        long milliTime = (long)(seconds * 100f);
        Date dateTime = new Date(milliTime);
        return String.format(Locale.US, "%s:%s.%s", dateTime.getHours(),dateTime.getMinutes(),dateTime.getSeconds());
    }


    public int execSox(List<String> cmds, ShellCallback sc){

        //ensure that the arguments are in the correct Locale format
        String soxCommand = "";

        for (String cmd :cmds)
        {
            cmd = String.format(Locale.US, "%s", cmd);

            soxCommand += (cmd);
            soxCommand += (' ');
        }

        Log.v(TAG, soxCommand.toString());


        int exitVal = SoxCommandLib.executeCommand(soxCommand);
        if(exitVal == 0){
            Log.v("SOX", "Sox finished successfully");
        }else{
            Log.v("SOX", "Sox failed");
        }

        sc.processComplete(exitVal);

        return exitVal;
    }



    public int execSox(List<String> cmds){

        //ensure that the arguments are in the correct Locale format
        String soxCommand = "";

        for (String cmd :cmds)
        {
            cmd = String.format(Locale.US, "%s", cmd);

            soxCommand += (cmd);
            soxCommand += (' ');
        }


        int exitVal = SoxCommandLib.executeCommand(soxCommand);
        if(exitVal == 0){
            Log.v("SOX", "Sox finished successfully");
        }else{
            Log.v("SOX", "Sox failed");
        }

        return exitVal;
    }



    public interface ShellCallback
    {
        public void shellOut(String shellLine);

        public void processComplete(int exitValue);
    }

  
    public class MediaDesc implements Cloneable
    {

        public int width = -1;
        public int height = -1;

        public String videoCodec;
        public String videoFps;
        public int videoBitrate = -1;
        public String videoBitStreamFilter;

        public String audioCodec;
        public int audioChannels = -1;
        public int audioBitrate = -1;
        public String audioQuality;
        public float audioVolume = 1.0f;
        public String audioBitStreamFilter;

        public String path;
        public String format;
        public String mimeType;

        public String startTime; //00:00:00 or seconds format
        public String duration; //00:00:00 or seconds format

        public String videoFilter;
        public String audioFilter;

        public String qscale;
        public String aspect;
        public int passCount = 1; //default

        public MediaDesc(String path){
            this.path = path;
        }
        public MediaDesc(){
        }
        public org.ffmpeg.android.MediaDesc clone ()  throws CloneNotSupportedException
        {
            return (org.ffmpeg.android.MediaDesc)super.clone();
        }

        public boolean isImage() {
            return mimeType.startsWith("image");
        }

        public boolean isVideo() {
            return mimeType.startsWith("video");
        }

        public boolean isAudio() {
            return mimeType.startsWith("audio");
        }
    }


}
