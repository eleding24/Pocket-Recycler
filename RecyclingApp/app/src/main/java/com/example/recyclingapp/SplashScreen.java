package com.example.recyclingapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;



public class SplashScreen extends AppCompatActivity {
    Global globalClass;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalClass = (Global) getApplicationContext();
        SharedPreferences pref = getSharedPreferences("MyUserPrefs", Context.MODE_PRIVATE);
        globalClass.setCount(pref.getInt("pref_total_key", Context.MODE_PRIVATE));
        startActivity(new Intent(this,NavigationDrawerActivity.class));
        finish();
    }
}
