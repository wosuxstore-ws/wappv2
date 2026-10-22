package com.wl.orders;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 2-column product tiles on the Home screen.
// STATIC for now: DEFAULT_TILES below is hard-coded. When products come from the
// API later, build a List<Tile> from the response and call setTiles(...).
public class HomeProductAdapter extends RecyclerView.Adapter<HomeProductAdapter.ViewHolder> {

    public static class Tile {
        final String price;
        @DrawableRes final int icon;
        @ColorRes final int bg;
        @ColorRes final int fg;

        public Tile(String price, @DrawableRes int icon, @ColorRes int bg, @ColorRes int fg) {
            this.price = price;
            this.icon = icon;
            this.bg = bg;
            this.fg = fg;
        }
    }

    public static final List<Tile> DEFAULT_TILES = Arrays.asList(
            new Tile("Rs. 89.00",  R.drawable.ic_bag,        R.color.tile_blue_bg,  R.color.tile_blue_fg),
            new Tile("Rs. 64.00",  R.drawable.ic_cup,        R.color.tile_peach_bg, R.color.tile_peach_fg),
            new Tile("Rs. 42.00",  R.drawable.ic_headphones, R.color.tile_green_bg, R.color.tile_green_fg),
            new Tile("Rs. 115.00", R.drawable.ic_bolt,       R.color.tile_lilac_bg, R.color.tile_lilac_fg)
    );

    private List<Tile> tiles = new ArrayList<>(DEFAULT_TILES);

    public void setTiles(List<Tile> newTiles) {
        this.tiles = newTiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Tile t = tiles.get(position);

        // Each tile needs its own background instance so colors don't bleed between tiles.
        GradientDrawable bg = (GradientDrawable) ContextCompat
                .getDrawable(h.itemView.getContext(), R.drawable.bg_tile).mutate();
        bg.setColor(ContextCompat.getColor(h.itemView.getContext(), t.bg));
        h.itemView.setBackground(bg);

        h.icon.setImageResource(t.icon);
        ImageViewCompat.setImageTintList(h.icon,
                ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), t.fg)));
        h.price.setText(t.price);
    }

    @Override
    public int getItemCount() {
        return tiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView price;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tileIcon);
            price = itemView.findViewById(R.id.tilePrice);
        }
    }
}
