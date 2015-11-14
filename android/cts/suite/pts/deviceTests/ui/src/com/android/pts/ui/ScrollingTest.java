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
package com.android.pts.ui;

import android.cts.util.TimeoutReq;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.MeasureTime;
import com.android.pts.util.PtsActivityInstrumentationTestCase2;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;

import java.io.IOException;

public class ScrollingTest extends PtsActivityInstrumentationTestCase2<ScrollingActivity> {
    private ScrollingActivity mActivity;

    public ScrollingTest() {
        super(ScrollingActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        getInstrumentation().waitForIdleSync();
        try {
            runTestOnUiThread(new Runnable() {
                public void run() {
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mActivity = null;
        super.tearDown();
    }

    @TimeoutReq(minutes = 30)
    public void testFullScrolling() throws Exception {
        final int NUMBER_REPEAT = 10;
        final ScrollingActivity activity = mActivity;
        double[] results = MeasureTime.measure(NUMBER_REPEAT, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                assertTrue(activity.scrollToBottom());
                assertTrue(activity.scrollToTop());
            }
        });
        getReportLog().printArray("ms", results, false);
        Stat.StatResult stat = Stat.getStat(results);
        getReportLog().printSummary("Time ms", stat.mAverage, stat.mStddev);
    }
}
