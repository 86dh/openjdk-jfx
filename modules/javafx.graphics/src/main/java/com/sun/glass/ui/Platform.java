/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.ui;

import com.sun.javafx.PlatformUtil;

final class Platform {

    public static final String MAC = "Mac";
    public static final String WINDOWS = "Win";
    public static final String GTK = "Gtk";
    public static final String IOS = "Ios";
    public static final String UNKNOWN = "unknown";

    static private String type = null;

    static public synchronized String determinePlatform() {
        if (type == null) {

            // Provide for a runtime override, allowing EGL for example
            String userPlatform = System.getProperty("glass.platform");

            if (userPlatform != null) {
                if (userPlatform.equals("macosx"))
                   type = MAC;
                else if (userPlatform.equals("windows"))
                   type = WINDOWS;
                else if (userPlatform.equals("linux"))
                   type = GTK;
                else if (userPlatform.equals("gtk"))
                   type = GTK;
                else if (userPlatform.equals("ios"))
                   type = IOS;
                else
                   type = userPlatform;
                return type;
            }

            if (PlatformUtil.isMac()) {
                type = MAC;
            } else if (PlatformUtil.isWindows()) {
                type = WINDOWS;
            } else if (PlatformUtil.isLinux()) {
                type = GTK;
            } else if (PlatformUtil.isIOS()) {
                type = IOS;
            }
        }

        return type;
    }
}
