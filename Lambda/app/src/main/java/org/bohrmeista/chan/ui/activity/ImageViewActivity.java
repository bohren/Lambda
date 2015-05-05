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
package org.bohrmeista.chan.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.lucasr.twowayview.TwoWayView;
import org.bohrmeista.chan.R;
import org.bohrmeista.chan.chan.ImageSearch;
import org.bohrmeista.chan.core.ChanPreferences;
import org.bohrmeista.chan.core.manager.ThreadManager;
import org.bohrmeista.chan.core.model.Post;
import org.bohrmeista.chan.ui.adapter.ImageViewAdapter;
import org.bohrmeista.chan.ui.adapter.PostAdapter;
import org.bohrmeista.chan.ui.adapter.ThumbListAdapter;
import org.bohrmeista.chan.ui.fragment.ImageViewFragment;
import org.bohrmeista.chan.utils.ImageSaver;
import org.bohrmeista.chan.utils.Logger;
import org.bohrmeista.chan.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * An fragment pager that contains images. Call setPosts first, and then start
 * the activity with startActivity()
 */
public class ImageViewActivity extends Activity implements ViewPager.OnPageChangeListener {
    private static final String TAG = "ImageViewActivity";

    private static PostAdapter postAdapter;
    private static int selectedId = -1;
    private static ThreadManager threadManagerStatic;

    private ViewPager viewPager;
    private ImageViewAdapter imageAdapter;
    private TwoWayView thumbList;
    private ThumbListAdapter thumbListAdapter;
    private ProgressBar progressBar;
    private ThreadManager threadManager;

    private int currentPosition;

    /**
     * Set the posts to show
     *
     * @param adapter  the adapter to get image data from
     * @param selected the no that the user clicked on
     */
    public static void setAdapter(PostAdapter adapter, int selected, ThreadManager threadManager) {
        postAdapter = adapter;
        selectedId = selected;
        threadManagerStatic = threadManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

        super.onCreate(savedInstanceState);

        if (postAdapter == null) {
            Logger.e(TAG, "Posts in ImageViewActivity was null");
            finish();
            return;
        }

        threadManager = threadManagerStatic;

        ThemeHelper.setTheme(this);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar_no_bg));
        progressBar.setIndeterminate(false);
        progressBar.setMax(1000000);

        final FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(progressBar);

        progressBar.post(new Runnable() {
            @Override
            public void run() {
                View contentView = decorView.findViewById(android.R.id.content);
                progressBar.setY(contentView.getY() - progressBar.getHeight() / 2);
            }
        });

        // Get the posts with images
        ArrayList<Post> imagePosts = new ArrayList<>();
        for (Post post : postAdapter.getList()) {
            if (post.images.size() > 0) {
                imagePosts.add(post);
            }
        }

        // Setup our pages and adapter
        setContentView(R.layout.image_pager);
        viewPager = (ViewPager) findViewById(R.id.image_pager);
        imageAdapter = new ImageViewAdapter(getFragmentManager(), this);
        imageAdapter.setList(imagePosts);
        viewPager.setAdapter(imageAdapter);
        viewPager.setOnPageChangeListener(this);

        // Select the right image
        int imageIndex = 0;
        for (Post post : imagePosts) {
            if (post.no == selectedId) {
                viewPager.setCurrentItem(imageIndex);
                onPageSelected(imageIndex);

                thumbList = (TwoWayView) findViewById(R.id.thumbList);
                thumbListAdapter = new ThumbListAdapter(this, post.images, imageIndex);
                thumbList.setAdapter(thumbListAdapter);
                thumbList.setVisibility(post.images.size() <= 1 ? View.GONE : View.VISIBLE);
                return;
            }
            imageIndex += post.images.size();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Avoid things like out of sync, since this is an activity.
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        postAdapter = null;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        currentPosition = position;

        for (int i = -1; i <= 1; i++) {
            ImageViewFragment fragment = getFragment(position + i);
            if (fragment != null) {
                fragment.onDeselected();
            }
        }

        ImageViewFragment fragment = getFragment(currentPosition);
        if (fragment != null) {
            fragment.onSelected(imageAdapter, position);
        }

        ImageViewAdapter.PostPosition postPosition = imageAdapter.imageToPostPosition(position);
        if (postAdapter != null && !threadManager.arePostRepliesOpen()) {
            postAdapter.scrollToPost(postPosition.post.no);
        }

        if (thumbListAdapter != null) {
            if (thumbListAdapter.getImages() != postPosition.post.images) {
                if (postPosition.post.images.size() <= 1) {
                    thumbList.setVisibility(View.GONE);
                } else {
                    thumbListAdapter.setImages(postPosition.post.images, position - postPosition.position);
                    thumbList.setAdapter(thumbListAdapter);
                    thumbList.setVisibility(View.VISIBLE);
                }
                thumbList.getParent().requestLayout();
            }
            thumbListAdapter.setIndex(postPosition.position);
        }
    }

    public void invalidateActionBar() {
        invalidateOptionsMenu();
    }

    public void updateActionBarIfSelected(ImageViewFragment targetFragment) {
        ImageViewFragment fragment = getFragment(currentPosition);
        if (fragment != null && fragment == targetFragment) {
            fragment.onSelected(imageAdapter, currentPosition);
        }
    }

    public void setProgressBar(long current, long total, boolean done) {
        if (done) {
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress((int) (((double) current / total) * progressBar.getMax()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_download_album:
                List<ImageSaver.DownloadPair> list = new ArrayList<>();

                String name = "downloaded";
                String filename;
                for (Post post : imageAdapter.getList()) {
                    if (post.images.size() == 0)
                        continue;
                    Post.ImageData image = post.images.get(0);
                    filename = (ChanPreferences.getImageSaveOriginalFilename() ? post.tim : image.originalFilename) + "." + image.ext;
                    list.add(new ImageSaver.DownloadPair(image.url, filename));

                    name = post.board + "_" + post.resto;
                }

                if (list.size() > 0) {
                    if (!TextUtils.isEmpty(name)) {
                        ImageSaver.getInstance().saveAll(this, name, list);
                    }
                }

                return true;
            default:
                ImageViewFragment fragment = getFragment(currentPosition);
                if (fragment != null) {
                    fragment.customOnOptionsItemSelected(item);
                }

                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_view, menu);

        MenuItem imageSearch = menu.findItem(R.id.action_image_search);
        SubMenu subMenu = imageSearch.getSubMenu();
        for (ImageSearch engine : ImageSearch.engines) {
            subMenu.add(Menu.NONE, engine.getId(), Menu.NONE, engine.getName());
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ImageViewFragment fragment = getFragment(currentPosition);
        if (fragment != null) {
            fragment.onPrepareOptionsMenu(menu);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private ImageViewFragment getFragment(int i) {
        if (i >= 0 && i < imageAdapter.getCount()) {
            Object o = imageAdapter.instantiateItem(viewPager, i);
            if (o instanceof ImageViewFragment) {
                return (ImageViewFragment) o;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void showImage(int i) {
        viewPager.setCurrentItem(i);
    }
}
