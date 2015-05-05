/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;

import org.bohrmeista.chan.core.model.Post;
import org.bohrmeista.chan.ui.activity.ImageViewActivity;
import org.bohrmeista.chan.ui.fragment.ImageViewFragment;

import java.util.ArrayList;
import java.util.List;

public class ImageViewAdapter extends FragmentStatePagerAdapter {
    private final ImageViewActivity activity;
    private final ArrayList<Post> postList = new ArrayList<>();
    private int total_images = 0;

    public ImageViewAdapter(FragmentManager fragmentManager, ImageViewActivity activity) {
        super(fragmentManager);
        this.activity = activity;
    }

    public static class PostPosition {
        public PostPosition(Post post, int imagePosition) {
            this.post = post;
            this.position = imagePosition;
        }

        public Post post;
        public int position;
    }

    public PostPosition imageToPostPosition(int imagePosition) {
        if (imagePosition < 0)
            return null;
        for (Post post : postList) {
            if (imagePosition < post.images.size()) {
                return new PostPosition(post, imagePosition);
            } else {
                imagePosition -= post.images.size();
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return total_images;
    }

    @Override
    public Fragment getItem(int imagePosition) {
        PostPosition postPosition = imageToPostPosition(imagePosition);
        if (postPosition == null)
            return null;
        return ImageViewFragment.newInstance(postPosition.post, postPosition.position, activity);
    }

    public Post getPostFromImagePosition(int imagePosition) {
        if (imagePosition < 0 || imagePosition >= getCount())
            return null;
        PostPosition postPosition = imageToPostPosition(imagePosition);
        if (postPosition == null)
            return null;
        return postPosition.post;
    }

    public void setList(ArrayList<Post> list) {
        postList.clear();
        postList.addAll(list);
        total_images = 0;
        for (Post post : postList)
            total_images += post.images.size();

        notifyDataSetChanged();
    }

    public List<Post> getList() {
        return postList;
    }
}
