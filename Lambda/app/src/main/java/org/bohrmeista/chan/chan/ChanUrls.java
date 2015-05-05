/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 * Copyright (C) 2014  wingy
 * Copyright (C) 2015  bohrmeista
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
package org.bohrmeista.chan.chan;

import java.util.Locale;

public class ChanUrls {
    private static String scheme;

    public static void loadScheme(boolean useHttps) {
        scheme = useHttps ? "https" : "http";
    }

    public static String getCatalogUrl(String board) {
        return scheme + "://lainchan.org/" + board + "/catalog.json";
    }

    public static String getPageUrl(String board, int pageNumber) {
        return scheme + "://lainchan.org/" + board + "/" + pageNumber + ".json";
    }

    public static String getThreadUrl(String board, int no) {
        return scheme + "://lainchan.org/" + board + "/res/" + no + ".json";
    }

    public static String getImageUrl(String board, String code, String extension, boolean thumb) {
        if (thumb)
            extension = "png";
        else
            extension = extension.toLowerCase();
        return scheme + "://lainchan.org/" + board + (thumb ? "/thumb/" : "/src/") + code + "." + extension;
    }

    public static String getSpoilerUrl() {
        return scheme + "://lainchan.org/static/spoiler.png";
    }

    public static String getCountryFlagUrl(String countryCode) {
        return scheme + "://lainchan.org/static/flags/" + countryCode.toLowerCase(Locale.ENGLISH) + ".png";
    }

    // TODO: Remove if unused
    public static String getTrollCountryFlagUrl(String countryCode) {
        return getCountryFlagUrl(countryCode);
    }

    public static String getReplyUrl() {
        return "https://lainchan.org/post.php";
    }

    // TODO: Implement
    public static String getDeleteUrl() {
        return getReplyUrl();
    }

    public static String getBoardUrlDesktop(String board) {
        return scheme + "://lainchan.org/" + board + "/";
    }

    public static String getThreadUrlDesktop(String board, int no) {
        return scheme + "://lainchan.org/" + board + "/res/" + no + ".html";
    }

    public static String getCatalogUrlDesktop(String board) {
        return scheme + "://lainchan.org/" + board + "/catalog.html";
    }

    // TODO: Implement
    public static String getReportUrl(String board, int no) {
        return ""; //return "https://sys.4chan.org/" + board + "/imgboard.php?mode=report&no=" + no;
    }
}
