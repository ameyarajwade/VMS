package com.example.mayur.myapplication;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

public class VideoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String path = "video";

    // TODO: Rename and change types of parameters
    private OnFragmentInteractionListener mListener;
    String mPath;
    VideoView videoView;
    public VideoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1
     * @return A new instance of fragment ImageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoFragment newInstance(String param1) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(path, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPath = getArguments().getString(path);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View RootView = inflater.inflate(R.layout.fragment_video, container, false);
        videoView = (VideoView) RootView.findViewById(R.id.videoview);
        videoView.setVideoPath(mPath);
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if(mListener != null) {
                    mListener.onTimerStartStop(true);
                }
                return true;
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(mListener != null) {
                    mListener.onTimerStartStop(true);
                }

            }
        });
        return RootView;
    }

    public void playvideo() {
        if(mListener != null) {
            mListener.onTimerStartStop(false);
        }
        if(videoView != null) {
            videoView.requestFocus();
            videoView.start();
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onTimerStartStop(boolean start);
    }
}
