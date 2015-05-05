/*
 * Copyright 2015  bohrmeista
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bohrmeista.chan.ui.adapter;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.bohrmeista.chan.ChanApplication;
import org.bohrmeista.chan.R;
import org.bohrmeista.chan.core.model.Post;
import org.bohrmeista.chan.ui.activity.ImageViewActivity;

import java.util.List;

public class ThumbListAdapter implements ListAdapter {
    private List<Post.ImageData> images;
    private ImageViewActivity activity;
    int globalOffset;
    int current = 0;

    public ThumbListAdapter(ImageViewActivity activity, List<Post.ImageData> images, int globalOffset) {
        this.activity = activity;
        this.images = images;
        this.globalOffset = globalOffset;
    }

    public void setImages(List<Post.ImageData> images, int globalOffset) {
        this.images = images;
        this.globalOffset = globalOffset;
    }

    public void setIndex(int index) {
        this.current = index;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
    }

    @Override
    public int getCount() {
        return images.size() <= 1 ? 0 : images.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (i < 0 || i >= images.size())
            return null;
        final ImageView imageView = new ImageView(activity);
        imageView.setMinimumHeight(android.R.dimen.thumbnail_height);
        imageView.setMaxHeight(android.R.dimen.thumbnail_height);
        Post.ImageData imageData = images.get(i);
        final ThumbListAdapter adapter = this;
        // Also use volley for the thumbnails
        ChanApplication.getVolleyImageLoader().get(imageData.thumbUrl, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, R.string.image_preview_failed, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null)
                    imageView.setImageBitmap(response.getBitmap());
            }
        }, imageData.thumbWidth, imageData.thumbHeight);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.activity.showImage(adapter.globalOffset + i);
            }
        });
        return imageView;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public List<Post.ImageData> getImages() {
        return images;
    }
}
