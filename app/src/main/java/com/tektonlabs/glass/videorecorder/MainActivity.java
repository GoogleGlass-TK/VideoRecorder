package com.tektonlabs.glass.videorecorder;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.*;
import android.widget.VideoView;

import com.facebook.*;
import com.facebook.android.Facebook;
import com.facebook.model.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView)findViewById(R.id.videoView);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // Stop the preview and release the camera.
            // Execute your logic as quickly as possible
            // so the capture happens quickly.

            capture_video();

            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Log.d("s", "------------1--------------");
//        ArrayList<String> voiceResults = getIntent().getExtras()
//                .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
//        Log.d("s", "------------2--------------");
        // Re-acquire the camera and start the preview.
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d("s", "------------1--------------");
//        ArrayList<String> voiceResults = intent.getExtras()
//                .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
//        Log.d("s", "------------2--------------");
//        return 1;
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    private static final int CAPTURE_VIDEO_REQUEST = 1;

    private void capture_video() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, CAPTURE_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            String videoPath = data.getStringExtra("video_file_path"); //CameraManager.EXTRA_VIDEO_FILE_PATH);
            processVideoWhenReady(videoPath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processVideoWhenReady(final String videoPath) {
        final File videoFile = new File(videoPath);
        Log.d("Video file", "The video file ... " + videoFile.getAbsolutePath());
        if (videoFile.exists()) {

            Log.d("Video completed", "The recording video has finished ... " + videoFile.getAbsolutePath());
            videoView.setVideoPath(videoPath);
            videoView.requestFocus();
            videoView.start();
            //shareVideoOnFacebook(videoFile);
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = videoFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath()) {
                // Protect against additional pending events after CLOSE_WRITE is
                // handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = (event == FileObserver.CLOSE_WRITE
                                && affectedFile.equals(videoFile));

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processVideoWhenReady(videoPath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    private void shareVideoOnFacebook(File videoFile) {
        Log.d("V", "in to share");
        Session session = Session.openActiveSession(this, true, null);
        Log.d("V", "session got");

        Bundle params = new Bundle();
        params.putString("message", "This is a test message");
        Log.d("V", "in to share");

        Request.Callback callback = new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                Log.d("h", "Success");
                Log.d("In Response ", "" +response.getGraphObject().getProperty("id"));
            }
        };

        try{
            Request request = Request.newUploadVideoRequest(session, videoFile, callback);
            request.setParameters(params);
            RequestAsyncTask task = new RequestAsyncTask(request);
            task.execute();
//            new Request(session, "me/videos", params, HttpMethod.POST, new Request.Callback() {
//
//                @Override
//                public void onCompleted(Response response) {
//                    Log.d("In Response ", "" +response.getGraphObject().getProperty("id"));
//
//                    JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
//                    String postId = null;
//                    try {
//                        postId = graphResponse.getString("id");
//                        Log.d("In Response id ", ": " + postId);
//                    } catch (JSONException e) {
//                        Log.d("JSON error: ", " " + e.getMessage());
//                    }
//
//                }
//            }).executeAsync();
        }catch (Exception e){
            Log.d("h", "EXCEPTION. Error:" + e.getMessage());
        }
//        new Request(
//                session,
//                "/me/feed",
//                params,
//                HttpMethod.POST,
//                new Request.Callback() {
//            public void onCompleted(Response response) {
//                /* handle the result */
//            }
//        }
//        ).executeAsync();
    }

}
