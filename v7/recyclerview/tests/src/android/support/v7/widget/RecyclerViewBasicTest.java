/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewBasicTest {

    RecyclerView mRecyclerView;


    @Before
    public void setUp() throws Exception {
        mRecyclerView = new RecyclerView(getContext());
    }

    private Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    @Test
    public void measureWithoutLayoutManager() {
        measure();
    }

    private void measure() {
        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 320, View.MeasureSpec.AT_MOST | 240);
    }

    private void layout() {
        mRecyclerView.layout(0, 0, 320, 320);
    }

    private void focusSearch() {
        mRecyclerView.focusSearch(1);
    }

    @Test
    public void layoutWithoutAdapter() throws InterruptedException {
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        assertEquals("layout manager should not be called if there is no adapter attached",
                0, layoutManager.mLayoutCount);
    }

    public void setScrollContainer() {
        assertEquals("RecyclerView should announce itself as scroll container for the IME to "
                + "handle it properly", true, mRecyclerView.isScrollContainer());
    }

    @Test
    public void layoutWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
    }

    @Test
    public void focusWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        focusSearch();
    }

    @Test
    public void scrollWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.scrollBy(10, 10);
    }

    @Test
    public void smoothScrollWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.smoothScrollBy(10, 10);
    }

    @Test
    public void scrollToPositionWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.scrollToPosition(5);
    }

    @Test
    public void smoothScrollToPositionWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.smoothScrollToPosition(5);
    }

    @Test
    public void interceptTouchWithoutLayoutManager() {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        assertFalse(mRecyclerView.onInterceptTouchEvent(
                MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 10, 10, 0)));
    }

    @Test
    public void onTouchWithoutLayoutManager() {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        assertFalse(mRecyclerView.onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 10, 10, 0)));
    }

    @Test
    public void layoutSimple() throws InterruptedException {
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(new MockAdapter(3));
        layout();
        assertEquals("when both layout manager and activity is set, recycler view should call"
                + " layout manager's layout method", 1, layoutManager.mLayoutCount);
    }

    @Test
    public void observingAdapters() {
        MockAdapter adapterOld = new MockAdapter(1);
        mRecyclerView.setAdapter(adapterOld);
        assertTrue("attached adapter should have observables", adapterOld.hasObservers());

        MockAdapter adapterNew = new MockAdapter(2);
        mRecyclerView.setAdapter(adapterNew);
        assertFalse("detached adapter should lose observable", adapterOld.hasObservers());
        assertTrue("new adapter should have observers", adapterNew.hasObservers());

        mRecyclerView.setAdapter(null);
        assertNull("adapter should be removed successfully", mRecyclerView.getAdapter());
        assertFalse("when adapter is removed, observables should be removed too",
                adapterNew.hasObservers());
    }

    @Test
    public void adapterChangeCallbacks() {
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        MockAdapter adapterOld = new MockAdapter(1);
        mRecyclerView.setAdapter(adapterOld);
        layoutManager.assertPrevNextAdapters(null, adapterOld);

        MockAdapter adapterNew = new MockAdapter(2);
        mRecyclerView.setAdapter(adapterNew);
        layoutManager.assertPrevNextAdapters("switching adapters should trigger correct callbacks"
                , adapterOld, adapterNew);

        mRecyclerView.setAdapter(null);
        layoutManager.assertPrevNextAdapters(
                "Setting adapter null should trigger correct callbacks",
                adapterNew, null);
    }

    @Test
    public void recyclerOffsetsOnMove() {
        MockLayoutManager  layoutManager = new MockLayoutManager();
        final List<RecyclerView.ViewHolder> recycledVhs = new ArrayList<>();
        mRecyclerView.setLayoutManager(layoutManager);
        MockAdapter adapter = new MockAdapter(100) {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                super.onViewRecycled(holder);
                recycledVhs.add(holder);
            }
        };
        MockViewHolder mvh = new MockViewHolder(new TextView(getContext()));
        mRecyclerView.setAdapter(adapter);
        adapter.bindViewHolder(mvh, 20);
        mRecyclerView.mRecycler.mCachedViews.add(mvh);
        mRecyclerView.offsetPositionRecordsForRemove(10, 9, false);

        mRecyclerView.offsetPositionRecordsForRemove(11, 1, false);
        assertEquals(1, recycledVhs.size());
        assertSame(mvh, recycledVhs.get(0));
    }

    @Test
    public void recyclerOffsetsOnAdd() {
        MockLayoutManager  layoutManager = new MockLayoutManager();
        final List<RecyclerView.ViewHolder> recycledVhs = new ArrayList<>();
        mRecyclerView.setLayoutManager(layoutManager);
        MockAdapter adapter = new MockAdapter(100) {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                super.onViewRecycled(holder);
                recycledVhs.add(holder);
            }
        };
        MockViewHolder mvh = new MockViewHolder(new TextView(getContext()));
        mRecyclerView.setAdapter(adapter);
        adapter.bindViewHolder(mvh, 20);
        mRecyclerView.mRecycler.mCachedViews.add(mvh);
        mRecyclerView.offsetPositionRecordsForRemove(10, 9, false);

        mRecyclerView.offsetPositionRecordsForInsert(15, 10);
        assertEquals(11, mvh.mPosition);
    }

    @Test
    public void savedStateWithStatelessLayoutManager() throws InterruptedException {
        mRecyclerView.setLayoutManager(new MockLayoutManager() {
            @Override
            public Parcelable onSaveInstanceState() {
                return null;
            }
        });
        mRecyclerView.setAdapter(new MockAdapter(3));
        Parcel parcel = Parcel.obtain();
        String parcelSuffix = UUID.randomUUID().toString();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);

        // reset position for reading
        parcel.setDataPosition(0);
        RecyclerView restored = new RecyclerView(getContext());
        restored.setLayoutManager(new MockLayoutManager());
        mRecyclerView.setAdapter(new MockAdapter(3));
        // restore
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        restored.onRestoreInstanceState(savedState);

        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        assertEquals("When unmarshalling, all of the parcel should be read", 0, parcel.dataAvail());

    }

    @Test
    public void savedState() throws InterruptedException {
        MockLayoutManager mlm = new MockLayoutManager();
        mRecyclerView.setLayoutManager(mlm);
        mRecyclerView.setAdapter(new MockAdapter(3));
        layout();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);

        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);

        RecyclerView restored = new RecyclerView(getContext());
        MockLayoutManager mlmRestored = new MockLayoutManager();
        restored.setLayoutManager(mlmRestored);
        restored.setAdapter(new MockAdapter(3));
        restored.onRestoreInstanceState(savedState);

        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        assertEquals("When unmarshalling, all of the parcel should be read", 0, parcel.dataAvail());
        assertEquals("uuid in layout manager should be preserved properly", mlm.mUuid,
                mlmRestored.mUuid);
        assertNotSame("stateless parameter should not be preserved", mlm.mLayoutCount,
                mlmRestored.mLayoutCount);
        layout();
    }

    @Test
    public void dontSaveChildrenState() throws InterruptedException {
        MockLayoutManager mlm = new MockLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                View view = recycler.getViewForPosition(0);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            }
        };
        mRecyclerView.setLayoutManager(mlm);
        mRecyclerView.setAdapter(new MockAdapter(3) {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final LoggingView itemView = new LoggingView(parent.getContext());
                //noinspection ResourceType
                itemView.setId(3);
                return new MockViewHolder(itemView);
            }
        });
        measure();
        layout();
        View view = mRecyclerView.getChildAt(0);
        assertNotNull("test sanity", view);
        LoggingView loggingView = (LoggingView) view;
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        mRecyclerView.saveHierarchyState(container);
        assertEquals("children's save state method should not be called", 0,
                loggingView.getOnSavedInstanceCnt());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void prefetchChangesCacheSize() {
        MockLayoutManager mlm = new MockLayoutManager() {
            @Override
            int getItemPrefetchCount() {
                return 3;
            }
        };
        RecyclerView.Recycler recycler = mRecyclerView.mRecycler;
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE, recycler.mViewCacheMax);
        mRecyclerView.setLayoutManager(mlm);
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE + 3, recycler.mViewCacheMax);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void prefetcherWithoutLayout() {
        mRecyclerView.mViewPrefetcher.run();
    }

    static class MockLayoutManager extends RecyclerView.LayoutManager {

        int mLayoutCount = 0;

        int mAdapterChangedCount = 0;

        RecyclerView.Adapter mPrevAdapter;

        RecyclerView.Adapter mNextAdapter;

        String mUuid = UUID.randomUUID().toString();

        @Override
        public void onAdapterChanged(RecyclerView.Adapter oldAdapter,
                RecyclerView.Adapter newAdapter) {
            super.onAdapterChanged(oldAdapter, newAdapter);
            mPrevAdapter = oldAdapter;
            mNextAdapter = newAdapter;
            mAdapterChangedCount++;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            mLayoutCount += 1;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            LayoutManagerSavedState lss = new LayoutManagerSavedState();
            lss.mUuid = mUuid;
            return lss;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
            super.onRestoreInstanceState(state);
            if (state instanceof LayoutManagerSavedState) {
                mUuid = ((LayoutManagerSavedState) state).mUuid;
            }
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        public void assertPrevNextAdapters(String message, RecyclerView.Adapter prevAdapter,
                RecyclerView.Adapter nextAdapter) {
            assertSame(message, prevAdapter, mPrevAdapter);
            assertSame(message, nextAdapter, mNextAdapter);
        }

        public void assertPrevNextAdapters(RecyclerView.Adapter prevAdapter,
                RecyclerView.Adapter nextAdapter) {
            assertPrevNextAdapters("Adapters from onAdapterChanged callback should match",
                    prevAdapter, nextAdapter);
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return dx;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return dy;
        }

        @Override
        public boolean canScrollHorizontally() {
            return true;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }
    }

    static class LayoutManagerSavedState implements Parcelable {

        String mUuid;

        public LayoutManagerSavedState(Parcel in) {
            mUuid = in.readString();
        }

        public LayoutManagerSavedState() {

        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mUuid);
        }

        public static final Parcelable.Creator<LayoutManagerSavedState> CREATOR
                = new Parcelable.Creator<LayoutManagerSavedState>() {
            @Override
            public LayoutManagerSavedState createFromParcel(Parcel in) {
                return new LayoutManagerSavedState(in);
            }

            @Override
            public LayoutManagerSavedState[] newArray(int size) {
                return new LayoutManagerSavedState[size];
            }
        };
    }

    static class MockAdapter extends RecyclerView.Adapter {

        private int mCount = 0;


        MockAdapter(int count) {
            this.mCount = count;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MockViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mCount;
        }

        void removeItems(int start, int count) {
            mCount -= count;
            notifyItemRangeRemoved(start, count);
        }

        void addItems(int start, int count) {
            mCount += count;
            notifyItemRangeInserted(start, count);
        }
    }

    static class MockViewHolder extends RecyclerView.ViewHolder {
        public Object mItem;
        public MockViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class LoggingView extends TextView {
        private int mOnSavedInstanceCnt = 0;

        public LoggingView(Context context) {
            super(context);
        }

        public LoggingView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public LoggingView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            mOnSavedInstanceCnt ++;
            return super.onSaveInstanceState();
        }

        public int getOnSavedInstanceCnt() {
            return mOnSavedInstanceCnt;
        }
    }
}