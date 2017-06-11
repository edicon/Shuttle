package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.PlaylistAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.PlaylistView;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.simplecity.amp_library.ShuttleApplication.HI_RES;
import static com.simplecity.amp_library.utils.ColorUtils.fetchAttrColor;
import static com.simplecity.amp_library.utils.MenuUtils.changeActionModeBackground;

public class PlaylistFragment extends BaseFragment implements
        MusicUtils.Defs,
        PlaylistAdapter.PlaylistListener,
        RecyclerView.RecyclerListener {

    public interface PlaylistClickListener {

        void onItemClicked(Playlist playlist);
    }

    private static final String TAG = "PlaylistFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private SharedPreferences prefs;

    private FastScrollRecyclerView mRecyclerView;

    private PlaylistAdapter mPlaylistAdapter;

    private PlaylistClickListener playlistClickListener;

    private Subscription subscription;

    // HI_RES
    MultiSelector multiSelector = new MultiSelector();
    ActionMode actionMode;
    boolean inActionMode = false;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
    private RequestManager requestManager;
    // HI_RES
    private Toolbar toolbar;
    private View dummyToolbar;
    private View dummyStatusBar;

    public PlaylistFragment() {

    }

    public static PlaylistFragment newInstance(String pageTitle) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        playlistClickListener = (PlaylistClickListener) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        playlistClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( HI_RES )
            setHasOptionsMenu(true);

        mPlaylistAdapter = new PlaylistAdapter();
        mPlaylistAdapter.setListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color")
                    || key.equals("pref_theme_accent_color")
                    || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            } else if (key.equals("albumWhitelist")) {
                refreshAdapterItems();
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

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
            mRecyclerView.setRecyclerListener(this);
            mRecyclerView.setAdapter(mPlaylistAdapter);
            themeUIComponents();
        }

        if( HI_RES )
            return rootView;
        return mRecyclerView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }

    private void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                Observable<List<Playlist>> defaultPlaylistsObservable =
                        Observable.defer(() ->
                                {
                                    List<Playlist> playlists = new ArrayList<>();

                                    if( !HI_RES ) {
                                        Playlist podcastPlaylist = Playlist.podcastPlaylist();
                                        if (podcastPlaylist != null) {
                                            playlists.add(podcastPlaylist);
                                        }
                                    }

                                    playlists.add(Playlist.recentlyAddedPlaylist());
                                    playlists.add(Playlist.mostPlayedPlaylist());
                                    return Observable.just(playlists);
                                }
                        );

                Observable<List<Playlist>> playlistsObservable = DataManager.getInstance().getPlaylistsRelay();

                subscription = Observable.combineLatest(
                        defaultPlaylistsObservable, playlistsObservable, (defaultPlaylists, playlists) -> {
                            List<Playlist> list = new ArrayList<>();
                            list.addAll(defaultPlaylists);
                            // Skip Favorite
                            if( HI_RES ) {
                                for( Playlist playlist : playlists ) {
                                    if( !"Favorites".equals(playlist.name))
                                        list.add(playlist);
                                }
                            } else {
                                list.addAll(playlists);
                            }
                            return list;
                        })
                        .subscribeOn(Schedulers.io())
                        .map(playlists -> Stream.of(playlists)
                                .sorted((a, b) -> ComparisonUtils.compare(a.name, b.name))
                                .sorted((a, b) -> ComparisonUtils.compareInt(a.type, b.type))
                                // HI_RES .map(playlist -> (AdaptableItem) new PlaylistView(playlist))
                                .map(playlist -> (AdaptableItem) new PlaylistView(getActivity(), playlist, requestManager, multiSelector))
                                .collect(Collectors.toList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {
                            if (items.isEmpty()) {
                                mPlaylistAdapter.setEmpty(new EmptyView(R.string.empty_playlist));
                            } else {
                                mPlaylistAdapter.setItems(items);
                            }
                        });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if( !HI_RES )
            inflater.inflate(R.menu.context_menu_playlists, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sort:
                break;
            case R.id.action_setting:
                if (inActionMode) {
                    break;
                }

                if (multiSelector.getSelectedPositions().size() == 0) {
                    actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
                    inActionMode = true;
                }

                // Do Not Select Default
                // int position = 0;
                // multiSelector.setSelected(position, songsAdapter.getItemId(position), !multiSelector.isSelected(position, songsAdapter.getItemId(position)));
                updateActionModeSelectionCount();
                break;
        }
        return true;
    }

    private void themeUIComponents() {

        if( HI_RES )
            dummyStatusBar.setVisibility(View.VISIBLE);

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

    @Override
    public void onItemClick(View v, int position, Playlist playlist) {
        if ( inActionMode ) {
            multiSelector.setSelected(position, mPlaylistAdapter.getItemId(position), !multiSelector.isSelected(position, mPlaylistAdapter.getItemId(position)));

            if (multiSelector.getSelectedPositions().size() == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
            updateActionModeSelectionCount();
        } else {
            playlistClickListener.onItemClicked(playlist);
        }
    }

    @Override
    public void onOverflowClick(View v, int position, Playlist playlist) {
        PopupMenu menu = new PopupMenu(PlaylistFragment.this.getActivity(), v);
        MenuUtils.addPlaylistMenuOptions(menu, playlist);
        MenuUtils.addClickHandler(getContext(), menu, playlist, null, null);
        menu.show();
    }

    // HI_RES
    private void updateActionModeSelectionCount() {
        if (actionMode != null && multiSelector != null) {
            if( !HI_RES )
                actionMode.setTitle(getString(R.string.action_mode_selection_count, multiSelector.getSelectedPositions().size()));
        }
    }

    private ActionMode.Callback mActionModeCallback = new ModalMultiSelectorCallback(multiSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ThemeUtils.themeContextualActionBar(getActivity());
            inActionMode = true;
            getActivity().getMenuInflater().inflate(R.menu.action_menu_playlists, menu);
            // SubMenu sub = menu.getItem(0).getSubMenu();
            // PlaylistUtils.makePlaylistMenu(getActivity(), sub, PLAYLIST_FRAGMENT_GROUP_ID);
            return true;
        }

        // ToDo: ActionMode Background
        //  -https://stackoverflow.com/questions/20769315/how-to-change-actionmode-background-color-in-android
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            if( HI_RES ) {
                changeActionModeBackground( getActivity(), fetchAttrColor(getActivity(), R.attr.colorActionModeBackground));
                ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
                dummyStatusBar.setVisibility(View.GONE);
                // dummyToolbar.setVisibility(View.GONE);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

            final List<Playlist> checkedPlaylists = getCheckedPlaylists();

            if (checkedPlaylists == null || checkedPlaylists.size() == 0) {
                if(item.getItemId() == R.id.menu_cancel) {
                    if( actionMode != null )
                        actionMode.finish();
                    return true;
                }
                if(item.getItemId() != R.id.add)
                    return true;
            }
            Context context = getContext();

            switch (item.getItemId()) {
                // ToDo: rename: SingleSelector 적용
                case R.id.rename:
                    if( checkedPlaylists.size() > 1) {
                        Toast.makeText(context, R.string.playlist_renamed_info, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    for( Playlist playlist : checkedPlaylists) {
                        PlaylistUtils.renamePlaylistDialog(getContext(), playlist, null );
                    }
                    break;
                case R.id.add:
                    // ToDo: Check listener
                    List<Song> songs = new ArrayList<>();
                    // songs.add(song);
                    PlaylistUtils.createPlaylistDialog(getActivity(), songs /* (OnSavePlaylistListener)getActivity() or null*/);
                    break;
                case R.id.delete:
                    for( Playlist playlist : checkedPlaylists) {
                        playlist.delete(context);
                        Toast.makeText(context, R.string.playlist_deleted_message, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.menu_cancel:
                    if (actionMode != null)
                        actionMode.finish();
                    break;
            }
            mPlaylistAdapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            PlaylistFragment.this.actionMode = null;
            multiSelector.clearSelections();

            if( HI_RES ) {
                dummyStatusBar.setVisibility(View.VISIBLE);
                ((AppCompatActivity) getActivity()).getSupportActionBar().show();
            }
        }
    };

    List<Playlist> getCheckedPlaylists() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> mPlaylistAdapter.getPlaylist(i))
                .collect(Collectors.toList());
    }
    // END HI_RES

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            mPlaylistAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
