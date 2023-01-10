package com.example.recyclingapp.ui.gallery;

import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.recyclingapp.Global;
import com.example.recyclingapp.R;
import com.example.recyclingapp.ui.home.HomeViewModel;

public class GalleryFragment extends Fragment {

    private int num = 0;

    private GalleryViewModel galleryViewModel;
    TextView counter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //galleryViewModel = ViewModelProviders.of(this).get(GalleryViewModel.class);
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        counter = (TextView) view.findViewById(R.id.tv_count);
        //counter.setText("" + num);
        counter.setText("" + Global.getCount());

        return view;
    }
}
