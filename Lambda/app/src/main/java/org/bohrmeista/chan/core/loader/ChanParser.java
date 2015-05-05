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
package org.bohrmeista.chan.core.loader;


import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import org.bohrmeista.chan.ChanApplication;
import org.bohrmeista.chan.R;
import org.bohrmeista.chan.chan.ChanUrls;
import org.bohrmeista.chan.core.ChanPreferences;
import org.bohrmeista.chan.core.model.Post;
import org.bohrmeista.chan.core.model.PostLinkable;
import org.bohrmeista.chan.utils.ThemeHelper;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChanParser {
    private static final Pattern colorPattern = Pattern.compile("color:#([0-9a-fA-F]*)");
    private static final Pattern postLinkHrefPattern = Pattern.compile("/(\\w+)/res/(\\d+)\\.html#(\\d+)");
    private static final Pattern boardLinkHrefPattern = Pattern.compile("/(\\w+)/index\\.html");

    private static ChanParser instance = new ChanParser();

    public static ChanParser getInstance() {
        return instance;
    }

    public ChanParser() {
    }

    public void parse(Post post) {
        try {
            if (!TextUtils.isEmpty(post.name)) {
                post.name = Parser.unescapeEntities(post.name, false);
            }

            if (!TextUtils.isEmpty(post.subject)) {
                post.subject = Parser.unescapeEntities(post.subject, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!post.parsedSpans) {
            TypedArray ta = ThemeHelper.getInstance().getThemedContext().obtainStyledAttributes(null, R.styleable.PostView, R.attr.post_style, 0);
            post.parsedSpans = true;
            parseSpans(post, ta);
            ta.recycle();
        }

        if (post.rawComment != null) {
            post.comment = parseComment(post, post.rawComment);
        }
    }

    private void parseSpans(Post post, TypedArray ta) {
        boolean anonymize = ChanPreferences.getAnonymize();
        boolean anonymizeIds = ChanPreferences.getAnonymizeIds();

        if (anonymize) {
            post.name = ChanApplication.getInstance().getString(R.string.default_name);
            post.tripcode = "";
        }

        if (anonymizeIds) {
            post.id = "";
        }

        int detailSize = ta.getDimensionPixelSize(R.styleable.PostView_detail_size, 0);

        if (!TextUtils.isEmpty(post.subject)) {
            post.subjectSpan = new SpannableString(post.subject);
            post.subjectSpan.setSpan(new ForegroundColorSpan(ta.getColor(R.styleable.PostView_subject_color, 0)), 0, post.subjectSpan.length(), 0);
        }

        if (!TextUtils.isEmpty(post.name)) {
            post.nameSpan = new SpannableString(post.name);
            post.nameSpan.setSpan(new ForegroundColorSpan(ta.getColor(R.styleable.PostView_name_color, 0)), 0, post.nameSpan.length(), 0);
        }

        if (!TextUtils.isEmpty(post.tripcode)) {
            post.tripcodeSpan = new SpannableString(post.tripcode);
            post.tripcodeSpan.setSpan(new ForegroundColorSpan(ta.getColor(R.styleable.PostView_name_color, 0)), 0, post.tripcodeSpan.length(), 0);
            post.tripcodeSpan.setSpan(new AbsoluteSizeSpan(detailSize), 0, post.tripcodeSpan.length(), 0);
        }

        if (!TextUtils.isEmpty(post.id)) {
            post.idSpan = new SpannableString("  ID: " + post.id + "  ");

            // Stolen from the 4chan extension
            int hash = post.id.hashCode();

            int r = (hash >> 24) & 0xff;
            int g = (hash >> 16) & 0xff;
            int b = (hash >> 8) & 0xff;

            int idColor = (0xff << 24) + (r << 16) + (g << 8) + b;
            boolean lightColor = (r * 0.299f) + (g * 0.587f) + (b * 0.114f) > 125f;
            int idBgColor = lightColor ? ta.getColor(R.styleable.PostView_id_background_light, 0) : ta.getColor(R.styleable.PostView_id_background_dark, 0);

            post.idSpan.setSpan(new ForegroundColorSpan(idColor), 0, post.idSpan.length(), 0);
            post.idSpan.setSpan(new BackgroundColorSpan(idBgColor), 0, post.idSpan.length(), 0);
            post.idSpan.setSpan(new AbsoluteSizeSpan(detailSize), 0, post.idSpan.length(), 0);
        }

        if (!TextUtils.isEmpty(post.capcode)) {
            post.capcodeSpan = new SpannableString("Capcode: " + post.capcode);
            post.capcodeSpan.setSpan(new ForegroundColorSpan(ta.getColor(R.styleable.PostView_capcode_color, 0)), 0, post.capcodeSpan.length(), 0);
            post.capcodeSpan.setSpan(new AbsoluteSizeSpan(detailSize), 0, post.capcodeSpan.length(), 0);
        }

        post.nameTripcodeIdCapcodeSpan = new SpannableString("");
        if (post.nameSpan != null) {
            post.nameTripcodeIdCapcodeSpan = TextUtils.concat(post.nameTripcodeIdCapcodeSpan, post.nameSpan, " ");
        }

        if (post.tripcodeSpan != null) {
            post.nameTripcodeIdCapcodeSpan = TextUtils.concat(post.nameTripcodeIdCapcodeSpan, post.tripcodeSpan, " ");
        }

        if (post.idSpan != null) {
            post.nameTripcodeIdCapcodeSpan = TextUtils.concat(post.nameTripcodeIdCapcodeSpan, post.idSpan, " ");
        }

        if (post.capcodeSpan != null) {
            post.nameTripcodeIdCapcodeSpan = TextUtils.concat(post.nameTripcodeIdCapcodeSpan, post.capcodeSpan, " ");
        }
    }

    private CharSequence parseComment(Post post, String commentRaw) {
        CharSequence total = new SpannableString("");

        try {
            String commenttemp = commentRaw.replace("<wbr>", "");

            String comment = commenttemp.replaceAll("<p class=\"body-line (ltr|rtl|empty) (?:quote)?\">(.*?)</p>", "$2<br>");

            Document document = Jsoup.parseBodyFragment(comment);

            List<Node> nodes = document.body().childNodes();

            for (Node node : nodes) {
                CharSequence nodeParsed = parseNode(post, node);
                if (nodeParsed != null) {
                    total = TextUtils.concat(total, nodeParsed);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return total;
    }

    private CharSequence parseNode(Post post, Node node) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).text();
            SpannableString spannable = new SpannableString(text);

            detectLinks(post, text, spannable);

            return spannable;
        }

        switch (node.nodeName()) {
            case "br": {
                return "\n";
            }
            case "span": {
                Element span = (Element) node;

                SpannableString spannable = new SpannableString(span.text());

                if (span.classNames().size() != 1) {
                    spannable.setSpan(new ForegroundColorSpan(ThemeHelper.getInstance().getInlineQuoteColor()), 0, spannable.length(), 0);
                    detectLinks(post, span.text(), spannable);
                    return spannable;
                }

                String spanClass = span.classNames().iterator().next();
                switch (spanClass) {
                    case "quote": {
                        spannable.setSpan(new ForegroundColorSpan(ThemeHelper.getInstance().getInlineQuoteColor()), 0, spannable.length(), 0);
                        detectLinks(post, span.text(), spannable);
                        break;
                    }
                    case "heading": {
                        spannable.setSpan(new ForegroundColorSpan(ThemeHelper.getInstance().getQuoteColor()), 0, spannable.length(), 0);
                        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
                        break;
                    }
                    case "spoiler": {
                        PostLinkable pl = new PostLinkable(post, span.text(), span.text(), PostLinkable.Type.SPOILER);
                        spannable.setSpan(pl, 0, spannable.length(), 0);
                        post.linkables.add(pl);
                        break;
                    }
                    case "deadlink": {
                        spannable.setSpan(new ForegroundColorSpan(ThemeHelper.getInstance().getQuoteColor()), 0, spannable.length(), 0);
                        spannable.setSpan(new StrikethroughSpan(), 0, spannable.length(), 0);
                        break;
                    }
                }

                return spannable;
            }
            case "strong": {
                Element strong = (Element) node;

                SpannableString bold = new SpannableString(strong.text());
                bold.setSpan(new StyleSpan(Typeface.BOLD), 0, bold.length(), 0);

                return bold;
            }
            case "a": {
                CharSequence anchor = parseAnchor(post, (Element) node);
                if (anchor != null) {
                    return anchor;
                } else {
                    return ((Element) node).text();
                }
            }
            case "s": {
                Element em = (Element) node;

                SpannableString strikethrough = new SpannableString(em.text());
                strikethrough.setSpan(new StrikethroughSpan(), 0, strikethrough.length(), 0);

                return strikethrough;
            }
            case "pre": {
                Element pre = (Element) node;

                Set<String> classes = pre.classNames();
                if (classes.contains("prettyprint")) {
                    String text = getNodeText(pre);
                    SpannableString monospace = new SpannableString(text);
                    monospace.setSpan(new TypefaceSpan("monospace"), 0, monospace.length(), 0);
                    monospace.setSpan(new AbsoluteSizeSpan(ThemeHelper.getInstance().getCodeTagSize()), 0, monospace.length(), 0);
                    return monospace;
                } else {
                    return pre.text();
                }
            }
            case "em": {
                Element em = (Element) node;

                SpannableString italic = new SpannableString(em.text());
                italic.setSpan(new StyleSpan(Typeface.ITALIC), 0, italic.length(), 0);

                return italic;
            }
            default: {
                // Unknown tag, add the inner part
                if (node instanceof Element) {
                    return ((Element) node).text();
                } else {
                    return null;
                }
            }
        }
    }

    private CharSequence parseAnchor(Post post, Element anchor) {
        String href = anchor.attr("href");

        PostLinkable.Type t = null;
        String key = null;
        Object value = null;

        Matcher postLinkMatcher = postLinkHrefPattern.matcher(href);
        Matcher boardLinkMatcher = boardLinkHrefPattern.matcher(href);

        if (anchor.text().startsWith(">>") && postLinkMatcher.find()) {
            String board = postLinkMatcher.group(1);
            int threadId = Integer.parseInt(postLinkMatcher.group(2));
            int postId = Integer.parseInt(postLinkMatcher.group(3));
            if (threadId != post.resto || !board.equals(post.board)) {
                // link to another thread
                PostLinkable.ThreadLink threadLink = new PostLinkable.ThreadLink(board, threadId, postId);
                t = PostLinkable.Type.THREAD;
                key = anchor.text() + " \u2192"; // arrow to the right
                value = threadLink;
            } else {
                // normal quote
                t = PostLinkable.Type.QUOTE;
                key = anchor.text();
                value = postId;
                post.repliesTo.add(postId);

                // Append OP when its a reply to OP
                if (postId == post.resto) {
                    key += " (OP)";
                }

                // Append You when it's a reply to an saved reply
                // todo synchronized
                if (ChanApplication.getDatabaseManager().isSavedReply(post.board, postId)) {
                    key += " (You)";
                }
            }
        } else if (anchor.text().startsWith(">>") && boardLinkMatcher.find()) {
            // board link
            t = PostLinkable.Type.BOARD;
            key = anchor.text();
            value = boardLinkMatcher.group(1);
        } else {
            // normal link
            t = PostLinkable.Type.LINK;
            key = anchor.text();
            value = href;
        }

        SpannableString link = new SpannableString(key);
        PostLinkable pl = new PostLinkable(post, key, value, t);
        link.setSpan(pl, 0, link.length(), 0);
        post.linkables.add(pl);

        return link;
    }

    private void detectLinks(Post post, String text, SpannableString spannable) {
        int startPos = 0;
        int endPos;
        while (true) {
            startPos = text.indexOf("://", startPos);
            if (startPos < 0) break;

            // go back to the first space
            while (startPos > 0 && !isWhitespace(text.charAt(startPos - 1))) {
                startPos--;
            }

            // find the last non whitespace character
            endPos = startPos;
            while (endPos < text.length() - 1 && !isWhitespace(text.charAt(endPos + 1))) {
                endPos++;
            }

            // one past
            endPos++;

            String linkString = text.substring(startPos, endPos);

            PostLinkable pl = new PostLinkable(post, linkString, linkString, PostLinkable.Type.LINK);
            spannable.setSpan(pl, startPos, endPos, 0);
            post.linkables.add(pl);

            startPos = endPos;
        }
    }

    private boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || c == '>'; // consider > as a link separator
    }

    // Below code taken from org.jsoup.nodes.Element.text(), but it preserves <br>
    private String getNodeText(Element node) {
        final StringBuilder accum = new StringBuilder();
        new NodeTraversor(new NodeVisitor() {
            public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode) node;
                    appendNormalisedText(accum, textNode);
                } else if (node instanceof Element) {
                    Element element = (Element) node;
                    if (accum.length() > 0 &&
                            element.isBlock() &&
                            !lastCharIsWhitespace(accum))
                        accum.append(" ");

                    if (element.tag().getName().equals("br")) {
                        accum.append("\n");
                    }
                }
            }

            public void tail(Node node, int depth) {
            }
        }).traverse(node);
        return accum.toString().trim();
    }

    private static boolean lastCharIsWhitespace(StringBuilder sb) {
        return sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ';
    }

    private static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();

        if (!preserveWhitespace(textNode.parent())) {
            text = normaliseWhitespace(text);
            if (lastCharIsWhitespace(accum))
                text = stripLeadingWhitespace(text);
        }
        accum.append(text);
    }

    private static String normaliseWhitespace(String text) {
        text = StringUtil.normaliseWhitespace(text);
        return text;
    }

    private static String stripLeadingWhitespace(String text) {
        return text.replaceFirst("^\\s+", "");
    }

    private static boolean preserveWhitespace(Node node) {
        // looks only at this element and one level up, to prevent recursion & needless stack searches
        if (node != null && node instanceof Element) {
            Element element = (Element) node;
            return element.tag().preserveWhitespace() ||
                    element.parent() != null && element.parent().tag().preserveWhitespace();
        }
        return false;
    }
}
