package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.FavoriteHeader;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.adapters.FavoriteAdapter;
import com.simplecity.amp_library.ui.adapters.SongAdapter;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.FavoriteHeaderView;
import com.simplecity.amp_library.ui.modelviews.FavoriteSongView;
import com.simplecity.amp_library.ui.modelviews.ShuffleView;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.IndiUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.simplecity.amp_library.ShuttleApplication.HI_RES;
import static com.simplecity.amp_library.utils.ColorUtils.fetchAttrColor;
import static com.simplecity.amp_library.utils.MenuUtils.changeActionModeBackground;

// HI_RES : From SongFragment
public class FavoriteFragment extends BaseFragment implements
        MusicUtils.Defs,
        RecyclerView.RecyclerListener,
        FavoriteAdapter.FavoriteListener {

    private static final String TAG = "FavoriteFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private SharedPreferences mPrefs;

    private FastScrollRecyclerView mRecyclerView;

    private GridLayoutManager layoutManager;

    private FavoriteAdapter favoriteAdapter;

    MultiSelector multiSelector = new MultiSelector();

    ActionMode actionMode;

    boolean inActionMode = false;

    private BroadcastReceiver mReceiver;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private boolean sortOrderChanged = false;

    private Subscription subscription;
    private CompositeSubscription comSubscription;

    private ShuffleView shuffleView;

    // HI_RES
    private Toolbar toolbar;
    private View dummyToolbar;
    private View dummyStatusBar;
    private RequestManager requestManager;

    public FavoriteFragment() {

    }

    public static FavoriteFragment newInstance(String pageTitle) {

        FavoriteFragment fragment = new FavoriteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);

        favoriteAdapter = new FavoriteAdapter();
        favoriteAdapter.setListener(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("restartLoader")) {
                    refreshAdapterItems();
                }
            }
        };

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                favoriteAdapter.notifyItemRangeChanged(0, favoriteAdapter.getItemCount());
                themeUIComponents();
            } else if (key.equals("songWhitelist")) {
                refreshAdapterItems();
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        shuffleView = new ShuffleView();

        if( HI_RES ) {
            if (requestManager == null) {
                requestManager = Glide.with(this);
            }
        }
    }

    private void themeUIComponents() {
        if( HI_RES ) {
            dummyStatusBar.setVisibility(View.VISIBLE);
            IndiUtils.hideIndiBar(getActivity(), dummyStatusBar);
        }

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

            int spanCount = SettingsManager.getInstance().getFavoriteColumnCount(getResources());
            layoutManager = new GridLayoutManager(getContext(), spanCount);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (favoriteAdapter.items.get(position) instanceof EmptyView) {
                        return spanCount;
                    }
                    return 1;
                }
            });

            if( HI_RES )
                mRecyclerView = (FastScrollRecyclerView) rootView.findViewById(R.id.fragment_recycler);
            else
                mRecyclerView = (FastScrollRecyclerView) inflater.inflate(R.layout.fragment_recycler, container, false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mRecyclerView.setRecyclerListener(this);
            mRecyclerView.setAdapter(favoriteAdapter);

            themeUIComponents();
        }

        if( HI_RES )
            return rootView;
        return mRecyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("restartLoader");
        getActivity().registerReceiver(mReceiver, filter);

        refreshAdapterItems();
    }

    void refreshAdapterItems() {
        PermissionUtils.RequestStoragePermissions(() -> {
                    if (getActivity() != null && isAdded()) {

                        boolean ascending = SortManager.getInstance().getSongsAscending();

                        /*
                        subscription = DataManager.getInstance().getSongsRelay()
                                .flatMap(songs -> {
                                    //Sort
                                    SortManager.getInstance().sortSongs(songs);
                                    //Reverse if required
                                    if (!ascending) {
                                        Collections.reverse(songs);
                                    }

                                    if( !HI_RES )
                                       requestManager = null;
                                    return Observable.from(songs)
                                            .map(song -> (AdaptableItem) new SongView(song, multiSelector, requestManager))
                                            // ToDo:  HI_RES
                                            // SongView songView = new SongView(song, multiSelector, requestManager);
                                            // songView.setShowAlbumArt(true);
                                            // return songView;
                                            .toList();

                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(items -> {

                                    if (items.isEmpty()) {
                                        favoriteAdapter.setEmpty(new EmptyView(R.string.empty_songlist));
                                    } else {
                                        if( !HI_RES )
                                            items.add(0, shuffleView);
                                        favoriteAdapter.setItems(items);
                                    }

                                    //Move the RV back to the top if we've had a sort order change.
                                    if (sortOrderChanged) {
                                        mRecyclerView.scrollToPosition(0);
                                    }

                                    sortOrderChanged = false;
                                });

                        */

                        // From SuggestFragment
                        comSubscription = new CompositeSubscription();
                        Observable<Playlist> favouritesPlaylistObservable = Observable.fromCallable(Playlist::favoritesPlaylist)
                                .subscribeOn(Schedulers.io())
                                .cache();

                        Observable<List<Song>> favouritesSongsObservable = favouritesPlaylistObservable
                                .filter(playlist -> playlist != null)
                                .flatMap(playlist -> playlist.getSongsObservable(getContext()))
                                .cache();

                        /*
                        Observable<List<AdaptableItem>> favoriteSongsItemsObservable = favouritesPlaylistObservable
                                .flatMap(playlist -> {

                                    FavoriteHeader favoriteHeader = new FavoriteHeader(getString(R.string.fav_title), getString(R.string.suggested_favorite_subtitle), playlist);
                                    FavoriteHeaderView favoriteHeaderView = new FavoriteHeaderView(favoriteHeader);

                                    return favouritesSongsObservable
                                            .map(songs -> {
                                                List<AdaptableItem> items = new ArrayList<>();
                                                if (!songs.isEmpty()) {
                                                    // items.add(favoriteHeaderView);
                                                    // items.add(favoriteRecyclerView);
                                                }
                                                return items;
                                            });
                                })
                                .switchIfEmpty(Observable.just(Collections.emptyList()));
                        */
                        comSubscription.add(favouritesSongsObservable
                                .map(songs -> Stream.of(songs)
                                        // HI_RES .map(song -> (AdaptableItem) new FavoriteSongView(song, requestManager))
                                        .map(song -> (AdaptableItem) new SongView(song, multiSelector, requestManager))
                                        .limit(20)
                                        .collect(Collectors.toList()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(adaptableItems -> {
                                    // HI_RES favoriteRecyclerView.itemAdapter.setItems(adaptableItems);
                                    favoriteAdapter.setItems(adaptableItems);
                                }));
                    }
                }
        );
    }

    @Override
    public void onPause() {
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
        }

        if (subscription != null) {
            subscription.unsubscribe();
        }
        if (comSubscription != null) {
            comSubscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if( !HI_RES ) {
            inflater.inflate(R.menu.menu_sort_songs, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if( HI_RES ) {
            MenuItem search = menu.findItem(R.id.action_search);
            MenuItem drawer = menu.findItem(R.id.action_drawer);
            MenuItem setting = menu.findItem(R.id.action_setting);
            if( search != null )
                search.setVisible(false);
            if( drawer != null )
                drawer.setVisible(true);
            if( setting != null )
                setting.setVisible(true);
            return;
        }

        int sortOrder = SortManager.getInstance().getSongsSortOrder();

        switch (sortOrder) {
            case SortManager.SongSort.DEFAULT:
                menu.findItem(R.id.sort_default).setChecked(true);
                break;
            case SortManager.SongSort.NAME:
                menu.findItem(R.id.sort_song_name).setChecked(true);
                break;
            case SortManager.SongSort.TRACK_NUMBER:
                menu.findItem(R.id.sort_song_track_number).setChecked(true);
                break;
            case SortManager.SongSort.DURATION:
                menu.findItem(R.id.sort_song_duration).setChecked(true);
                break;
            case SortManager.SongSort.DATE:
                menu.findItem(R.id.sort_song_date).setChecked(true);
                break;
            case SortManager.SongSort.YEAR:
                menu.findItem(R.id.sort_song_year).setChecked(true);
                break;
            case SortManager.SongSort.ALBUM_NAME:
                menu.findItem(R.id.sort_song_album_name).setChecked(true);
                break;
            case SortManager.SongSort.ARTIST_NAME:
                menu.findItem(R.id.sort_song_artist_name).setChecked(true);
                break;
        }

        menu.findItem(R.id.sort_ascending).setChecked(SortManager.getInstance().getSongsAscending());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sort_default:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.DEFAULT);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_name:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.NAME);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_track_number:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.TRACK_NUMBER);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_duration:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.DURATION);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_year:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.YEAR);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_date:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.DATE);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_album_name:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.ALBUM_NAME);
                sortOrderChanged = true;
                break;
            case R.id.sort_song_artist_name:
                SortManager.getInstance().setSongsSortOrder(SortManager.SongSort.ARTIST_NAME);
                sortOrderChanged = true;
                break;
            case R.id.sort_ascending:
                SortManager.getInstance().setSongsAscending(!item.isChecked());
                sortOrderChanged = true;
                break;
            // HI_RES
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
            // END HI_RES
        }

        if (sortOrderChanged) {
            refreshAdapterItems();
            getActivity().supportInvalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(View v, int position, Song song) {
        if (inActionMode) {
            multiSelector.setSelected(position, favoriteAdapter.getItemId(position), !multiSelector.isSelected(position, favoriteAdapter.getItemId(position)));

            if (multiSelector.getSelectedPositions().size() == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                }
            }

            updateActionModeSelectionCount();
        } else {
            List<Song> songs = Stream.of(favoriteAdapter.items)
                    .filter(adaptableItem -> adaptableItem instanceof SongView)
                    .map(adaptableItem -> ((SongView) adaptableItem).song)
                    .collect(Collectors.toList());

            int pos = songs.indexOf(song);

            MusicUtils.playAll(songs, pos, () -> {
                final String message = getContext().getString(R.string.emptyplaylist);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onOverflowClick(View v, int position, final Song song) {
        PopupMenu menu = new PopupMenu(FavoriteFragment.this.getActivity(), v);
        MenuUtils.addSongMenuOptions(getActivity(), menu);
        MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, song, item -> {
            switch (item.getItemId()) {
                case BLACKLIST: {
                    BlacklistHelper.addToBlacklist(song);
                    return true;
                }
            }
            return false;
        });
        menu.show();
    }

    @Override
    public void onLongClick(View v, int position, Song song) {
        if (inActionMode) {
            return;
        }

        if (multiSelector.getSelectedPositions().size() == 0) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            inActionMode = true;
        }

        multiSelector.setSelected(position, favoriteAdapter.getItemId(position), !multiSelector.isSelected(position, favoriteAdapter.getItemId(position)));

        updateActionModeSelectionCount();
    }

    @Override
    public void onShuffleClick() {
        MusicUtils.shuffleAll(getContext());
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        //Nothing to do.
    }

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
            getActivity().getMenuInflater().inflate(R.menu.context_menu_favor, menu);
            SubMenu sub = menu.getItem(0).getSubMenu();
            PlaylistUtils.makePlaylistMenu(getActivity(), sub, SONG_FRAGMENT_GROUP_ID);
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

            final List<Song> checkedSongs = getCheckedSongs();

            if (checkedSongs == null || checkedSongs.size() == 0) {
                if(item.getItemId() == R.id.menu_view_as) {
                    toggleViewAs();
                } else if(item.getItemId() == R.id.menu_cancel) {
                    if( actionMode != null )
                        actionMode.finish();
                }
                return true;
            }

            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    PlaylistUtils.createPlaylistDialog(getActivity(), checkedSongs);
                    break;
                case PLAYLIST_SELECTED:
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(getContext(), playlist, checkedSongs);
                    break;
                case R.id.delete:
                    new DialogUtils.DeleteDialogBuilder()
                            .context(getContext())
                            .singleMessageId(R.string.delete_song_desc)
                            .multipleMessage(R.string.delete_song_desc_multiple)
                            .itemNames(Stream.of(checkedSongs)
                                    .map(song -> song.name)
                                    .collect(Collectors.toList()))
                            .songsToDelete(Observable.just(checkedSongs))
                            .build()
                            .show();
                    mode.finish();
                    break;
                case R.id.menu_add_to_queue:
                    MusicUtils.addToQueue(FavoriteFragment.this.getActivity(), checkedSongs);
                    break;
                case R.id.menu_cancel:
                    if (actionMode != null)
                        actionMode.finish();
                    break;
                case R.id.menu_view_as:
                    toggleViewAs();
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            FavoriteFragment.this.actionMode = null;
            multiSelector.clearSelections();

            if( HI_RES ) {
                dummyStatusBar.setVisibility(View.VISIBLE);
                ((AppCompatActivity) getActivity()).getSupportActionBar().show();
            }
        }
    };

    private void toggleViewAs() {
        int viewType = SettingsManager.getInstance().getFavoriteDisplayType();
        if( viewType == ViewType.FAVORITE_CARD ) {
            SettingsManager.getInstance().setFavoriteDisplayType(ViewType.FAVORITE_LIST);
            layoutManager.setSpanCount(getResources().getInteger(R.integer.list_num_columns));
            favoriteAdapter.updateItemViewType();
            favoriteAdapter.notifyItemRangeChanged(0, favoriteAdapter.getItemCount());
            actionMode.getMenu().findItem(R.id.menu_view_as).setIcon(R.drawable.view_type_1_btn);
        } else {
            SettingsManager.getInstance().setFavoriteDisplayType(ViewType.FAVORITE_CARD);
            layoutManager.setSpanCount(SettingsManager.getInstance().getFavoriteColumnCount(getResources()));
            favoriteAdapter.updateItemViewType();
            favoriteAdapter.notifyItemRangeChanged(0, favoriteAdapter.getItemCount());
            actionMode.getMenu().findItem(R.id.menu_view_as).setIcon(R.drawable.view_type_2_btn);
        }
        Log.d("toggleViewAs", "viewType" + viewType );
    }

    List<Song> getCheckedSongs() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> favoriteAdapter.getSong(i))
                .collect(Collectors.toList());
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            favoriteAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}