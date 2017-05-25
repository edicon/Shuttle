package com.simplecity.amp_library.ui.modelviews;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.FavoriteHeader;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;

public class FavoriteHeaderView extends BaseAdaptableItem<FavoriteHeader, FavoriteHeaderView.ViewHolder> {

    public FavoriteHeader favoriteHeader;

    public FavoriteHeaderView(FavoriteHeader favoriteHeader) {
        this.favoriteHeader = favoriteHeader;
    }

    @Override
    public int getViewType() {
        return ViewType.FAVORITE_HEADER;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.favorite_header;
    }

    @Override
    public void bindView(ViewHolder holder) {
        holder.titleOne.setText(favoriteHeader.title);
        holder.titleTwo.setText(favoriteHeader.subtitle);
        holder.titleThree.setBackground(DrawableUtils.getColoredAccentDrawable((holder.itemView.getContext()), holder.titleThree.getBackground(), false, true));
        holder.titleThree.setTextColor(ColorUtils.getAccentColorSensitiveTextColor(holder.itemView.getContext()));
        if (favoriteHeader.subtitle == null || favoriteHeader.subtitle.length() == 0) {
            holder.titleTwo.setVisibility(View.GONE);
        } else {
            holder.titleTwo.setVisibility(View.VISIBLE);
        }

        holder.itemView.setContentDescription(favoriteHeader.title);
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent) {
        return new FavoriteHeaderView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLayoutResId(), parent, false));
    }

    @Override
    public FavoriteHeader getItem() {
        return favoriteHeader;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView titleOne;
        TextView titleTwo;
        TextView titleThree;

        public ViewHolder(View itemView) {
            super(itemView);
            titleOne = (TextView) itemView.findViewById(R.id.text1);
            titleTwo = (TextView) itemView.findViewById(R.id.text2);
            titleThree = (TextView) itemView.findViewById(R.id.text3);
        }

        @Override
        public String toString() {
            return "FavoriteHeaderView.ViewHolder";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FavoriteHeaderView that = (FavoriteHeaderView) o;

        return favoriteHeader != null ? favoriteHeader.equals(that.favoriteHeader) : that.favoriteHeader == null;

    }

    @Override
    public int hashCode() {
        return favoriteHeader != null ? favoriteHeader.hashCode() : 0;
    }
}
