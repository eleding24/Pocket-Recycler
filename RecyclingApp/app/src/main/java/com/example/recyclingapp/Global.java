package com.example.recyclingapp;

import android.app.Application;
import android.content.SharedPreferences;

public class Global extends Application {
    public static int count = 0;
    public static int getCount(){
        return count;
    }
    public void setCount(int c){
        count = c;
    }
    public void addCount(){
        count++;
        PreConfig.saveTotalInPref(getApplicationContext(), count);
    }

}
