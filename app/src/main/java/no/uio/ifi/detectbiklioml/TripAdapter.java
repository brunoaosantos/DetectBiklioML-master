package no.uio.ifi.detectbiklioml;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.List;

/**
 * Recycler view adapter. List of trips.
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripsList;
    private Context context;

    TripAdapter(List<Trip> tripsList) {
        this.tripsList = tripsList;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @androidx.annotation.NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.trip_view, viewGroup, false);
        context = viewGroup.getContext();
        return new TripViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull final TripViewHolder viewHolder, int i) {
        final Trip trip = tripsList.get(i);
        DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

        viewHolder.txtStart.setText(dateFormatter.format(trip.getStartDate()));
        viewHolder.txtEnd.setText(dateFormatter.format(trip.getEndDate()));
        viewHolder.txtDistance.setText(trip.getDistance() + " m");

        ImageView tripIcon = viewHolder.tripIcon;
        if (!trip.isBike()) {
            //deprecated
            //Drawable unknownMode = context.getResources().getDrawable(R.drawable.ic_unknown);
            Drawable unknownMode = ContextCompat.getDrawable(context.getApplicationContext(),R.drawable.ic_unknown);
            tripIcon.setImageDrawable(unknownMode);
        }

        viewHolder.btnDelete.setOnClickListener(v -> {
            TripRepository.delete(trip.getId());
            TripRepository.fetch(v.getContext());
        });
    }

    @Override
    public int getItemCount() {
        return tripsList.size();
    }

    void setTripsList(List<Trip> tripsList) {
        this.tripsList = tripsList;
    }

    class TripViewHolder extends RecyclerView.ViewHolder {

        TextView txtStart;
        TextView txtEnd;
        TextView txtDistance;
        ImageView tripIcon;
        ImageButton btnDelete;

        TripViewHolder(@androidx.annotation.NonNull View itemView) {
            super(itemView);
            txtStart = itemView.findViewById(R.id.txtStart);
            txtEnd = itemView.findViewById(R.id.txtEnd);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            tripIcon = itemView.findViewById(R.id.imgViewIsBike);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

    }
}
