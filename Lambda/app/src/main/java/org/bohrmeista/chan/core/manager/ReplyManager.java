/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 * Copyright (C) 2014  bohrmeista
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
package org.bohrmeista.chan.core.manager;

import android.content.Context;
import android.content.Intent;

import org.bohrmeista.chan.ChanApplication;
import org.bohrmeista.chan.chan.ChanUrls;
import org.bohrmeista.chan.core.model.Reply;
import org.bohrmeista.chan.core.model.SavedReply;
import org.bohrmeista.chan.ui.activity.ImagePickActivity;
import org.bohrmeista.chan.utils.Logger;
import org.bohrmeista.chan.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.boye.httpclientandroidlib.Consts;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.config.RequestConfig;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.protocol.HttpClientContext;
import ch.boye.httpclientandroidlib.entity.ContentType;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntityBuilder;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.util.EntityUtils;

/**
 * To send an reply to 4chan.
 */
public class ReplyManager {
    private static final String TAG = "ReplyManager";

    private static final Pattern postURIPattern = Pattern.compile("/(\\d+)\\.html#(\\d+)$");
    private static final Pattern threadURIPattern = Pattern.compile("/(\\d+)\\.html$");
    private static final int POST_TIMEOUT = 10000;

    private static final ContentType TEXT_UTF_8 = ContentType.create(
            "text/plain", Consts.UTF_8);

    private final Context context;
    private Reply draft;
    private FileListener fileListener;
    private final Random random = new Random();

    public ReplyManager(Context context) {
        this.context = context;
        draft = new Reply();
    }

    /**
     * Clear the draft
     */
    public void removeReplyDraft() {
        draft = new Reply();
    }

    /**
     * Set an reply draft.
     *
     * @param value the draft to save.
     */
    public void setReplyDraft(Reply value) {
        draft = value;
    }

    /**
     * Gets the saved reply draft.
     *
     * @return the saved draft or an empty draft.
     */
    public Reply getReplyDraft() {
        return draft;
    }

    /**
     * Add an quote to the comment field. Looks like >>123456789\n
     *
     * @param no the raw no to quote to.
     */
    public void quote(int no) {
        String textToInsert = ">>" + no + "\n";
        draft.comment = new StringBuilder(draft.comment).insert(draft.cursorPosition, textToInsert).toString();
        draft.cursorPosition += textToInsert.length();
    }

    public void quoteInline(int no, String text) {
        String textToInsert = ">>" + no + "\n";
        String[] lines = text.split("\n+");
        for (String line : lines) {
            textToInsert += ">" + line + "\n";
        }

        draft.comment = new StringBuilder(draft.comment).insert(draft.cursorPosition, textToInsert).toString();
        draft.cursorPosition += textToInsert.length();
    }

    /**
     * Pick an file. Starts up the ImagePickActivity.
     *
     * @param listener FileListener to listen on.
     */
    public void pickFile(FileListener listener) {
        fileListener = listener;

        Intent intent = new Intent(context, ImagePickActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Called from ImagePickActivity, sends onFileLoading to the fileListener.
     */
    public void _onPickedFileLoading() {
        if (fileListener != null) {
            fileListener.onFileLoading();
        }
    }

    /**
     * Called from ImagePickActivity. Sends the file to the listening
     * fileListener, and deletes the fileListener.
     */
    public void _onPickedFile(String name, File file) {
        if (fileListener != null) {
            fileListener.onFile(name, file);
        }
        fileListener = null;
    }

    /**
     * Delete the fileListener.
     */
    public void removeFileListener() {
        fileListener = null;
    }

    public static abstract class FileListener {
        public abstract void onFile(String name, File file);

        public abstract void onFileLoading();
    }

    public static interface PassListener {
        public void onResponse(PassResponse response);
    }

    public static class PassResponse {
        public boolean isError = false;
        public boolean unknownError = false;
        public String responseData = "";
        public String message = "";
        public String passId;
    }

    public void sendDelete(final SavedReply reply, boolean onlyImageDelete, final DeleteListener listener) {
        Logger.i(TAG, "Sending delete request: " + reply.board + ", " + reply.no);

        HttpPost httpPost = new HttpPost(ChanUrls.getDeleteUrl());
        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        entity.addTextBody("delete", "Delete");
        entity.addTextBody("delete_" + reply.no, "");
        entity.addTextBody("reason", "");
        entity.addTextBody("board", reply.board);
        entity.addTextBody("password", reply.password);

        if (onlyImageDelete) {
            entity.addTextBody("file", "on");
        }

        httpPost.setEntity(entity.build());

        sendHttpPost(httpPost, new HttpPostSendListener() {
            @Override
            public void onResponse(String responseString, HttpClient client, HttpResponse response, String lastURI) {
                DeleteResponse e = new DeleteResponse();

                if (responseString == null) {
                    e.isNetworkError = true;
                } else {
                    // TODO: We can't error check here. API returns 200 with no status indication in body.
                    e.responseData = responseString;
                    e.isSuccessful = true;
                }

                listener.onResponse(e);
            }
        });
    }

    public static interface DeleteListener {
        public void onResponse(DeleteResponse response);
    }

    public static class DeleteResponse {
        public boolean isNetworkError = false;
        public boolean isUserError = false;
        public boolean isInvalidPassword = false;
        public boolean isTooSoonError = false;
        public boolean isTooOldError = false;
        public boolean isSuccessful = false;
        public String responseData = "";
    }

    /**
     * Send an reply off to the server.
     *
     * @param reply    The reply object with all data needed, like captcha and the
     *                 file.
     * @param listener The listener, after server response.
     */
    public void sendReply(final Reply reply, final ReplyListener listener) {
        Logger.i(TAG, "Sending reply request: " + reply.board + ", " + reply.resto);

        HttpPost httpPost = new HttpPost(ChanUrls.getReplyUrl());
        httpPost.addHeader("Referer", reply.resto > 0 ? ChanUrls.getThreadUrlDesktop(reply.board, reply.resto)
                                                      : ChanUrls.getBoardUrlDesktop(reply.board));

        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        entity.addTextBody("name", reply.name, TEXT_UTF_8);
        entity.addTextBody("email", reply.email, TEXT_UTF_8);
        entity.addTextBody("subject", reply.subject, TEXT_UTF_8);
        entity.addTextBody("body", reply.comment, TEXT_UTF_8);
        entity.addTextBody("post", reply.resto == 0 ? "New Topic" : "New Reply");
        entity.addTextBody("board", reply.board);

        if (reply.resto >= 0) {
            entity.addTextBody("thread", Integer.toString(reply.resto));
        }

        if (reply.spoilerImage) {
            entity.addTextBody("spoiler", "on");
        }

        reply.password = Long.toHexString(random.nextLong());
        entity.addTextBody("password", reply.password);

        // TODO: Review property
        /*if (reply.usePass) {
            httpPost.addHeader("Cookie", "pass_id=" + reply.passId);
        }*/

        if (reply.file != null) {
            entity.addBinaryBody("file", reply.file, ContentType.APPLICATION_OCTET_STREAM, reply.fileName);
        }

        httpPost.setEntity(entity.build());

        sendHttpPost(httpPost, new HttpPostSendListener() {
            @Override
            public void onResponse(String responseString, HttpClient client, HttpResponse response, String lastURI) {
                ReplyResponse e = new ReplyResponse();

                if (responseString == null) {
                    e.isNetworkError = true;
                    listener.onResponse(e);
                    return;
                }

                int status = response.getStatusLine().getStatusCode();

                e.responseData = responseString;
                if (status == 200) {
                    e.isSuccessful = true;
                } else {
                    e.isUserError = true;
                }

                if (e.isSuccessful) {
                    Matcher postMatcher = postURIPattern.matcher(lastURI);
                    Matcher threadMatcher = threadURIPattern.matcher(lastURI);

                    int threadNo = -1;
                    int no = -1;
                    if (postMatcher.find()) {
                        try {
                            threadNo = Integer.parseInt(postMatcher.group(1));
                            no = Integer.parseInt(postMatcher.group(2));
                        } catch (NumberFormatException err) {
                            err.printStackTrace();
                        }
                    } else if (threadMatcher.find()) {
                        try {
                            threadNo = Integer.parseInt(threadMatcher.group(1));
                            no = threadNo;
                        } catch (NumberFormatException err) {
                            err.printStackTrace();
                        }
                    }

                    if (threadNo >= 0 && no >= 0) {
                        SavedReply savedReply = new SavedReply();
                        savedReply.board = reply.board;
                        savedReply.no = no;
                        savedReply.password = reply.password;

                        ChanApplication.getDatabaseManager().saveReply(savedReply);

                        e.threadNo = threadNo;
                        e.no = no;
                    } else {
                        Logger.w(TAG, "No thread & no in the response");
                    }
                }

                listener.onResponse(e);
            }
        });
    }

    public static interface ReplyListener {
        public void onResponse(ReplyResponse response);
    }

    public static class ReplyResponse {
        /**
         * No response from server.
         */
        public boolean isNetworkError = false;

        /**
         * Some user error, like no file or captcha wrong.
         */
        public boolean isUserError = false;

        /**
         * The userError was an fileError
         */
        public boolean isFileError = false;

        /**
         * The userError was an captchaError
         */
        public boolean isCaptchaError = false;

        /**
         * Received 'post successful'
         */
        public boolean isSuccessful = false;

        /**
         * Raw html from the response. Used to set html in an WebView to the
         * client, when the error was not recognized by Clover.
         */
        public String responseData = "";

        /**
         * The no the post has
         */
        public int no = -1;

        /**
         * The thread no the post has
         */
        public int threadNo = -1;
    }

    /**
     * Async task to send an reply to the server. Uses HttpClient. Since Android
     * 4.4 there is an updated version of HttpClient, 4.2, given with Android.
     * However, that version causes problems with file uploading. Version 4.3 of
     * HttpClient has been given with a library, that has another namespace:
     * ch.boye.httpclientandroidlib This lib also has some fixes/improvements of
     * HttpClient for Android.
     */
    private void sendHttpPost(final HttpPost post, final HttpPostSendListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RequestConfig.Builder requestBuilder = RequestConfig.custom();
                requestBuilder = requestBuilder.setConnectTimeout(POST_TIMEOUT);
                requestBuilder = requestBuilder.setConnectionRequestTimeout(POST_TIMEOUT);

                HttpClientBuilder httpBuilder = HttpClientBuilder.create();
                httpBuilder.setDefaultRequestConfig(requestBuilder.build());
                final CloseableHttpClient client = httpBuilder.build();
                try {
                    final HttpClientContext httpClientContext = new HttpClientContext();
                    final CloseableHttpResponse response = client.execute(post, httpClientContext);
                    List<URI> allURIs = httpClientContext.getRedirectLocations();
                    final String lastURI = allURIs == null ? null : allURIs.get(allURIs.size() - 1).toASCIIString();
                    final String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (lastURI != null)
                                listener.onResponse(responseString, client, response, lastURI);
                            else
                                listener.onResponse(null, client, null, null);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResponse(null, client, null, null);
                        }
                    });
                } finally {
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private static interface HttpPostSendListener {
        public void onResponse(String responseString, HttpClient client, HttpResponse response, String lastURI);
    }
}
