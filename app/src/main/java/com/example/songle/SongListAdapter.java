package com.example.songle;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

/**
 * Created by Paul Moore on 25-Oct-17.
 * Displays the songs currently saved as a list.
 */

class SongListAdapter extends ArrayAdapter<Song> {
    private final Context context;
    private int currentSongNumber = -1;
    private final List<Song> songs;

    SongListAdapter(Context context, List<Song> songs){
        super(context, R.layout.song_list_item, songs);
        this.context = context;
        this.songs = songs;
        SharedPreference sharedPreference = new SharedPreference(context);
        String strLastSong = sharedPreference.getCurrentSongNumber();
        if (strLastSong != null) {
            currentSongNumber = Integer.parseInt(strLastSong);
        }
    }

    private class ViewHolder {
        TextView songNumberTxt;
        TextView songStatusTxt;
        ImageView songImg;
    }

    @Override
    public int getCount(){
        if (songs != null) return songs.size();
        else return 0;
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

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent){
        ViewHolder holder;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.song_list_item, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.songNumberTxt = convertView.findViewById(R.id.txt_song_number);
        holder.songStatusTxt = convertView.findViewById(R.id.txt_song_status);
        holder.songImg = convertView.findViewById(R.id.imgbtn_music);
        Song song = getItem(position);
        holder.songNumberTxt.setText(song.getNumber());
        holder.songStatusTxt.setText(song.showStatus());

        //make the icon green if the song is completed, orange if incomplete, grey otherwise
        if (song.isSongComplete()){
            holder.songImg.setImageResource(R.drawable.music_note_green);
        } else if (song.isSongIncomplete()) {
            holder.songImg.setImageResource(R.drawable.music_note_orange);
        } else {
            holder.songImg.setImageResource(R.drawable.music_note_grey);
        }

       //show last chosen song as the primary color
        if (currentSongNumber != -1){
            if (getItemId(position) == currentSongNumber) {
                holder.songNumberTxt.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.songStatusTxt.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            }
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
