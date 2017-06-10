package com.simplecity.amp_library.ui.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.BackPressListener;
import com.simplecity.amp_library.interfaces.Breadcrumb;
import com.simplecity.amp_library.interfaces.BreadcrumbListener;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.AdaptableItem;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.DrawerGroupItem;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.sql.databases.WhitelistHelper;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.adapters.FolderAdapter;
import com.simplecity.amp_library.ui.modelviews.BreadcrumbsView;
import com.simplecity.amp_library.ui.modelviews.FolderView;
import com.simplecity.amp_library.ui.views.BreadcrumbItem;
import com.simplecity.amp_library.ui.views.CustomEditText;
import com.simplecity.amp_library.utils.ActionBarUtils;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.CustomMediaScanner;
import com.simplecity.amp_library.utils.DialogUtils;
import com.simplecity.amp_library.utils.DrawableUtils;
import com.simplecity.amp_library.utils.FileBrowser;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.ThemeUtils;
import com.simplecity.amp_library.utils.ViewUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.simplecity.amp_library.R.styleable.Android;
import static com.simplecity.amp_library.ShuttleApplication.HI_RES;
import static com.simplecity.amp_library.utils.ColorUtils.fetchAttrColor;
import static com.simplecity.amp_library.utils.MenuUtils.changeActionModeBackground;

public class FolderFragment extends BaseFragment implements
        MusicUtils.Defs,
        BreadcrumbListener,
        BackPressListener,
        RecyclerView.RecyclerListener,
        FolderAdapter.Listener,
        View.OnClickListener
{

    private static final String TAG = "FolderFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private static final String ARG_CURRENT_DIR = "current_dir";

    static final int FRAGMENT_GROUPID = FOLDER_FRAGMENT_GROUP_ID;

    private RecyclerView recyclerView;

    FolderAdapter adapter;

    private Toolbar toolbar;

    private View dummyToolbar;
    private View dummyStatusBar;

    boolean isInActionMode = false;

    String currentDir;

    private SharedPreferences prefs;

    Breadcrumb breadcrumb;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    FileBrowser fileBrowser;

    boolean showCheckboxes;

    List<String> paths = new ArrayList<>();

    boolean showBreadcrumbsInList;

    private ActionMode actionMode;

    ActionMode.Callback actionModeCallback;
    private TextView sdInfo;
    private ImageButton sdIn, sdEx0, sdEx1;

    // HI_RES
    boolean inActionMode = false;
    MultiSelector multiSelector = new MultiSelector();

    private CompositeSubscription subscriptions;

    public FolderFragment() {
    }

    public static FolderFragment newInstance(String pageTitle) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) {
            ((MainActivity) context).setOnBackPressedListener(this);
            if (!(getParentFragment() != null && getParentFragment() instanceof MainFragment)) {
                ((MainActivity) context).onSectionAttached(getString(R.string.folders_title));
            }
        }
        // sdCardClickListener = (SdCardClickListener) getActivity();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        subscriptions = new CompositeSubscription();

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("pref_theme_highlight_color") || key.equals("pref_theme_accent_color") || key.equals("pref_theme_white_accent")) {
                themeUIComponents();
            }
        };

        setHasOptionsMenu(true);

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        adapter = new FolderAdapter();
        adapter.setListener(this);

        fileBrowser = new FileBrowser();

        if (savedInstanceState != null) {
            currentDir = savedInstanceState.getString(ARG_CURRENT_DIR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_folder_browser, container, false);

        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        dummyToolbar = rootView.findViewById(R.id.dummyToolbar);
        dummyStatusBar = rootView.findViewById(R.id.dummyStatusBar);

        sdInfo = (TextView)rootView.findViewById(R.id.sd_info);
        View sdCard = (View)rootView.findViewById(R.id.sd_card);
        sdIn = (ImageButton)rootView.findViewById(R.id.btn_sd_in);
        sdEx0 = (ImageButton)rootView.findViewById(R.id.btn_sd_ex0);
        sdEx1 = (ImageButton)rootView.findViewById(R.id.btn_sd_ex1);

        //We need to set the dummy status bar height.
        if (ShuttleUtils.hasKitKat()) {
            LinearLayout.LayoutParams statusBarParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) ActionBarUtils.getStatusBarHeight(getActivity()));
            dummyStatusBar.setLayoutParams(statusBarParams);
        } else {
            dummyStatusBar.setVisibility(View.GONE);
        }

        if (getParentFragment() == null || !(getParentFragment() instanceof PlayerFragment /*MainFragment*/)) {
            showBreadcrumbsInList = false;
            breadcrumb = (Breadcrumb) rootView.findViewById(R.id.breadcrumb_view);
            breadcrumb.setTextColor(Color.WHITE);
            breadcrumb.addBreadcrumbListener(this);
            if (!TextUtils.isEmpty(currentDir)) {
                breadcrumb.changeBreadcrumbPath(currentDir);
            }
            if (ShuttleUtils.hasKitKat()) {
                dummyStatusBar.setVisibility(View.VISIBLE);
            }
            dummyToolbar.setVisibility(View.VISIBLE);
        } else {
            showBreadcrumbsInList = true;
            changeBreadcrumbPath();
            toolbar.setVisibility(View.GONE);
            if (ShuttleUtils.hasKitKat()) {
                dummyStatusBar.setVisibility(View.GONE);
            }
            dummyToolbar.setVisibility(View.GONE);
        }

        recyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        // sdCard.setOnClickListener( this );
        sdIn.setOnClickListener( this );
        sdEx0.setOnClickListener( this );
        sdEx1.setOnClickListener( this );

        themeUIComponents();

        return rootView;
    }

    public static boolean isSdPresent() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentDir == null) {
            subscriptions.add(Observable.fromCallable(() -> {
                if (!TextUtils.isEmpty(currentDir)) {
                    return new File(currentDir);
                } else {
                    return fileBrowser.getInitialDir();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::changeDir));
        }
    }

    private void dispSdcard( File currFile ) {

        sdIn.setImageResource(R.drawable.memory_internal_off);
        sdEx0.setImageResource(R.drawable.memory_1_off);
        sdEx1.setImageResource(R.drawable.memory_2_off);

        if( currFile != null ) {
            if( currFile.exists() ) {
                String path = currFile.getAbsolutePath();
                if( BuildConfig.DEBUG)
                    Log.d("PATH", "currPath: " + path);
                if( path.contains("0") )
                    sdEx0.setImageResource(R.drawable.memory_1_on);
                else if( path.contains("1"))
                    sdEx1.setImageResource(R.drawable.memory_2_on);
                else
                    sdIn.setImageResource(R.drawable.memory_internal_on);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        subscriptions.clear();
    }

    @Override
    public void onDestroyView() {

        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        actionModeCallback = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        actionModeCallback = null;

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnBackPressedListener(null);
        }
    }

    private void themeUIComponents() {

        if( !HI_RES ) {
            if (dummyStatusBar != null) {
                //noinspection ResourceAsColor
                dummyStatusBar.setBackgroundColor(ShuttleUtils.hasLollipop() ? ColorUtils.getPrimaryColorDark(getContext()) : ColorUtils.getPrimaryColor());
            }

            if (dummyToolbar != null) {
                dummyToolbar.setBackgroundColor(ColorUtils.getPrimaryColor());
            }

            if (toolbar != null) {
                if (getParentFragment() != null && getParentFragment() instanceof MainFragment) {
                    toolbar.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    toolbar.setBackgroundColor(ColorUtils.getPrimaryColor());
                }
            }
        }


        adapter.notifyItemRangeChanged(0, adapter.getItemCount());

        ThemeUtils.themeRecyclerView(recyclerView);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ThemeUtils.themeRecyclerView(recyclerView);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_CURRENT_DIR, currentDir);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if( !HI_RES ) {
            inflater.inflate(R.menu.menu_sort_folders, menu);
            if (HI_RES) {
                menu.findItem(R.id.whitelist).setVisible(false);
                menu.findItem(R.id.show_filenames).setVisible(false);
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if( HI_RES )
            return;

        switch (SettingsManager.getInstance().getFolderBrowserFilesSortOrder()) {
            case SortManager.SortFiles.DEFAULT:
                menu.findItem(R.id.sort_files_default).setChecked(true);
                break;
            case SortManager.SortFiles.FILE_NAME:
                menu.findItem(R.id.sort_files_filename).setChecked(true);
                break;
            case SortManager.SortFiles.SIZE:
                menu.findItem(R.id.sort_files_size).setChecked(true);
                break;
            case SortManager.SortFiles.ARTIST_NAME:
                menu.findItem(R.id.sort_files_artist_name).setChecked(true);
                break;
            case SortManager.SortFiles.ALBUM_NAME:
                menu.findItem(R.id.sort_files_album_name).setChecked(true);
                break;
            case SortManager.SortFiles.TRACK_NAME:
                menu.findItem(R.id.sort_files_track_name).setChecked(true);
                break;
        }

        switch (SettingsManager.getInstance().getFolderBrowserFoldersSortOrder()) {
            case SortManager.SortFolders.DEFAULT:
                menu.findItem(R.id.sort_folder_default).setChecked(true);
                break;
            case SortManager.SortFolders.COUNT:
                menu.findItem(R.id.sort_folder_count).setChecked(true);
                break;
        }

        menu.findItem(R.id.show_filenames).setChecked(SettingsManager.getInstance().getFolderBrowserShowFileNames());
        menu.findItem(R.id.files_ascending).setChecked(SettingsManager.getInstance().getFolderBrowserFilesAscending());
        menu.findItem(R.id.folders_ascending).setChecked(SettingsManager.getInstance().getFolderBrowserFoldersAscending());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_files_default:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.DEFAULT);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_filename:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.FILE_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_size:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.SIZE);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_artist_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.ARTIST_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_album_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.ALBUM_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_files_track_name:
                SettingsManager.getInstance().setFolderBrowserFilesSortOrder(SortManager.SortFiles.TRACK_NAME);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.files_ascending:
                SettingsManager.getInstance().setFolderBrowserFilesAscending(!item.isChecked());
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_folder_count:
                SettingsManager.getInstance().setFolderBrowserFoldersSortOrder(SortManager.SortFolders.COUNT);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.sort_folder_default:
                SettingsManager.getInstance().setFolderBrowserFoldersSortOrder(SortManager.SortFolders.DEFAULT);
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.folders_ascending:
                SettingsManager.getInstance().setFolderBrowserFoldersAscending(!item.isChecked());
                reload();
                getActivity().supportInvalidateOptionsMenu();
                return true;

            case R.id.whitelist:
                if( HI_RES )
                    actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(getActionModeCallback());
                else
                    actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(getActionModeCallback());
                    // ToDo: ERROR: actionMode = recyclerView.startActionMode(getActionModeCallback());
                isInActionMode = true;
                updateWhitelist();
                showCheckboxes(true);
                break;

            case R.id.show_filenames:
                SettingsManager.getInstance().setFolderBrowserShowFileNames(!item.isChecked());
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        changeDir(new File(item.getItemPath()));
    }

    @Override
    public void onCheckedChange(FolderView folderView, boolean isChecked) {

        folderView.setChecked(isChecked);

        if (isChecked) {
            if (!paths.contains(folderView.baseFileObject.path)) {
                paths.add(folderView.baseFileObject.path);
            }
        } else {
            if (paths.contains(folderView.baseFileObject.path)) {
                paths.remove(folderView.baseFileObject.path);
            }
        }
    }

    public void changeDir(File newDir) {

        if( HI_RES )
            dispSdcard( newDir );

        subscriptions.add(Observable.fromCallable(() -> {

            final String path = FileHelper.getPath(newDir);

            if (TextUtils.isEmpty(path)) {
                return new ArrayList<BaseFileObject>();
            }

            currentDir = path;

            return fileBrowser.loadDir(new File(path));
        })
                .map(baseFileObjects -> {
                    List<AdaptableItem> items = Stream.of(baseFileObjects)
                            .map(baseFileObject -> {
                                FolderView folderView;
                                if( HI_RES )
                                    folderView = new FolderView(baseFileObject, multiSelector);
                                else
                                    folderView = new FolderView(baseFileObject);
                                folderView.setChecked(showCheckboxes);
                                return folderView;
                            })
                            .collect(Collectors.toList());

                    if (showBreadcrumbsInList) {
                        BreadcrumbsView breadcrumbsView = new BreadcrumbsView(currentDir);
                        breadcrumbsView.setBreadcrumbsPath(currentDir);
                        items.add(0, breadcrumbsView);
                    }
                    return items;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adaptableItems -> {

                    if (adapter != null) {
                        adapter.setItems(adaptableItems);
                    }

                    if (breadcrumb != null) {
                        breadcrumb.changeBreadcrumbPath(currentDir);
                    }
                    if (adapter != null) {
                        changeBreadcrumbPath();
                    }
                }));
    }

    public void reload() {
        if (currentDir != null) {
            changeDir(new File(currentDir));
        }
    }

    @Override
    public boolean onBackPressed() {

        if (fileBrowser.getCurrentDir() != null && fileBrowser.getRootDir() != null && fileBrowser.getCurrentDir().compareTo(fileBrowser.getRootDir()) != 0) {
            File parent = fileBrowser.getCurrentDir().getParentFile();
            changeDir(parent);
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(View v, int position, BaseFileObject fileObject) {

        // HI_RES
        if (isInActionMode || inActionMode ) {
            if (fileObject.fileType == FileType.FILE) {
                multiSelector.setSelected(position, adapter.getItemId(position), !multiSelector.isSelected(position, adapter.getItemId(position)));

                if (multiSelector.getSelectedPositions().size() == 0) {
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                }
                updateActionModeSelectionCount();
            } else {
                changeDir(new File(fileObject.path));
                // ToDo: Folder에 적용할 지 검토
                // updateActionModeSelectionCount();
            }
        // END HI_RES
        } else {
            if (fileObject.fileType == FileType.FILE) {
                FileHelper.getSongList(new File(fileObject.path), false, true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(songs -> {
                            int index = -1;
                            for (int i = 0, songsSize = songs.size(); i < songsSize; i++) {
                                Song song = songs.get(i);
                                if (song.path.contains(fileObject.path)) {
                                    index = i;
                                    break;
                                }
                            }
                            MusicUtils.playAll(songs, index, () -> {
                                if (isAdded() && getContext() != null) {
                                    final String message = getContext().getString(R.string.emptyplaylist);
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
            } else {
                changeDir(new File(fileObject.path));
            }
        }
    }

    @Override
    public void onOverflowClick(View v, int position, BaseFileObject fileObject) {

        PopupMenu menu = new PopupMenu(getActivity(), v);

        if (fileObject.fileType == FileType.FILE) {

            //Play this song next
            menu.getMenu().add(FRAGMENT_GROUPID, PLAY_NEXT, 4, R.string.play_next);

            //Tag editor
            if (ShuttleUtils.isUpgraded()) {
                if( !HI_RES )
                    menu.getMenu().add(FRAGMENT_GROUPID, TAGGER, 5, R.string.edit_tags);
            }

            //Set this song as the ringtone
            if( !HI_RES )
                menu.getMenu().add(FRAGMENT_GROUPID, USE_AS_RINGTONE, 6, R.string.ringtone_menu);


            if (FileHelper.canReadWrite(new File(fileObject.path))) {
                //Rename File
                menu.getMenu().add(FRAGMENT_GROUPID, RENAME, 7, R.string.rename_file);
                //Delete File
                menu.getMenu().add(FRAGMENT_GROUPID, DELETE_ITEM, 8, R.string.delete_item);
            }

            menu.getMenu().add(FRAGMENT_GROUPID, VIEW_INFO, 9, R.string.song_info);

        } else {

            //Play all files in this dir
            menu.getMenu().add(FRAGMENT_GROUPID, PLAY_SELECTION, 0, R.string.play_selection);

            //Set this directory as initial directory
            menu.getMenu().add(FRAGMENT_GROUPID, SET_INITIAL_DIR, 4, R.string.set_initial_dir);

            if (FileHelper.canReadWrite(new File(fileObject.path))) {
                //Rename dir
                menu.getMenu().add(FRAGMENT_GROUPID, RENAME, 5, R.string.rename_folder);
                //Delete dir
                menu.getMenu().add(FRAGMENT_GROUPID, DELETE_ITEM, 6, R.string.delete_item);
            }
        }

        //Bring up the add to playlist menu
        SubMenu sub = menu.getMenu().addSubMenu(FRAGMENT_GROUPID, ADD_TO_PLAYLIST, 2, R.string.add_to_playlist);
        PlaylistUtils.makePlaylistMenu(getActivity(), sub, FRAGMENT_GROUPID);

        //Add to queue
        menu.getMenu().add(FRAGMENT_GROUPID, QUEUE, 3, R.string.add_to_queue);

        menu.getMenu().add(FRAGMENT_GROUPID, RESCAN, 4, R.string.scan_file);

        menu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {

                case TAGGER:
                    subscriptions.add(FileHelper.getSong(new File(fileObject.path))
                            .subscribeOn(Schedulers.io())
                            .subscribe(song -> TaggerDialog.newInstance(song).show(getFragmentManager())));
                    return true;

                case QUEUE:
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.addToQueue(getActivity(), songs)));
                    return true;

                case DELETE_ITEM:
                    MaterialDialog.Builder builder = DialogUtils.getBuilder(getActivity())
                            .title(R.string.delete_item)
                            .icon(DrawableUtils.getBlackDrawable(getActivity(), R.drawable.ic_dialog_alert));
                    if (fileObject.fileType == FileType.FILE) {
                        builder.content(String.format(getResources().getString(
                                R.string.delete_file_confirmation_dialog), fileObject.name));
                    } else {
                        builder.content(String.format(getResources().getString(
                                R.string.delete_folder_confirmation_dialog), fileObject.path));
                    }
                    builder.positiveText(R.string.button_ok)
                            .onPositive((materialDialog, dialogAction) -> {
                                if (FileHelper.deleteFile(new File(fileObject.path))) {
                                    adapter.removeItem(position);
                                    CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), null);
                                } else {
                                    Toast.makeText(getActivity(),
                                            fileObject.fileType == FileType.FOLDER ? R.string.delete_folder_failed : R.string.delete_file_failed,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                    builder.negativeText(R.string.cancel)
                            .show();
                    return true;

                case RENAME:

                    View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rename, null);
                    final CustomEditText editText = (CustomEditText) customView.findViewById(R.id.editText);
                    ThemeUtils.themeEditText(editText);
                    editText.setText(fileObject.name);

                    builder = DialogUtils.getBuilder(getActivity());
                    if (fileObject.fileType == FileType.FILE) {
                        builder.title(R.string.rename_file);
                    } else {
                        builder.title(R.string.rename_folder);
                    }

                    builder.customView(customView, false);
                    builder.positiveText(R.string.save)
                            .onPositive((materialDialog, dialogAction) -> {
                                if (editText.getText() != null) {
                                    if (FileHelper.renameFile(getActivity(), fileObject, editText.getText().toString())) {
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Toast.makeText(getActivity(),
                                                fileObject.fileType == FileType.FOLDER ? R.string.rename_folder_failed : R.string.rename_file_failed,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    builder.negativeText(R.string.cancel)
                            .show();
                    return true;
                case USE_AS_RINGTONE:
                    subscriptions.add(FileHelper.getSong(new File(fileObject.path))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(song -> ShuttleUtils.setRingtone(getContext(), song)));
                    return true;
                case PLAY_NEXT:
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), false, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> MusicUtils.playNext(getActivity(), songs)));

                    return true;
                case PLAY_SELECTION:
                    final ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "", getString(R.string.gathering_songs), false);
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, fileObject.fileType == FileType.FILE)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                MusicUtils.playAll(songs, 0, () -> {
                                    final String message = getContext().getString(R.string.emptyplaylist);
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                });

                                if (isAdded() && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            }));
                    return true;
                case NEW_PLAYLIST:
                    List<BaseFileObject> fileObjects = new ArrayList<>();
                    fileObjects.add(fileObject);
                    PlaylistUtils.createFileObjectPlaylistDialog(getActivity(), fileObjects);
                    return true;
                case PLAYLIST_SELECTED:
                    final Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                    subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> PlaylistUtils.addToPlaylist(getContext(), playlist, songs)));

                    return true;
                case SET_INITIAL_DIR:
                    SettingsManager.getInstance().setFolderBrowserInitialDir(fileObject.path);
                    Toast.makeText(getActivity(),
                            fileObject.path + getResources().getString(R.string.initial_dir_set_message),
                            Toast.LENGTH_SHORT).show();
                    return true;
                case RESCAN:

                    if (fileObject instanceof FolderObject) {

                        //Todo:
                        // Abstract this away to DialogUtils or somewhere else, where it can be reused
                        // by anyone else who wants to run a scan (like the Tagger)
                        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_progress, null);
                        TextView pathsTextView = (TextView) view.findViewById(R.id.paths);
                        pathsTextView.setText(fileObject.path);

                        ProgressBar indeterminateProgress = (ProgressBar) view.findViewById(R.id.indeterminateProgress);
                        DrawableCompat.setTint(DrawableCompat.wrap(indeterminateProgress.getIndeterminateDrawable()), ColorUtils.getAccentColor());

                        ProgressBar horizontalProgress = (ProgressBar) view.findViewById(R.id.horizontalProgress);
                        DrawableCompat.setTint(DrawableCompat.wrap(horizontalProgress.getProgressDrawable()), ColorUtils.getAccentColor());

                        MaterialDialog dialog = DialogUtils.getBuilder(getContext())
                                .title(R.string.scanning)
                                .customView(view, false)
                                .negativeText(R.string.close)
                                .show();

                        subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                                .map(songs -> Stream.of(songs)
                                        .map(song -> song.path)
                                        .collect(Collectors.toList()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(paths -> {
                                    ViewUtils.fadeOut(indeterminateProgress, null);
                                    ViewUtils.fadeIn(horizontalProgress, null);
                                    horizontalProgress.setMax(paths.size());

                                    CustomMediaScanner.scanFiles(paths, new CustomMediaScanner.ScanCompletionListener() {
                                        @Override
                                        public void onPathScanned(String path) {
                                            horizontalProgress.setProgress(horizontalProgress.getProgress() + 1);
                                            pathsTextView.setText(path);
                                        }

                                        @Override
                                        public void onScanCompleted() {
                                            if (isAdded() && dialog.isShowing()) {
                                                dialog.dismiss();
                                            }
                                        }
                                    });
                                }));
                    } else {
                        CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), new CustomMediaScanner.ScanCompletionListener() {
                            @Override
                            public void onPathScanned(String path) {

                            }

                            @Override
                            public void onScanCompleted() {
                                Toast.makeText(getContext(), R.string.scan_complete, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    return true;
                case VIEW_INFO:
                    DialogUtils.showFileInfoDialog(getActivity(), (FileObject) fileObject);
                    break;
            }
            return false;
        });
        menu.show();
    }

    public ActionMode.Callback getActionModeCallback() {
        if (actionModeCallback == null) {
            actionModeCallback = new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    ThemeUtils.themeContextualActionBar(getActivity());
                    isInActionMode = true;
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.menu_save_whitelist, menu);
                    return true;
                }

                // ToDo: ActionMode Background
                //  -https://stackoverflow.com/questions/20769315/how-to-change-actionmode-background-color-in-android
                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    changeActionModeBackground( getActivity(), fetchAttrColor(getActivity(), R.attr.colorActionModeBackground));
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_save:
                            WhitelistHelper.deleteAllFolders();
                            WhitelistHelper.addToWhitelist(paths);
                            showCheckboxes(false);
                            adapter.notifyDataSetChanged();
                            mode.finish();
                            return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    isInActionMode = false;
                    actionModeCallback = null;
                    showCheckboxes(false);
                    adapter.notifyDataSetChanged();
                }
            };
        }
        return actionModeCallback;
    }

    // HI_RES
    // onClick inside fragment called on Activity
    // -https://stackoverflow.com/questions/6091194/how-to-handle-button-clicks-using-the-xml-onclick-within-fragments
    // -https://stackoverflow.com/questions/7570575/onclick-inside-fragment-called-on-activity
    @Override
    public void onClick(View v) {
        File musicFolder; //  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        switch (v.getId()) {
            case R.id.btn_sd_in:
                musicFolder = new File(com.simplecity.amp_library.constants.Config.SD_IN);
                if( BuildConfig.DEBUG )
                    Log.d("FOLDER", "SD1: " + musicFolder.getAbsolutePath());
                if( musicFolder.exists())
                    changeDir(musicFolder);
                break;
            case R.id.btn_sd_ex0:
                musicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                // musicFolder = new File(com.simplecity.amp_library.constants.Config.SD_EX0);
                if( BuildConfig.DEBUG )
                    Log.d("FOLDER", "SD1: " + musicFolder.getAbsolutePath());
                if( musicFolder.exists())
                    changeDir(musicFolder);
                break;
            case R.id.btn_sd_ex1:
                musicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                // musicFolder = new File(com.simplecity.amp_library.constants.Config.SD_EX1);
                if( BuildConfig.DEBUG )
                    Log.d("FOLDER", "SD1: " + musicFolder.getAbsolutePath());
                if( musicFolder.exists())
                    changeDir(musicFolder);
                break;
        }
    }

    @Override
    public void onLongClick(View v, int position, BaseFileObject file) {
        if (inActionMode) {
            return;
        }

        if (multiSelector.getSelectedPositions().size() == 0) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            inActionMode = true;
        }

        multiSelector.setSelected(position, adapter.getItemId(position), !multiSelector.isSelected(position, adapter.getItemId(position)));

        updateActionModeSelectionCount();
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

    // HI_RES
    private ActionMode.Callback mActionModeCallback = new ModalMultiSelectorCallback(multiSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ThemeUtils.themeContextualActionBar(getActivity());
            inActionMode = true;
            if( HI_RES )
                getActivity().getMenuInflater().inflate(R.menu.context_menu_folder, menu);
            else
                getActivity().getMenuInflater().inflate(R.menu.context_menu_songs, menu);
            SubMenu sub = menu.getItem(0).getSubMenu();
            PlaylistUtils.makePlaylistMenu(getActivity(), sub, FOLDER_FRAGMENT_GROUP_ID);
            return true;
        }

        // ToDo: ActionMode Background
        //  -https://stackoverflow.com/questions/20769315/how-to-change-actionmode-background-color-in-android
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            changeActionModeBackground( getActivity(), fetchAttrColor(getActivity(), R.attr.colorActionModeBackground));
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

            final List<BaseFileObject> checkedFiles = getCheckedFiles();

            if (checkedFiles == null || checkedFiles.size() == 0) {
                if(item.getItemId() == R.id.menu_folder) {
                    goToParent();
                } else if(item.getItemId() == R.id.menu_cancel) {
                    if (actionMode != null)
                        actionMode.finish();
                }
                return true;
            }

            // ToDo: Folder 경우에도 적용 할지? 현재는 파일만 적용
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    PlaylistUtils.createFileObjectPlaylistDialog(getActivity(), checkedFiles);
                    break;
                case PLAYLIST_SELECTED:
                    for( BaseFileObject fileObject : checkedFiles) {
                        final Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(ShuttleUtils.ARG_PLAYLIST);
                        subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(songs -> PlaylistUtils.addToPlaylist(getContext(), playlist, songs)));
                    }
                    break;
                case R.id.delete:
                    for( BaseFileObject fileObject : checkedFiles) {
                        MaterialDialog.Builder builder = DialogUtils.getBuilder(getActivity())
                                .title(R.string.delete_item)
                                .icon(DrawableUtils.getBlackDrawable(getActivity(), R.drawable.ic_dialog_alert));

                        if (fileObject.fileType == FileType.FILE) {
                            builder.content(String.format(getResources().getString(
                                    R.string.delete_file_confirmation_dialog), fileObject.name));
                        } else {
                            builder.content(String.format(getResources().getString(
                                    R.string.delete_folder_confirmation_dialog), fileObject.path));
                        }
                        builder.positiveText(R.string.button_ok)
                                .onPositive((materialDialog, dialogAction) -> {
                                    if (FileHelper.deleteFile(new File(fileObject.path))) {
                                        // ToDo: check notifyDataChanged
                                        // adapter.removeItem(position);
                                        adapter.notifyDataSetChanged();
                                        CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), null);
                                    } else {
                                        Toast.makeText(getActivity(),
                                                fileObject.fileType == FileType.FOLDER ? R.string.delete_folder_failed : R.string.delete_file_failed,
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                        builder.negativeText(R.string.cancel)
                                .show();
                    }
                    break;
                case R.id.menu_add_to_queue:
                    for( BaseFileObject fileObject : checkedFiles) {
                        subscriptions.add(FileHelper.getSongList(new File(fileObject.path), true, false)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(songs -> MusicUtils.addToQueue(getActivity(), songs)));
                    }
                    break;
                case R.id.menu_cancel:
                    if (actionMode != null)
                        actionMode.finish();
                    break;
                case R.id.menu_folder:
                    goToParent();
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(android.support.v7.view.ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            inActionMode = false;
            FolderFragment.this.actionMode = null;
            multiSelector.clearSelections();
        }
    };

    private void goToParent() {
        if (fileBrowser.getCurrentDir() != null && fileBrowser.getRootDir() != null && fileBrowser.getCurrentDir().compareTo(fileBrowser.getRootDir()) != 0) {
            File parent = fileBrowser.getCurrentDir().getParentFile();
            changeDir(parent);
        }
    }

    public void showCheckboxes(boolean show) {

        showCheckboxes = show;

        List<AdaptableItem> folderViews = Stream.of(adapter.items)
                .filter(adaptableItem -> adaptableItem instanceof FolderView)
                .collect(Collectors.toList());

        for (AdaptableItem adaptableItem : folderViews) {
            ((FolderView) adaptableItem).setShowCheckboxes(showCheckboxes);
            adapter.notifyItemChanged(adapter.items.indexOf(adaptableItem));
        }
    }

    public void changeBreadcrumbPath() {

        List<AdaptableItem> breadcrumbViews = Stream.of(adapter.items)
                .filter(adaptableItem -> adaptableItem instanceof BreadcrumbsView)
                .collect(Collectors.toList());

        for (AdaptableItem adaptableItem : breadcrumbViews) {
            ((BreadcrumbsView) adaptableItem).setBreadcrumbsPath(currentDir);
            adapter.notifyItemChanged(adapter.items.indexOf(adaptableItem));
        }
    }

    /**
     * Retrieves all folders from the whitelist, and adds them to our 'pathlist'
     * so the appropriate checkboxes can be checked.
     */
    public void updateWhitelist() {
        WhitelistHelper.getWhitelistFolders()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(whitelistFolders -> {
                    paths.clear();
                    paths.addAll(Stream.of(whitelistFolders)
                            .map(whitelistFolder -> whitelistFolder.folder)
                            .collect(Collectors.toList()));

                    if (showCheckboxes) {
                        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                    }
                });
    }

    // HI_RES
    List<BaseFileObject> getCheckedFiles() {
        return Stream.of(multiSelector.getSelectedPositions())
                .map(i -> adapter.getFileObject(i))
                .collect(Collectors.toList());
    }

    // ToDo: Check: RecycleListener
    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() != -1) {
            adapter.items.get(holder.getAdapterPosition()).recycle(holder);
        }
    }
    // END HI_RES

    @Override
    protected String screenName() {
        return TAG;
    }
}