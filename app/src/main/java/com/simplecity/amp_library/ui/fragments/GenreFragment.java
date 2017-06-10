package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.ui.adapters.GenreAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.GenreView;
import com.simplecity.amp_library.ui.recyclerview.GridDividerDecoration;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static com.simplecity.amp_library.ShuttleApplication.HI_RES;

public class GenreFragment extends BaseFragment implements
        MusicUtils.Defs,
        RecyclerView.RecyclerListener,
        GenreAdapter.GenreListener {

    private static final String TAG = "GenreFragment";

    public interface GenreClickListener {

        void onItemClicked(Genre genre);
    }

    private static final String ARG_PAGE_TITLE = "page_title";

    private SharedPreferences mPrefs;

    private GenreClickListener genreClickListener;

    private FastScrollRecyclerView mRecyclerView;

    private GenreAdapter genreAdapter;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private Subscription subscription;

    // HI_RES
    private Toolbar toolbar;
    private View dummyToolbar;
    private View dummyStatusBar;

    public GenreFragment() {

    }

    public static GenreFragment newInstance(String pageTitle) {
        GenreFragment fragment = new GenreFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        genreClickListener = (GenreClickListener) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        genreAdapter = new GenreAdapter();
        genreAdapter.setListener(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // ToDo: HI_RES
        View rootView = inflater.inflate(R.layout.fragment_recycler_hires, container, false);

        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        dummyToolbar = rootView.findViewById(R.id.dummyToolbar);
        dummyStatusBar = rootView.findViewById(R.id.dummyStatusBar);

        //We need to set the dummy status bar height.
        if (ShuttleUtils.hasKitKat()) {
            LinearLayout.LayoutParams statusBarParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) ActionBarUtils.getStatusBarHeight(getActivity()));
            dummyStatusBar.setLayoutParams(statusBarParams);
        } else {
            dummyStatusBar.setVisibility(View.GONE);
        }

        if (getParentFragment() == null || !(getParentFragment() instanceof PlayerFragment /*MainFragment*/)) {
            if (ShuttleUtils.hasKitKat()) {
                dummyStatusBar.setVisibility(View.VISIBLE);
            }
            dummyToolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
            if (ShuttleUtils.hasKitKat()) {
                dummyStatusBar.setVisibility(View.GONE);
            }
            dummyToolbar.setVisibility(View.GONE);
        }
        // ToDo: END HI_RES

        if (mRecyclerView == null) {

            if( HI_RES )
                mRecyclerView = (FastScrollRecyclerView) rootView.findViewById(R.id.fragment_recycler);
            else
                mRecyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mRecyclerView.addItemDecoration(new GridDividerDecoration(getResources(), 4, true));
            mRecyclerView.setRecyclerListener(this);
            mRecyclerView.setAdapter(genreAdapter);

            themeUIComponents();
        }

        if( HI_RES )
            return rootView;
        return mRecyclerView;
    }

    @Override
    public void onPause() {

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    private void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                subscription = DataManager.getInstance().getGenresRelay()
                        .map(genres -> Stream.of(genres)
                                .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                                .map(genre -> (AdaptableItem) new GenreView(genre))
                                .collect(Collectors.toList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {
                            if (items.isEmpty()) {
                                genreAdapter.setEmpty(new EmptyView(R.string.empty_genres));
                            } else {
                                genreAdapter.setItems(items);
                            }
                        });
            }
        });
    }

    private void themeUIComponents() {

        if( HI_RES )
            dummyStatusBar.setVisibility(View.VISIBLE);

        if (mRecyclerView != null) {
            ThemeUtils.themeRecyclerView(mRecyclerView);
            mRecyclerView.setThumbColor(ColorUtils.getAccentColor());
            mRecyclerView.setPopupBgColor(ColorUtils.getAccentColor());
            mRecyclerView.setPopupTextColor(ColorUtils.getAccentColorSensitiveTextColor(getContext()));
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    ThemeUtils.themeRecyclerView(recyclerView);
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
        }
    }

    @Override
    public void onItemClick(View v, int position, Genre genre) {
        genreClickListener.onItemClicked(genre);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            genreAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
