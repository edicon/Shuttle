package com.simplecity.amp_library.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.Album;
import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.model.FavoriteHeader;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.adapters.FavoriteAdapter;
import com.simplecity.amp_library.ui.adapters.ItemAdapter;
import com.simplecity.amp_library.ui.modelviews.AlbumView;
import com.simplecity.amp_library.ui.modelviews.EmptyView;
import com.simplecity.amp_library.ui.modelviews.FavoriteHeaderView;
import com.simplecity.amp_library.ui.modelviews.FavoriteSongView;
import com.simplecity.amp_library.ui.modelviews.HorizontalRecyclerView;
import com.simplecity.amp_library.ui.views.FavoriteDividerDecoration;
// import com.simplecity.amp_library.model.SuggestedHeader;
// import com.simplecity.amp_library.ui.modelviews.SuggestedHeaderView;
// import com.simplecity.amp_library.ui.modelviews.SuggestedSongView;
// import com.simplecity.amp_library.ui.views.SuggestedDividerDecoration;
import com.simplecity.amp_library.ui.modelviews.ViewType;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ComparisonUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.Operators;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.simplecity.amp_library.ShuttleApplication.HI_RES;

public class FavoriteFragment extends BaseFragment implements
        MusicUtils.Defs,
        FavoriteAdapter.FavoriteListener,
        RecyclerView.RecyclerListener,
        HorizontalRecyclerView.HorizontalAdapter.ItemListener {

    public interface FavoriteClickListener {

        void onItemClicked(Serializable item, View transitionView);
    }

    private static final String TAG = "FavoriteFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private RecyclerView recyclerView;

    FavoriteAdapter favoriteAdapter;

    private BroadcastReceiver mReceiver;

    private SharedPreferences mPrefs;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    private CompositeSubscription subscription;

    private RequestManager requestManager;

    private HorizontalRecyclerView favoriteRecyclerView;
    private HorizontalRecyclerView mostPlayedRecyclerView;

    private FavoriteClickListener favoriteClickListener;

    // HI_RES
    private Toolbar toolbar;
    private View dummyToolbar;
    private View dummyStatusBar;

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
    public void onAttach(Context context) {
        super.onAttach(context);

        favoriteClickListener = (FavoriteClickListener) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        favoriteClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                themeUIComponents();
            } else if (key.equals("albumWhitelist")) {
                refreshAdapterItems();
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        favoriteAdapter = new FavoriteAdapter();
        favoriteAdapter.setListener(this);

        if (requestManager == null) {
            requestManager = Glide.with(this);
        }

        mostPlayedRecyclerView = new HorizontalRecyclerView();
        mostPlayedRecyclerView.setListener(this);

        favoriteRecyclerView = new HorizontalRecyclerView();
        favoriteRecyclerView.setListener(this);
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

        if (recyclerView == null) {

            if( HI_RES )
                recyclerView = (FastScrollRecyclerView) rootView.findViewById(R.id.fragment_recycler);
            else
                recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_favorite, container, false);
            recyclerView.addItemDecoration(new FavoriteDividerDecoration(getResources()));
            recyclerView.setAdapter(favoriteAdapter);
            recyclerView.setRecyclerListener(this);

            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (!favoriteAdapter.items.isEmpty() && position >= 0) {
                        AdaptableItem item = favoriteAdapter.items.get(position);
                        if (item instanceof HorizontalRecyclerView
                                || item instanceof FavoriteHeaderView
                                || (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_LIST)
                                || (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_LIST_SMALL)
                                || item instanceof EmptyView) {
                            return 6;
                        }
                        if (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_CARD_LARGE) {
                            return 3;
                        }
                    }

                    return 2;
                }
            });

            // ToDo: Check Linear/Grid Layout and Menu
            recyclerView.setLayoutManager(gridLayoutManager);

            themeUIComponents();
        }

        if( HI_RES )
            return rootView;
        return recyclerView;
    }

    private void themeUIComponents() {
        if (recyclerView != null) {
            ThemeUtils.themeRecyclerView(recyclerView);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    ThemeUtils.themeRecyclerView(recyclerView);
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
        }
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

                subscription = new CompositeSubscription();

                Observable<Playlist> mostPlayedPlaylistObservable = Observable.fromCallable(Playlist::mostPlayedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .cache();

                Observable<List<Song>> mostPlayedSongsObservable = mostPlayedPlaylistObservable
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> playlist.getSongsObservable(getContext()))
                        .cache();

                Observable<List<AdaptableItem>> mostPlayedItemsObservable = mostPlayedPlaylistObservable
                        .flatMap(playlist -> {

                            FavoriteHeader mostPlayedHeader = new FavoriteHeader(getString(R.string.mostplayed), getString(R.string.suggested_most_played_songs_subtitle), playlist);
                            FavoriteHeaderView mostPlayedHeaderView = new FavoriteHeaderView(mostPlayedHeader);

                            return mostPlayedSongsObservable
                                    .map(songs -> {
                                        List<AdaptableItem> items = new ArrayList<>();
                                        if (!songs.isEmpty()) {
                                            items.add(mostPlayedHeaderView);
                                            items.add(mostPlayedRecyclerView);
                                        }
                                        return items;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<List<AdaptableItem>> recentlyPlayedAlbums = Observable.fromCallable(Playlist::recentlyPlayedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> {

                            FavoriteHeader recentlyPlayedHeader = new FavoriteHeader(getString(R.string.suggested_recent_title), getString(R.string.suggested_recent_subtitle), playlist);
                            FavoriteHeaderView recentlyPlayedHeaderView = new FavoriteHeaderView(recentlyPlayedHeader);

                            return playlist.getSongsObservable(getContext())
                                    .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                                    .flatMap(Observable::from)
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                                    .limit(6)
                                    .flatMap(album ->
                                            //We need to populate the song count
                                            album.getSongsObservable()
                                                    .map(songs -> {
                                                        album.numSongs = songs.size();
                                                        return album;
                                                    }))
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                                    .filter(album -> album.numSongs > 0)
                                    .map(album -> (AdaptableItem) new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager))
                                    .toList()
                                    .map(adaptableItems -> {
                                        if (!adaptableItems.isEmpty()) {
                                            adaptableItems.add(0, recentlyPlayedHeaderView);
                                        }
                                        return adaptableItems;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<Playlist> favouritesPlaylistObservable = Observable.fromCallable(Playlist::favoritesPlaylist)
                        .subscribeOn(Schedulers.io())
                        .cache();

                Observable<List<Song>> favouritesSongsObservable = favouritesPlaylistObservable
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> playlist.getSongsObservable(getContext()))
                        .cache();

                Observable<List<AdaptableItem>> favoriteSongsItemsObservable = favouritesPlaylistObservable
                        .flatMap(playlist -> {

                            FavoriteHeader favoriteHeader = new FavoriteHeader(getString(R.string.fav_title), getString(R.string.suggested_favorite_subtitle), playlist);
                            FavoriteHeaderView favoriteHeaderView = new FavoriteHeaderView(favoriteHeader);

                            return favouritesSongsObservable
                                    .map(songs -> {
                                        List<AdaptableItem> items = new ArrayList<>();
                                        if (!songs.isEmpty()) {
                                            items.add(favoriteHeaderView);
                                            items.add(favoriteRecyclerView);
                                        }
                                        return items;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable<List<AdaptableItem>> recentlyAddedAlbums = Observable.fromCallable(Playlist::recentlyAddedPlaylist)
                        .subscribeOn(Schedulers.io())
                        .filter(playlist -> playlist != null)
                        .flatMap(playlist -> {

                            FavoriteHeader recentlyAddedHeader = new FavoriteHeader(getString(R.string.recentlyadded), getString(R.string.suggested_recently_added_subtitle), playlist);
                            FavoriteHeaderView recentlyAddedHeaderView = new FavoriteHeaderView(recentlyAddedHeader);

                            return playlist.getSongsObservable(getContext())
                                    .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                                    .flatMap(Observable::from)
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.dateAdded, a.dateAdded))
                                    .limit(4)
                                    .flatMap(album ->
                                            //We need to populate the song count
                                            album.getSongsObservable()
                                                    .map(songs -> {
                                                        album.numSongs = songs.size();
                                                        return album;
                                                    }))
                                    .sorted((a, b) -> ComparisonUtils.compareLong(b.dateAdded, a.dateAdded))
                                    .filter(album -> album.numSongs > 0)
                                    .map(album -> (AdaptableItem) new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager))
                                    .toList()
                                    .map(adaptableItems -> {
                                        if (!adaptableItems.isEmpty()) {
                                            adaptableItems.add(0, recentlyAddedHeaderView);
                                        }
                                        return adaptableItems;
                                    });
                        })
                        .switchIfEmpty(Observable.just(Collections.emptyList()));

                Observable.merge(mostPlayedItemsObservable, recentlyPlayedAlbums, favoriteSongsItemsObservable, recentlyAddedAlbums);

                subscription.add(
                        Observable.combineLatest(mostPlayedItemsObservable, recentlyPlayedAlbums, favoriteSongsItemsObservable, recentlyAddedAlbums,
                                (mostPlayedSongs1, recentlyPlayedAlbums1, favoriteSongs1, recentlyAddedAlbums1) -> {
                                    List<AdaptableItem> items = new ArrayList<>();
                                    if( HI_RES ) {
                                        // items.addAll(mostPlayedSongs1);
                                        // items.addAll(recentlyPlayedAlbums1);
                                        // items.addAll(recentlyAddedAlbums1);
                                    }
                                    items.addAll(favoriteSongs1);
                                    return items;
                                })
                                .debounce(250, TimeUnit.MILLISECONDS)
                                .switchIfEmpty(Observable.just(new ArrayList<>()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(adaptableItems -> {
                                    if (adaptableItems.isEmpty()) {
                                        favoriteAdapter.setEmpty(new EmptyView(R.string.empty_suggested));
                                    } else {
                                        favoriteAdapter.setItems(adaptableItems);
                                    }
                                }));

                subscription.add(mostPlayedSongsObservable
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.playCount, a.playCount));
                            return Stream.of(songs)
                                    .map(song -> (AdaptableItem) new FavoriteSongView(song, requestManager))
                                    .limit(20)
                                    .collect(Collectors.toList());
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            mostPlayedRecyclerView.itemAdapter.setItems(adaptableItems);
                        }));

                subscription.add(favouritesSongsObservable
                        .map(songs -> Stream.of(songs)
                                .map(song -> (AdaptableItem) new FavoriteSongView(song, requestManager))
                                .limit(20)
                                .collect(Collectors.toList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            favoriteRecyclerView.itemAdapter.setItems(adaptableItems);
                        }));
            }
        });
    }

    @Override
    public void onPause() {
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
        }

        if (subscription != null) {
            subscription.unsubscribe();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            favoriteAdapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }

    @Override
    public void onItemClick(ItemAdapter adapter, View v, int position, final Object item) {
        if (item instanceof Song) {

            Observable<List<Song>> songsObservable;
            if (adapter instanceof HorizontalRecyclerView.HorizontalAdapter) {
                //The user tapped a song belonging to a HorizontalRecyclerView adapter. Play it amongst the
                //other songs within that adapter.
                songsObservable = Observable.fromCallable(() ->
                        Stream.of(((HorizontalRecyclerView.HorizontalAdapter) adapter).items)
                                .map(adaptableItem -> (Song) adaptableItem.getItem())
                                .collect(Collectors.toList()));
            } else {
                //Otherwise, play the song amongst other songs from the same album
                songsObservable = ((Song) item).getAlbum()
                        .getSongsObservable()
                        .map(songs -> {
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                            Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                            return songs;
                        });
            }

            songsObservable.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(songs -> MusicUtils.playAll(songs, songs.indexOf((Song) item), () -> {
                        final String message = getContext().getString(R.string.emptyplaylist);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }));
        } else {
            Object model = item;
            if (favoriteClickListener != null) {
                if (item instanceof FavoriteHeader) {
                    model = ((FavoriteHeader) item).playlist;
                }
                favoriteClickListener.onItemClicked((Serializable) model, v.findViewById(R.id.image));
            }
        }
    }

    @Override
    public void onOverflowClick(View v, int position, Object item) {
        if (item instanceof AlbumArtist) {
            PopupMenu menu = new PopupMenu(FavoriteFragment.this.getActivity(), v);
            MenuUtils.addAlbumArtistMenuOptions(getActivity(), menu);
            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (AlbumArtist) item);
            menu.show();
        } else if (item instanceof Album) {
            PopupMenu menu = new PopupMenu(FavoriteFragment.this.getActivity(), v);
            MenuUtils.addAlbumMenuOptions(getActivity(), menu);
            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Album) item);
            menu.show();
        } else if (item instanceof Song) {
            PopupMenu menu = new PopupMenu(FavoriteFragment.this.getActivity(), v);
            MenuUtils.addSongMenuOptions(getActivity(), menu);
            MenuUtils.addClickHandler((AppCompatActivity) getActivity(), menu, (Song) item, menuItem -> {
                switch (menuItem.getItemId()) {
                    case BLACKLIST: {
                        BlacklistHelper.addToBlacklist(((Song) item));
                        favoriteAdapter.removeItem(position);
                        return true;
                    }
                }
                return false;
            });
            menu.show();
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
