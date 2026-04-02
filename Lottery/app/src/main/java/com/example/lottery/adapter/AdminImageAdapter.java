package com.example.lottery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.R;
import com.example.lottery.model.Event;
import com.example.lottery.util.PosterImageLoader;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying event poster images in the admin image browser.
 *
 * <p>Key Responsibilities:</p>
 * <ul>
 *   <li>Binds event poster thumbnails, titles, and dates to RecyclerView items.</li>
 *   <li>Uses {@link PosterImageLoader} to load poster images via Glide.</li>
 *   <li>Handles clicks on image items to navigate to the detail/preview screen.</li>
 * </ul>
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.ImageViewHolder> {

    /**
     * List of events with poster images to be displayed.
     */
    private final List<Event> imageList;
    /**
     * Listener for image click interactions.
     */
    private final OnImageClickListener listener;
    /**
     * Date formatter for displaying event scheduled times.
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    /**
     * Constructs a new AdminImageAdapter.
     *
     * @param imageList The list of events with poster images to display.
     * @param listener  The listener to handle click events.
     */
    public AdminImageAdapter(List<Event> imageList, OnImageClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bind(imageList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    /**
     * Interface definition for a callback to be invoked when an image item is clicked.
     */
    public interface OnImageClickListener {
        /**
         * Called when an image item has been clicked.
         *
         * @param event The Event object associated with the clicked poster.
         */
        void onImageClick(Event event);
    }

    /**
     * ViewHolder class for holding and binding image item views.
     */
    @VisibleForTesting
    public class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvEventTitle;
        private final TextView tvEventDate;

        /**
         * Constructs an ImageViewHolder.
         *
         * @param itemView The root view of the item layout.
         */
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDateTime);
        }

        /**
         * Binds event poster data to the view elements.
         *
         * @param event    The event data to bind.
         * @param listener The listener for click events.
         */
        public void bind(final Event event, final OnImageClickListener listener) {
            tvEventTitle.setText(event.getTitle());
            if (event.getScheduledDateTime() != null) {
                tvEventDate.setText(dateFormat.format(event.getScheduledDateTime().toDate()));
            } else {
                tvEventDate.setText("");
            }

            PosterImageLoader.load(ivThumbnail, event.getPosterBase64(), R.drawable.event_placeholder);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(event);
                }
            });
        }
    }
}
