package com.simplecity.amp_library.ui.modelviews;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.DrawableUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;

import static com.simplecity.amp_library.ShuttleApplication.HI_RES;

public class PlaylistView extends BaseAdaptableItem<Playlist, PlaylistView.ViewHolder> {

    public Context cx;
    public Playlist playlist;
    private MultiSelector multiSelector;
    private RequestManager requestManager;

    public PlaylistView(Playlist playlist) {
        this.playlist = playlist;
    }

    public PlaylistView(Context cx, Playlist playlist, RequestManager requestManager, MultiSelector multiSelector) {
        this.cx = cx;
        this.playlist = playlist;
        this.multiSelector = multiSelector;
        this.requestManager = requestManager;
    }

    @Override
    public int getViewType() {
        return ViewType.PLAYLIST;
    }

    @Override
    public int getLayoutResId() {
        if( HI_RES )
            return R.layout.list_item_playlist;
        return R.layout.list_item_one_line;
    }

    @Override
    public void bindView(ViewHolder holder ) {
        /* ToDo: check song count
        CompositeSubscription subscriptions = new CompositeSubscription();
        List<Song> songs = new ArrayList<>();
        Observable<List<Song>> observable = null;
        observable = playlist.getSongsObservable(cx);
        observable.subscribe( song -> {
            songs.add((Song) song);
            holder.lineTwo.setText(songs.size());
        });
        */

        holder.lineOne.setText(playlist.name);
        holder.lineTwo.setText("");
        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, playlist.name));
        /*
        requestManager.load(album)
                .listener(getViewType() == ViewType.ALBUM_PALETTE ? GlidePalette.with(album.getArtworkKey())
                        .use(GlidePalette.Profile.MUTED_DARK)
                        .intoBackground(holder.bottomContainer)
                        .crossfade(true)
                        : null)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(GlideUtils.getPlaceHolderDrawable(album.name, false))
                .into(holder.image);
        */
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        // return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false), multiSelector);
    }

    @Override
    public Playlist getItem() {
        return playlist;
    }

    // HI_RES
    public static class ViewHolder extends SwappingHolder {
    // public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageOne;
        public TextView lineOne;
        public TextView lineTwo;
        public NonScrollImageButton overflowButton;

        public ViewHolder(View itemView,  MultiSelector multiSelector) {
            super(itemView, multiSelector);

        // public ViewHolder(View itemView) {
        //     super(itemView);

            imageOne = (ImageView) itemView.findViewById(R.id.image);
            // ToDo: Change Default Image
            imageOne.setImageResource(R.drawable.playlist_icon);
            lineOne = (TextView) itemView.findViewById(R.id.line_one);
            lineTwo = (TextView) itemView.findViewById(R.id.line_two);
            overflowButton = (NonScrollImageButton) itemView.findViewById(R.id.btn_overflow);
            overflowButton.setImageDrawable(DrawableUtils.getColoredStateListDrawable(itemView.getContext(), R.drawable.ic_overflow_white));
        }

        @Override
        public String toString() {
            return "PlaylistView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlaylistView that = (PlaylistView) o;

        return playlist != null ? playlist.equals(that.playlist) : that.playlist == null;

    }

    @Override
    public int hashCode() {
        return playlist != null ? playlist.hashCode() : 0;
    }

    @Override
    public boolean areContentsEqual(Object other) {
        return equals(other);
    }
}
