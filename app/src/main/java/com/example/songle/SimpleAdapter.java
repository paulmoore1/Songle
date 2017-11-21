package com.example.songle;

/**
 * Created by Paul on 21/11/2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Suleiman on 03/02/17.
 */

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleItemVH> {

    //  Data
    private List<Song> songs = new ArrayList<>();
    private SharedPreference sharedPreference;
    private Context context;

    public SimpleAdapter(Context context) {
        this.context = context;
        this.sharedPreference = new SharedPreference(context);
        prepareSongs();
    }

    private void prepareSongs() {
        songs = sharedPreference.getAllSongs();
    }

    @Override
    public SimpleItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_simplevh, parent, false);

        return new SimpleItemVH(v);
    }

    @Override
    public void onBindViewHolder(SimpleItemVH holder, int position) {
        Song song = songs.get(position);

        holder.txtTitle.setText(song.getTitle());
        holder.txtDesc.setText(song.getLink());
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    protected static class SimpleItemVH extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDesc;

        public SimpleItemVH(View itemView) {
            super(itemView);

            txtTitle = (TextView) itemView.findViewById(R.id.item_simplevh_txttitle);
            txtDesc = (TextView) itemView.findViewById(R.id.item_simplevh_txtdescription);
        }
    }
}