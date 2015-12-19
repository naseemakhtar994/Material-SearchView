package ru.shmakinv.android.widget.material.searchview;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.devspark.robototextview.util.RobotoTypefaceManager;
import com.devspark.robototextview.util.RobotoTypefaceUtils;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;

import ru.shmakinv.android.widget.material.searchview.transition.SizeTransition;

/**
 * ProgressDialogFragment
 *
 * @author: Vyacheslav Shmakin
 * @version: 23.07.2015
 */
public class SearchView extends BaseRestoreInstanceFragment implements
        DialogInterface.OnShowListener,
        SearchEditText.OnBackKeyPressListener, View.OnKeyListener {

    private static final String TAG = "SearchView";

    private RelativeLayout mRoot;
    private RelativeLayout mSearchRegion;
    private LinearLayout mSuggestionsRegion;
    private CardView mSearchOverlay;
    private ImageButton mNavBackBtn;
    private ImageButton mCloseVoiceBtn;
    private SearchEditText mSearchEditText;
    private RecyclerView mSuggestionsView;
    private RecyclerView.Adapter mAdapter;

    private OnVoiceSearchListener voiceSearchListener;
    private OnQueryTextListener onQueryTextListener;

    public SearchView() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View dialogLayout = inflater.inflate(R.layout.search_view_layout, container, true);
        initViews(dialogLayout);
        initWindowParams();

        return dialogLayout;
    }

    private void initViews(final View layout) {
        mRoot = (RelativeLayout) layout.findViewById(R.id.root);
        mSearchOverlay = (CardView) layout.findViewById(R.id.search_overlay);
        mNavBackBtn = (ImageButton) layout.findViewById(R.id.ibtn_navigation_back);
        mCloseVoiceBtn = (ImageButton) layout.findViewById(R.id.ibtn_voice_close);
        mSearchEditText = (SearchEditText) layout.findViewById(R.id.et_search_text);
        mSearchRegion = (RelativeLayout) layout.findViewById(R.id.search_region);
        mSuggestionsRegion = (LinearLayout) layout.findViewById(R.id.suggestions_region);
        mSuggestionsView = (RecyclerView) layout.findViewById(R.id.suggestion_list);
        if (mAdapter != null) {
            mSuggestionsView.setAdapter(mAdapter);
        }
        mSuggestionsView.setLayoutManager(
                new WrapContentLinearLayoutManager(getActivity()));

        Typeface typeface = RobotoTypefaceManager.obtainTypeface(
                getActivity().getApplicationContext(),
                this.mTypefaceValue);
        RobotoTypefaceUtils.setup(mSearchEditText, typeface);

        mSearchEditText.setText(mQuery);
        mSearchEditText.setHint(mHint);
        if (mSelection == -1) {
            mSearchEditText.setSelection(mSearchEditText.length());
        } else {
            mSearchEditText.setSelection(mSelection);
        }

        mSearchEditText.addTextChangedListener(mSearchTextWatcher);
        mSearchEditText.setOnBackKeyListener(this);
        mSearchEditText.setOnKeyListener(this);

        if (mSearchEditText != null) {
            updateCloseVoiceState(mSearchEditText.getText());
        }

        mCloseVoiceBtn.setOnClickListener(mCloseVoiceClickListener);
        mNavBackBtn.setOnClickListener(mNavigationBackClickListener);
        mSearchEditText.setCustomSelectionActionModeCallback(mNotAllowedToEditCallback);
    }

    private void initWindowParams() {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable());
        getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getDialog().getWindow().getDecorView().setOnTouchListener(mOnOutsideTouchListener);

        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
        params.x = 0;
        params.y = 0;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.windowAnimations = R.style.NoAnimationWindow;

        getDialog().getWindow().setAttributes(params);

        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setOnShowListener(this);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        Log.d(TAG, "onShow");
        if (toolbarRequestUpdateListener != null) {
            toolbarRequestUpdateListener.onRequestToolbarClear();
        }

        animateShow(mSearchOverlay, mSearchRegion, mMenuItemId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSearchEditText.setOnKeyListener(null);
        mSearchEditText.removeTextChangedListener(mSearchTextWatcher);
        mSearchEditText.setOnBackKeyListener(null);
        mCloseVoiceBtn.setOnClickListener(null);
        mNavBackBtn.setOnClickListener(null);
        mSearchEditText.setCustomSelectionActionModeCallback(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mSelection = mSearchEditText.getSelectionStart();
        super.onSaveInstanceState(outState);
    }

    private void onClose() {
        if (toolbarRequestUpdateListener != null) {
            toolbarRequestUpdateListener.onRequestToolbarRestore();
        }

        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            TransitionSet transition = new SizeTransition()
                    .setDuration(ANIMATOR_MIN_SUGGESTION_DURATION)
                    .setInterpolator(new LinearInterpolator())
                    .addListener(mSuggestionsDismissListener);

            TransitionManager.beginDelayedTransition(mSearchOverlay, transition);

            mSuggestionsRegion.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    0));
        } else {
            animateDismiss(mSearchOverlay, mSearchRegion, mMenuItemId);
        }
    }

    @Override
    public boolean OnBackKeyEvent(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            onClose();
            return true;
        }
        return false;
    }

    public void receiveVoiceSearchResult(String result) {
        String text = mSearchEditText.getText() + " " + result;
        mSearchEditText.setText(text);
    }

    @Override
    public void show(FragmentManager manager) {
        if (isShown()) {
            dismiss();
        }
        super.show(manager);
    }

    public void setTypeface(int typefaceValue) {
        setupTypeface(typefaceValue);

        Typeface typeface = RobotoTypefaceManager.obtainTypeface(
                getActivity().getApplicationContext(),
                typefaceValue);

        if (mSearchEditText != null) {
            RobotoTypefaceUtils.setup(mSearchEditText, typeface);
        }
    }

    public void setSuggestionAdapter(RecyclerView.Adapter adapter) {
        this.mAdapter = adapter;
        if (mSuggestionsView != null) {
            mSuggestionsView.setAdapter(this.mAdapter);
        }
    }

    public void setQuery(String query, boolean submit) {
        this.mQuery = query;
        if (mSearchEditText == null) {
            return;
        }
        mSearchEditText.setText(query);

        if (submit) {
            submitQuery();
        }
    }

    public String getQuery() {
        return mQuery;
    }

    public String getHint() {
        if (mSearchEditText == null) {
            return mHint;
        }
        return mSearchEditText.getHint().toString();
    }

    public void setHint(String hint) {
        this.mHint = hint;
        if (mSearchEditText != null) {
            mSearchEditText.setHint(hint);
        }
    }

    @Override
    protected void onShowSearchAnimationEnd() {
        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            mSuggestionsView.setAdapter(mAdapter);

            int itemHeight = ((WrapContentLinearLayoutManager) mSuggestionsView.getLayoutManager()).getChildHeight();
            int itemCount = mAdapter.getItemCount();

            long duration = Math.max(
                    ANIMATOR_MIN_SUGGESTION_DURATION,
                    Math.min(ANIMATOR_MAX_SUGGESTION_DURATION, itemCount * itemHeight));

            TransitionSet transition = new SizeTransition()
                    .setDuration(duration)
                    .setInterpolator(new LinearInterpolator());

            TransitionManager.beginDelayedTransition(mRoot, transition);

            mSuggestionsRegion.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    @Override
    protected void onSuggestionsDismissed() {
        animateDismiss(mSearchOverlay, mSearchRegion, mMenuItemId);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_UP
                && onQueryTextListener != null) {

            String query = mSearchEditText.getText().toString();
            boolean result = onQueryTextListener.onQueryTextSubmit(query);
            if (result) {
                submitQuery();
            }
        }

        return false;
    }

    public interface OnVoiceSearchListener {
        void onRequestVoiceSearch();
    }

    public void setOnVoiceSearchListener(OnVoiceSearchListener listener) {
        this.voiceSearchListener = listener;
    }

    @Override
    protected void onQueryTextChanged(Editable s) {
        mQuery = s.toString();
        updateCloseVoiceState(s);

        if (onQueryTextListener != null) {
            onQueryTextListener.onQueryTextChanged(s.toString());
        }
    }

    private void updateCloseVoiceState(Editable searchText) {
        if (searchText != null && searchText.length() != 0) {
            mCloseVoiceBtn.setVisibility(View.VISIBLE);
            mCloseVoiceBtn.setImageResource(R.drawable.searchview_button_close);
        } else if (!hasDeviceSpeechRecognitionTool()) {
            mCloseVoiceBtn.setVisibility(View.INVISIBLE);
        } else {
            mCloseVoiceBtn.setImageResource(R.drawable.searchview_button_voice);
        }
    }

    @Override
    protected void onCloseVoiceClicked() {
        if (mSearchEditText != null && mSearchEditText.getText() != null && mSearchEditText.getText().toString().length() != 0) {
            mSearchEditText.getText().clear();
        } else if (voiceSearchListener != null && hasDeviceSpeechRecognitionTool()) {
            voiceSearchListener.onRequestVoiceSearch();
        }
    }

    @Override
    protected void onNavigationBackClicked() {
        onClose();
    }

    @Override
    protected void onOutsideTouch(View v, MotionEvent event) {
        if (!mCloseRequested) {
            Rect rect = new Rect();
            mSearchOverlay.getHitRect(rect);

            if (!rect.contains((int) event.getX(), (int) event.getY())) {
                Log.d(TAG, "View " + v + "was touched outside");
                mCloseRequested = true;
                onClose();
            }
        }
    }

    private OnToolbarRequestUpdateListener toolbarRequestUpdateListener;

    public void setOnToolbarRequestUpdateListener(OnToolbarRequestUpdateListener listener) {
        this.toolbarRequestUpdateListener = listener;
    }

    public interface OnToolbarRequestUpdateListener {
        void onRequestToolbarClear();

        void onRequestToolbarRestore();
    }

    public boolean onOptionsItemSelected(FragmentManager manager, MenuItem item) {
        this.mMenuItemId = item.getItemId();
        show(manager);
        return true;
    }

    public interface OnQueryTextListener {
        boolean onQueryTextSubmit(String query);

        void onQueryTextChanged(String newText);
    }

    public void setOnQueryTextListener(OnQueryTextListener listener) {
        this.onQueryTextListener = listener;
    }

    private void submitQuery() {
        setSuggestionAdapter(null);
        mSearchEditText.clearFocus();
        hideKeyboard();
    }
}