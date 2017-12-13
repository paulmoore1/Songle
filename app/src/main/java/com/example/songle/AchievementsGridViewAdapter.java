package com.example.songle;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Paul on 12/12/2017.
 * This along with the AchievementsListFragment displays the user's achievements in the game as a grid
 */

class AchievementsGridViewAdapter extends BaseAdapter {
    private final Context mContext;
    private final String[] gridViewString;
    private final int[] gridViewImageId;

    AchievementsGridViewAdapter(Context context, String[] gridViewString, int[] gridViewImageId){
        mContext = context;
        this.gridViewString  =gridViewString;
        this.gridViewImageId = gridViewImageId;
    }

    @Override
    public int getCount(){
        return gridViewString.length;
    }

    @Override
    public Object getItem(int i){
        if (gridViewString != null && i < gridViewString.length){
            return gridViewString[i];
        } else return null;
    }

    @Override
    public long getItemId(int i){
        if (gridViewImageId != null && i < gridViewImageId.length){
            return gridViewImageId[i];
        } else return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent){
        View view;
        LayoutInflater inflater = (LayoutInflater) mContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null){//if it's not recycled, initialize some attribute
            view = inflater.inflate(R.layout.achievements_gridview_item, null);
        } else {
            view = convertView;
        }
        TextView textViewAndroid = view.findViewById(R.id.android_gridview_text);
        ImageView imageViewAndroid = view.findViewById(R.id.android_gridview_image);
        textViewAndroid.setText(gridViewString[i]);
        imageViewAndroid.setImageResource(gridViewImageId[i]);
        return view;
    }
}
