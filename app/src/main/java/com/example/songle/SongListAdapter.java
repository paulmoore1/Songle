package com.example.songle;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Paul Moore on 25-Oct-17.
 */

public class SongListAdapter extends ArrayAdapter<Song> {

    private Context context;
    List<Song> songs;
    SharedPreference sharedPreference;

    public SongListAdapter(Context context, List<Song> songs){
        super(context, R.layout.song_list_item, songs);
        this.context = context;
        this.songs = songs;
        sharedPreference = new SharedPreference();
    }

    private class ViewHolder {
        TextView songNumberTxt;
        TextView songStatusTxt;
        ImageView songImg;
    }

    @Override
    public int getCount(){
        return songs.size();
    }

    @Override
    public Song getItem(int position){
        return songs.get(position);
    }

    @Override
    public long getItemId(int position){
        Song song = songs.get(position);
        int number = Integer.parseInt(song.getNumber());
        return (long) number;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        ViewHolder holder;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.song_list_item, null);
            holder = new ViewHolder();
            holder.songNumberTxt = (TextView) convertView.findViewById(R.id.txt_song_number);
            holder.songStatusTxt = (TextView) convertView.findViewById(R.id.txt_song_status);
            holder.songImg = (ImageView) convertView.findViewById(R.id.imgbtn_music);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();

        }
        Song song = getItem(position);
        holder.songNumberTxt.setText(song.getNumber());
        holder.songStatusTxt.setText(song.showStatus());

        //make the icon green if the song is completed, grey otherwise
        if (song.isSongComplete()){
            holder.songImg.setImageResource(R.drawable.music_note_green);
        } else {
            holder.songImg.setImageResource(R.drawable.music_note_grey);
        }


        return convertView;

    }

    @Override
    public void add(Song song){
        super.add(song);
        songs.add(song);
        notifyDataSetChanged();
    }

    @Override
    public void remove(Song song){
        super.remove(song);
        songs.remove(song);
        notifyDataSetChanged();
    }


}
