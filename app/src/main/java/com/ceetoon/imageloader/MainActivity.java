package com.ceetoon.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;


import libcore.io.ImageLoader;

public class MainActivity extends AppCompatActivity {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private ImageLoader mImageLoader = null;
    private Bitmap bitmap = null;
    private BaseAdapter mAdapter;
    private boolean mScrollIdel=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridView gv = (GridView) findViewById(R.id.gv);
        mImageLoader = new ImageLoader(MainActivity.this);
        bitmap = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        mAdapter = new BaseAdapter() {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder = null;
                if (convertView == null) {
                    convertView = View.inflate(MainActivity.this,
                            R.layout.item_gridview, null);
                    holder = new ViewHolder();
                    holder.iv = (ImageView) convertView.findViewById(R.id.iv);
                    convertView.setTag(holder);
                }
                holder = (ViewHolder) convertView.getTag();
                String url = MyImages.SAMPLEPICS[position];
                holder.iv.setImageBitmap(bitmap);
                mImageLoader.display(holder.iv, url);
                return convertView;
            }

            @Override
            public long getItemId(int position) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Object getItem(int position) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getCount() {
                return MyImages.SAMPLEPICS.length;
            }
        };
        gv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    mScrollIdel = true;
                    //mAdapter.notifyDataSetChanged();
                } else {
                    mScrollIdel = false;
                }

            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

            }
        });
        gv.setAdapter(mAdapter);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }



    static class ViewHolder {
        ImageView iv;
    }
}
