/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdklib.devices;

import java.awt.Point;
import java.io.File;

public class Meta {
    private File mIconSixtyFour;
    private File mIconSixteen;
    private File mFrame;
    private Point mFrameOffsetLandscape;
    private Point mFrameOffsetPortrait;

    public File getIconSixtyFour() {
        return mIconSixtyFour;
    }

    public void setIconSixtyFour(File iconSixtyFour) {
        mIconSixtyFour = iconSixtyFour;
    }

    public boolean hasIconSixtyFour() {
        if (mIconSixtyFour != null && mIconSixtyFour.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    public File getIconSixteen() {
        return mIconSixteen;
    }

    public void setIconSixteen(File iconSixteen) {
        mIconSixteen = iconSixteen;
    }

    public boolean hasIconSixteen() {
        if (mIconSixteen != null && mIconSixteen.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    public File getFrame() {
        return mFrame;
    }

    public void setFrame(File frame) {
        mFrame = frame;
    }

    public boolean hasFrame() {
        if (mFrame != null && mFrame.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    public Point getFrameOffsetLandscape() {
        return mFrameOffsetLandscape;
    }

    public void setFrameOffsetLandscape(Point offset) {
        mFrameOffsetLandscape = offset;
    }

    public Point getFrameOffsetPortrait() {
        return mFrameOffsetPortrait;
    }

    public void setFrameOffsetPortrait(Point offset) {
        mFrameOffsetPortrait = offset;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Meta)) {
            return false;
        }
        Meta m = (Meta) o;

        // Note that any of the fields of either object can be null
        if (mIconSixtyFour != null && !mIconSixtyFour.equals(m.getIconSixtyFour())){
            return false;
        } else if (m.getIconSixtyFour() != null && !m.getIconSixtyFour().equals(mIconSixtyFour)) {
            return false;
        }

        if (mIconSixteen != null && !mIconSixteen.equals(m.getIconSixteen())){
            return false;
        } else if (m.getIconSixteen() != null && !m.getIconSixteen().equals(mIconSixteen)) {
            return false;
        }

        if (mFrame != null && !mFrame.equals(m.getFrame())) {
            return false;
        } else if (m.getFrame() != null && !m.getFrame().equals(mFrame)) {
            return false;
        }

        if (mFrameOffsetLandscape != null
                && !mFrameOffsetLandscape.equals(m.getFrameOffsetLandscape())){
            return false;
        } else if (m.getFrameOffsetLandscape() != null
                && !m.getFrameOffsetLandscape().equals(mFrameOffsetLandscape)){
            return false;
        }


        if (mFrameOffsetPortrait != null
                && !mFrameOffsetPortrait.equals(m.getFrameOffsetPortrait())){
            return false;
        } else if (m.getFrameOffsetPortrait() != null
                && !m.getFrameOffsetPortrait().equals(mFrameOffsetPortrait)){
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        if(mIconSixteen != null){
            for (Character c : mIconSixteen.getAbsolutePath().toCharArray()) {
                hash = 31 * hash + c;
            }
        }
        if(mIconSixtyFour != null){
            for (Character c : mIconSixtyFour.getAbsolutePath().toCharArray()) {
                hash = 31 * hash + c;
            }
        }
        if(mFrame != null){
            for (Character c : mFrame.getAbsolutePath().toCharArray()) {
                hash = 31 * hash + c;
            }
        }
        if(mFrameOffsetLandscape != null){
            hash = 31 * hash + mFrameOffsetLandscape.x;
            hash = 31 * hash + mFrameOffsetLandscape.y;
        }
        if(mFrameOffsetPortrait != null){
            hash = 31 * hash + mFrameOffsetPortrait.x;
            hash = 31 * hash + mFrameOffsetPortrait.y;
        }
        return hash;
    }
}
