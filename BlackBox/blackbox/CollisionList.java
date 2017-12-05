package com.example.sunwoo.blackbox;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CollisionList extends AppCompatActivity {

    private static final String path = "/storage/emulated/0/DCIM/Camera/Collision";

    private ArrayList<Itemlist> aryVideoList;
    private ListArrayAdapter adtVideos;
    private ListView videolist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collisionlist);

        getVideoStore();

        videolist = (ListView) findViewById(R.id.listView_video_collision);
        videolist.setAdapter(adtVideos);

        videolist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CollisionList.this, PlayVideo.class);

                String tv = (String)parent.getAdapter().getItem(position);
                Log.e("Video_path", tv);
                Toast.makeText(CollisionList.this, tv + " 실행", Toast.LENGTH_SHORT).show();

                intent.putExtra("video_path", tv);
                startActivity(intent);
            }
        });
    }

    private void getVideoStore() {
        aryVideoList = new ArrayList<>();
        adtVideos = new ListArrayAdapter(this, R.layout.collisionlist_item, aryVideoList);

        Uri targetUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] filedata = {
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATA
        };

        Cursor cur = getContentResolver().query(targetUri, filedata, null, null, null);

        int id = cur.getColumnIndex(MediaStore.Video.VideoColumns._ID);
        int data = cur.getColumnIndex(MediaStore.Video.VideoColumns.DATA);

        if(cur != null && cur.moveToFirst()) {
            String strImage;

            do {
                strImage = cur.getString(data);
                if(strImage != null && strImage.startsWith(path)) {
                    aryVideoList.add(new Itemlist(cur.getString(data), cur.getInt(id)));
                }
            } while (cur.moveToNext());
        }
    }

    class Itemlist {
        String Video_path;
        int Video_ID;

        public Itemlist (String Video_Path, int Video_ID) {
            this.Video_path = Video_Path;
            this.Video_ID = Video_ID;
        }

        public String getVideoPath() {
            return Video_path;
        }

        public int getVideoID() {
            return Video_ID;
        }
    }

    class ListArrayAdapter extends BaseAdapter {

        private ArrayList<Itemlist> itemlist = null;
        private int layoutID;

        public ListArrayAdapter(Context context, int layoutID, ArrayList<Itemlist> dataSet) {
            this.itemlist = dataSet;
            this.layoutID = layoutID;
        }

        @Override
        public int getCount() {
            return itemlist.size();
        }

        @Override
        public Object getItem(int position) {
            return itemlist.get(position).getVideoPath();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ImageView list_thumbnail;
            TextView list_imagename;

            if(v == null) {
                LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(this.layoutID, null);
            }

            Itemlist item = itemlist.get(position);
            list_thumbnail = (ImageView) v.findViewById(R.id.itemlist_image_video_collision);
            list_imagename = (TextView) v.findViewById(R.id.itemlist_text_video_collision);

            if(item != null) {
                Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(),
                        itemlist.get(position).getVideoID(),
                        MediaStore.Video.Thumbnails.MICRO_KIND, null);
                list_thumbnail.setImageBitmap(bitmap);
                list_imagename.setText(itemlist.get(position).getVideoPath());
            }

            return v;
        }
    }
}
