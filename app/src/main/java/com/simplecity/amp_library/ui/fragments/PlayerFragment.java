package com.simplecity.amp_library.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.lyrics.LyricsFragment;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.sql.databases.BlacklistHelper;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.IndiUtils;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.ThemeUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import static com.simplecity.amp_library.ShuttleApplication.HI_RES;
import static com.simplecity.amp_library.ui.hires.MainActivity.mDrawerLayout;
import static com.simplecity.amp_library.utils.MusicUtils.Defs.BLACKLIST;

public class PlayerFragment extends BaseFragment implements PlayerView {

    private final String TAG = ((Object) this).getClass().getSimpleName();

    private SizableSeekBar seekBar;
    private boolean isSeeking;

    private PlayPauseView playPauseView;
    private ImageButton playPauseButton;

    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private ImageButton drawerButton;
    private ImageButton favorButton;
    private ImageButton subRepeatBtn, subSuffleBtn, subPlaylistBtn, subLylicBtn, subFileBtn, subDeleteBtn;


    private RepeatingImageButton nextButton;
    private RepeatingImageButton prevButton;

    FloatingActionButton fab;

    private TextView artist;
    private TextView album;
    private TextView track;
    private TextView currentTime;
    private TextView totalTime;
    private TextView queuePosition, bitrate;

    private View textViewContainer, seekinfoContainer;
    private View buttonContainer;

    private View bottomView;

    private SharedPreferences sharedPreferences;

    private static final String QUEUE_FRAGMENT = "queue_fragment";
    private static final String QUEUE_PAGER_FRAGMENT = "queue_pager_fragment";
    private static final String LYRICS_FRAGMENT = "lyrics_fragment";

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    boolean fabIsAnimating = false;

    private View dragView;

    private CompositeSubscription subscriptions;

    private PlayerPresenter presenter = new PlayerPresenter();

    // HI_RES
    private Toolbar toolbar;
    private View dummyToolbar;
    private View indiBar;
    private View dummyStatusBar;
    private RequestManager requestManager;
    private LinearLayout subMenuContainer;
    private ActionBar actionBar;

    public PlayerFragment() {
    }

    public static PlayerFragment newInstance() {
        PlayerFragment playerFragment = new PlayerFragment();
        Bundle args = new Bundle();
        playerFragment.setArguments(args);
        return playerFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        if( HI_RES ) {
            if (requestManager == null) {
                requestManager = Glide.with(this);
            }
            actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            initAnimation(getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);
        // ToDo: HI_RES
        if( HI_RES ) {
            rootView = inflater.inflate(R.layout.fragment_player_hires, container, false);
            seekinfoContainer = rootView.findViewById(R.id.seekBarContainer);
        }

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("");
        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        dummyToolbar = rootView.findViewById(R.id.dummyToolbar);
        indiBar = getActivity().findViewById(R.id.dummyIndiStatusBar);      // MainActivity
        dummyStatusBar = rootView.findViewById(R.id.dummyStatusBar);
        subMenuContainer  = (LinearLayout)rootView.findViewById(R.id.subMenuContainer);

        //We need to set the dummy status bar height.
        if (ShuttleUtils.hasKitKat()) {
            // ToDo: ERROR: Sliding..Exception
            // LinearLayout.LayoutParams statusBarParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) ActionBarUtils.getStatusBarHeight(getActivity()));
            // dummyStatusBar.setLayoutParams(statusBarParams);
        } else {
            dummyStatusBar.setVisibility(View.GONE);
        }

        if (getParentFragment() == null || !(getParentFragment() instanceof PlayerFragment )) {
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

        bottomView = rootView.findViewById(R.id.bottom_view);

        if( HI_RES ) {
            playPauseButton = (ImageButton) rootView.findViewById(R.id.btn_play);
            playPauseButton.setOnClickListener(v -> {
                // playPauseView.toggle();
                togglePlayButton();
                playPauseButton.postDelayed(() -> presenter.togglePlayback(), 200);
            });
        } else {
            playPauseView = (PlayPauseView) rootView.findViewById(R.id.play);
            playPauseView.setOnClickListener(v -> {
                playPauseView.toggle();
                playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
            });
        }

        if( HI_RES ) {
            favorButton = (ImageButton) rootView.findViewById(R.id.favor);
            favorButton.setOnClickListener(v -> toggleFavorite( getActivity() ));

            drawerButton = (ImageButton) rootView.findViewById(R.id.drawer);
            drawerButton.setOnClickListener(v -> toggleDrawerMenu( false ));


            subRepeatBtn = (ImageButton) rootView.findViewById(R.id.sub_repeat);
            subRepeatBtn.setOnClickListener(v -> presenter.toggleRepeat());

            subSuffleBtn = (ImageButton) rootView.findViewById(R.id.sub_shuffle);
            subSuffleBtn.setOnClickListener(v -> presenter.toggleShuffle());

            subPlaylistBtn = (ImageButton) rootView.findViewById(R.id.sub_playlist);
            subPlaylistBtn.setOnClickListener(v -> togglePlaylist(getActivity(), v));

            subLylicBtn = (ImageButton) rootView.findViewById(R.id.sub_lylic);
            subLylicBtn.setOnClickListener(v -> toggleLylic((AppCompatActivity)getActivity()));

            subFileBtn = (ImageButton) rootView.findViewById(R.id.sub_file);
            subFileBtn.setOnClickListener(v -> toggleFile(getActivity()));

            subDeleteBtn = (ImageButton) rootView.findViewById(R.id.sub_delete);
            subDeleteBtn.setOnClickListener(v -> toggleDelete(getActivity()));

        }

        repeatButton = (ImageButton) rootView.findViewById(R.id.repeat);
        repeatButton.setOnClickListener(v -> presenter.toggleRepeat());

        shuffleButton = (ImageButton) rootView.findViewById(R.id.shuffle);
        shuffleButton.setOnClickListener(v -> presenter.toggleShuffle());

        nextButton = (RepeatingImageButton) rootView.findViewById(R.id.next);
        nextButton.setOnClickListener(v -> presenter.skip());
        nextButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanForward(repeatcount, duration));

        prevButton = (RepeatingImageButton) rootView.findViewById(R.id.prev);
        prevButton.setOnClickListener(v -> presenter.prev(true));
        prevButton.setRepeatListener((v, duration, repeatcount) -> presenter.scanBackward(repeatcount, duration));

        currentTime = (TextView) rootView.findViewById(R.id.current_time);
        totalTime = (TextView) rootView.findViewById(R.id.total_time);
        queuePosition = (TextView) rootView.findViewById(R.id.queue_position);
        bitrate = (TextView) rootView.findViewById(R.id.bitrate);

        track = (TextView) rootView.findViewById(R.id.text1);
        album = (TextView) rootView.findViewById(R.id.text2);
        artist = (TextView) rootView.findViewById(R.id.text3);

        textViewContainer = rootView.findViewById(R.id.textContainer);
        buttonContainer = rootView.findViewById(R.id.button_container);

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                if (fabIsAnimating) {
                    return;
                }
                toggleQueue();
            });
        }

        seekBar = (SizableSeekBar) rootView.findViewById(R.id.seekbar);
        seekBar.setMax(1000);

        themeUIComponents();

        //If the queueFragment exists in the child fragment manager, retrieve it
        Fragment queueFragment = getChildFragmentManager().findFragmentByTag(QUEUE_FRAGMENT);

        Fragment queuePagerFragment = getChildFragmentManager().findFragmentByTag(QUEUE_PAGER_FRAGMENT);
        //We only want to add th
        if (queueFragment == null && queuePagerFragment == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.main_container, QueuePagerFragment.newInstance(), QUEUE_PAGER_FRAGMENT)
                    .commit();
        }

        toggleFabVisibility(queueFragment == null, false);

        return rootView;
    }
    private void togglePlayButton() {
        if( MusicUtils.isPlaying() )
            playPauseButton.setImageResource(R.drawable.btn_player_play);
        else
            playPauseButton.setImageResource(R.drawable.btn_player_pause);
        IndiUtils.updatePlay(!MusicUtils.isPlaying());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        presenter.unbindView(this);
    }

    public void update() {
        if (presenter != null) {
            presenter.updateTrackInfo();
        }
    }

    public void themeUIComponents() {

        if( HI_RES ) {
            dummyStatusBar.setVisibility(View.VISIBLE);
            return; // skip theme
        }

        if ( HI_RES ) {
            if (seekinfoContainer != null) {
                seekinfoContainer.setBackgroundColor(ColorUtils.getPrimaryColorDark(getActivity()));
            }
        }
        if (nextButton != null) {
            nextButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(getActivity(), nextButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (prevButton != null) {
            prevButton.setImageDrawable(DrawableUtils.getColoredStateListDrawableWithThemeColor(getActivity(), prevButton.getDrawable(), ThemeUtils.WHITE));
        }
        if (seekBar != null) {
            ThemeUtils.themeSeekBar(getActivity(), seekBar, true);
        }
        if (textViewContainer != null) {
            textViewContainer.setBackgroundColor(ColorUtils.getPrimaryColorDark(getActivity()));
        }
        if (buttonContainer != null) {
            buttonContainer.setBackgroundColor(ColorUtils.getPrimaryColor());
        }
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.getAccentColor()));
            fab.setRippleColor(ColorUtils.darkerise(ColorUtils.getAccentColor(), 0.85f));
        }

        if (presenter != null) {
            shuffleChanged(MusicUtils.getShuffleMode());
            repeatChanged(MusicUtils.getRepeatMode());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        subscriptions = new CompositeSubscription();

        Observable<SeekBarChangeEvent> sharedSeekBarEvents = RxSeekBar.changeEvents(seekBar)
                .onBackpressureLatest()
                .ofType(SeekBarChangeEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .share();

        subscriptions.add(sharedSeekBarEvents.subscribe(seekBarChangeEvent -> {
            if (seekBarChangeEvent instanceof SeekBarStartChangeEvent) {
                isSeeking = true;
            } else if (seekBarChangeEvent instanceof SeekBarStopChangeEvent) {
                isSeeking = false;
            }
        }));

        subscriptions.add(sharedSeekBarEvents
                .ofType(SeekBarProgressChangeEvent.class)
                .filter(SeekBarProgressChangeEvent::fromUser)
                .debounce(15, TimeUnit.MILLISECONDS)
                .subscribe(seekBarChangeEvent -> presenter.seekTo(seekBarChangeEvent.progress())));
    }

    @Override
    public void onPause() {
        subscriptions.unsubscribe();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // if( HI_RES )
        //     menu.findItem(R.id.action_search).setVisible(false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem search = menu.findItem(R.id.action_search);
        MenuItem drawer = menu.findItem(R.id.action_drawer);
        MenuItem setting = menu.findItem(R.id.action_setting);
        if( search != null )
            search.setVisible(true);
        if( drawer != null )
            drawer.setVisible(false);
        if( setting != null )
            setting.setVisible(false);
    }

    public void toggleLyrics() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.main_container);
        if (fragment instanceof LyricsFragment) {
            return;
        }
        FragmentTransaction ft;
        if( !HI_RES ) // Full Screen
            ft = getActivity().getSupportFragmentManager().beginTransaction();
        else // Half Screen?
            ft = getChildFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
        if (fragment instanceof QueueFragment) {
            ft.replace(R.id.main_container, new QueuePagerFragment(), QUEUE_PAGER_FRAGMENT);
            toggleFabVisibility(true, true);
        }
        ft.add(R.id.main_container, new LyricsFragment(), LYRICS_FRAGMENT);
        if( HI_RES )
            ft.addToBackStack(null);
        ft.commit();
    }

    public void toggleQueue() {

        Fragment lyricsFragment = getChildFragmentManager().findFragmentByTag(LYRICS_FRAGMENT);
        Fragment queueFragment = getChildFragmentManager().findFragmentByTag(QUEUE_FRAGMENT);
        Fragment queuePagerFragment = getChildFragmentManager().findFragmentByTag(QUEUE_PAGER_FRAGMENT);

        final FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
        //Remove the lyrics fragment

        if (lyricsFragment != null) {
            fragmentTransaction.remove(lyricsFragment);
        }

        if (queueFragment != null) {
            fragmentTransaction.remove(queueFragment);
            fragmentTransaction.replace(R.id.main_container, new QueuePagerFragment(), QUEUE_PAGER_FRAGMENT);
            toggleFabVisibility(true, true);

        } else if (queuePagerFragment != null) {
            fragmentTransaction.remove(queuePagerFragment);
            fragmentTransaction.add(R.id.queue_container, QueueFragment.newInstance(), QUEUE_FRAGMENT);
            bottomView.setClickable(true);
            toggleFabVisibility(false, true);
        }

        fragmentTransaction.commitAllowingStateLoss();
    }

    private void toggleFabVisibility(boolean show, boolean animate) {
        if (fab == null) {
            return;
        }
        if( HI_RES ) {
            fab.setVisibility(View.GONE);
            return;
        }

        if (show && fab.getVisibility() == View.VISIBLE) {
            return;
        }

        if (!show && fab.getVisibility() == View.GONE) {
            return;
        }

        if (fabIsAnimating) {
            return;
        }

        if (!animate) {
            if (show) {
                fab.setVisibility(View.VISIBLE);
            } else {
                fab.setVisibility(View.GONE);
            }
            return;
        }

        fabIsAnimating = true;

        if (show) {

            fab.setScaleX(0f);
            fab.setScaleY(0f);
            fab.setAlpha(0f);
            fab.setVisibility(View.VISIBLE);

            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f);
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 0f, 1f);
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 0f, 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
            animatorSet.setDuration(350);
            animatorSet.start();

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    fabIsAnimating = false;
                }
            });

        } else {
            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f);
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f);
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(fadeAnimator, scaleXAnimator, scaleYAnimator);
            animatorSet.setDuration(250);
            animatorSet.start();

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    fab.setVisibility(View.GONE);
                    fabIsAnimating = false;
                }
            });
        }
    }

    public void setDragView(View view) {
        dragView = view;
        if( HI_RES )
            ((com.simplecity.amp_library.ui.hires.MainActivity) getActivity()).setDragView(view, true);
        else
            ((com.simplecity.amp_library.ui.activities.MainActivity) getActivity()).setDragView(view, true);
    }

    public View getDragView() {
        return dragView;
    }

    public boolean isQueueShowing() {
        return getChildFragmentManager().findFragmentByTag(QUEUE_FRAGMENT) != null;
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    ////////////////////////////////////////////////////////////////////
    // View implementation
    ////////////////////////////////////////////////////////////////////

    @Override
    public void setSeekProgress(int progress) {
        if (!isSeeking) {
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void currentTimeVisibilityChanged(boolean visible) {
        currentTime.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void currentTimeChanged(long seconds) {
        currentTime.setText(StringUtils.makeTimeString(this.getActivity(), seconds));
    }

    @Override
    public void queueChanged(int queuePosition, int queueLength) {
        this.queuePosition.setText(String.format("%s / %s", queuePosition, queueLength));
    }

    @Override
    public void playbackChanged(boolean isPlaying) {
        if( HI_RES ) {
            if( isPlaying ) {
                playPauseButton.setImageResource(R.drawable.btn_player_pause);
                playPauseButton.setContentDescription(getString(R.string.btn_pause));
            } else {
                playPauseButton.setImageResource(R.drawable.btn_player_play);
                playPauseButton.setContentDescription(getString(R.string.btn_play));
            }
            return;
        }
        if (isPlaying) {
            if (playPauseView.isPlay()) {
                playPauseView.toggle();
                playPauseView.setContentDescription(getString(R.string.btn_pause));
            }
        } else {
            if (!playPauseView.isPlay()) {
                playPauseView.toggle();
                playPauseView.setContentDescription(getString(R.string.btn_play));
            }
        }
    }

    @Override
    public void shuffleChanged(@MusicService.ShuffleMode int shuffleMode) {
        switch (MusicUtils.getShuffleMode()) {
            case MusicService.ShuffleMode.OFF:
                shuffleButton.setImageDrawable(DrawableUtils.getWhiteDrawable(getActivity(), R.drawable.ic_shuffle_white));
                shuffleButton.setContentDescription(getString(R.string.btn_shuffle_off));
                break;
            case MusicService.ShuffleMode.ON:
                shuffleButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_shuffle_white)));
                shuffleButton.setContentDescription(getString(R.string.btn_shuffle_on));
                break;
        }
    }

    @Override
    public void repeatChanged(@MusicService.RepeatMode int repeatMode) {
        switch (MusicUtils.getRepeatMode()) {
            case MusicService.RepeatMode.ALL:
                repeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_repeat_white)));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_all));
                break;
            case MusicService.RepeatMode.ONE:
                repeatButton.setImageDrawable(DrawableUtils.getColoredAccentDrawableNonWhite(getActivity(), getResources().getDrawable(R.drawable.ic_repeat_one_white)));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_current));
                break;
            case MusicService.RepeatMode.OFF:
                repeatButton.setImageDrawable(DrawableUtils.getWhiteDrawable(getActivity(), R.drawable.ic_repeat_white));
                repeatButton.setContentDescription(getResources().getString(R.string.btn_repeat_off));
                break;
        }
    }

    @Override
    public void favoriteChanged() {
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void trackInfoChanged(@Nullable Song song) {

        if (song == null) return;

        String totalTime = StringUtils.makeTimeString(this.getActivity(), song.duration / 1000);
        if (!TextUtils.isEmpty(totalTime)) {
            if( HI_RES ) {
                String bitRate = song.getBitrateLabel();
                String sampleRate = song.getSampleRateLabel();
                if( bitRate.contains("null"))
                    bitRate = bitRate.replace("null", "");
                if( sampleRate.contains(".0"))
                    sampleRate = sampleRate.replace(".0", "");
                this.bitrate.setText(String.format("%s / %s",  bitRate, sampleRate));
                this.totalTime.setText(String.format("%s", totalTime));
            } else {
                this.totalTime.setText(String.format(" / %s", totalTime));
            }
        }

        track.setText(song.name);
        track.setSelected(true);
        album.setText(String.format("%s | %s", song.artistName, song.albumName));
    }

    // HI_RES
    @Override
    public void menuChanged( long time ) {
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (fragment instanceof PlayerFragment) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar.isShowing()) {
                time = System.currentTimeMillis();
                if ((time - toggleTime) > 5000)
                    toggleMenu();
            }
        }
    }

    // ToDo: HI_RES
    public static boolean actionDrawer;
    public void toggleDrawerMenu( boolean actionDrawer) {
        // MusicUtils.cycleRepeat();
        this.actionDrawer = actionDrawer;
        if (com.simplecity.amp_library.ui.hires.MainActivity.mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            // close
            mDrawerLayout.closeDrawers();
        } else {
            // open
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    public void toggleFavorite( Activity cx ) {
        PlaylistUtils.toggleFavorite( cx );
    }

    public void togglePlaylist( Activity cx, View v ) {
        /*
        // https://stackoverflow.com/questions/3720804/android-open-menu-from-a-button
        // https://stackoverflow.com/questions/16938522/how-to-get-the-android-id-for-a-menu-item-in-android
        cx.openOptionsMenu(); // activity's onCreateOptionsMenu gets called
        com.simplecity.amp_library.ui.hires.MainActivity.optionMenu.performIdentifierAction( playlistId, 0);
        cx.closeOptionsMenu();
        */
        Song song = MusicUtils.getSong();
        PopupMenu menu = new PopupMenu(cx, v);
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
        // cx.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        /* ToDo: Check
        v.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        // menu.show();
        */
    }

    public void toggleLylic(AppCompatActivity cx ) {
        Fragment playingFragment;
        if( HI_RES )
            playingFragment  = cx.getSupportFragmentManager().findFragmentById(R.id.main_container);
        else
            playingFragment  = cx.getSupportFragmentManager().findFragmentById(R.id.player_container);
        if (playingFragment != null) {
            Fragment fragment = playingFragment.getChildFragmentManager().findFragmentById(R.id.main_container);
            if (fragment instanceof LyricsFragment)
                ((LyricsFragment) fragment).remove();
            else
                ((PlayerFragment) playingFragment).toggleLyrics();

        }
    }

    private Handler mHandler = new Handler();
    public void toggleFile( Activity cx ) {
        DialogUtils.showSongInfoDialog( cx, MusicUtils.getSong());

        // ToDo: Hide SystemUI after popupwindow
        // mHandler.postDelayed(runHideSystemUI, 500);
        // hideSystemUI();
    }

    public void toggleDelete( Activity cx ) {
        new DialogUtils.DeleteDialogBuilder()
                .context(cx)
                .singleMessageId(R.string.delete_song_desc)
                .multipleMessage(R.string.delete_song_desc_multiple)
                .itemNames(Collections.singletonList(MusicUtils.getSongName()))
                .songsToDelete(Observable.just(Collections.singletonList(MusicUtils.getSong())))
                .build()
                .show();
    }

    private Runnable runHideSystemUI  = new Runnable() {
        public void run() {
            hideSystemUI();
        }
    };

    private void hideSystemUI() {
        View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    long toggleTime;
    public void toggleMenu() {
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (fragment instanceof PlayerFragment) {
            toggleTime = System.currentTimeMillis();
            toggleActionbar();
            toggleSubMenu();
        } else {
            IndiUtils.hideIndiBar(getActivity(), null );
        }
    }
    public void toggleSubMenu() {
    }

    private boolean useToggleAnimation = true;
    public void toggleActionbar() {

        if( actionBar.isShowing()) {
            if( !useToggleAnimation ) {
                actionBar.hide();
                dummyToolbar.setVisibility(View.INVISIBLE);
                indiBar.setVisibility(View.INVISIBLE);
                dummyStatusBar.setVisibility(View.INVISIBLE);
                subMenuContainer.setVisibility( View.INVISIBLE );
            } else {
                slideDownTop(dummyToolbar);
                slideDownTop(indiBar);
                slideDownTop(dummyStatusBar);
                slideDownBottom(subMenuContainer);
            }
        } else {
            if( !useToggleAnimation ) {
                actionBar.show();
                dummyToolbar.setVisibility(View.VISIBLE);
                indiBar.setVisibility(View.VISIBLE);
                dummyStatusBar.setVisibility(View.VISIBLE);
                subMenuContainer.setVisibility( View.VISIBLE );
            } else {
                slideUpTop(dummyToolbar);
                slideUpTop(indiBar);
                slideUpTop(dummyStatusBar);
                slideUpBottom(subMenuContainer);
            }
        }
    }

    public void showDummyToolbar() {
        if( dummyToolbar != null )
            dummyToolbar.setVisibility(View.VISIBLE);
    }

    private static Animation animShowTop, animHideTop;
    private static Animation animShowBottom, animHideBottom;
    private void initAnimation( Context cx ) {
        animShowTop = AnimationUtils.loadAnimation( cx, R.anim.view_show_top);
        animHideTop = AnimationUtils.loadAnimation( cx, R.anim.view_hide_top);

        animShowTop.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                actionBar.show();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animHideTop.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                actionBar.hide();
            }
            @Override
            public void onAnimationEnd(Animation animation) {
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        animShowBottom = AnimationUtils.loadAnimation( cx, R.anim.view_show_bottom);
        animHideBottom = AnimationUtils.loadAnimation( cx, R.anim.view_hide_bottom);
    }

    public static void slideUpTop( View view ) {
        view.setVisibility(View.VISIBLE);
        view.startAnimation( animShowTop );
    }
    public static void slideDownTop( View view ) {
        view.startAnimation( animHideTop );
        view.setVisibility(View.GONE);
    }

    public static void slideUpBottom( View view ) {
        view.setVisibility(View.VISIBLE);
        view.startAnimation( animShowBottom );
    }
    public static void slideDownBottom( View view ) {
        view.startAnimation( animHideBottom );
        view.setVisibility(View.GONE);
    }
}