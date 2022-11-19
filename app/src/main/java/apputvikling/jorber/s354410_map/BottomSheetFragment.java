package apputvikling.jorber.s354410_map;

import android.content.DialogInterface;
import android.location.Address;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Locale;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private LatLng coordinates;
    private IOnClick iOnClick;
    private Marker marker;
    private boolean savedMarker = false;
    private String address;

    public BottomSheetFragment(LatLng coordinates, Marker marker) {
        this.coordinates = coordinates;
        this.marker = marker;
    }

    public void setData(LatLng coordinates, Marker marker, String address) {
        this.coordinates = coordinates;
        this.marker = marker;
        this.address = address;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public void setAddress(String address) {
        this.address = address;
        System.out.println("Address: " + address);
        ((TextView)getView().findViewById(R.id.addressTextView)).setText(address);
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ((TextView)getView().findViewById(R.id.titleTextView)).setText("New Attraction");
        ((TextView)getView().findViewById(R.id.addressTextView)).setText("Fetching address");
        String coord = String.format(Locale.ENGLISH, "Lat: %.2f\nLong: %.2f", coordinates.latitude, coordinates.longitude);
        ((TextView) getView().findViewById(R.id.coordTextView)).setText(coord);
        Button okButton = view.findViewById(R.id.okBtn);
        okButton.setOnClickListener(v -> {
            String description = ((EditText) getView().findViewById(R.id.descriptionTextInput)).getText().toString();
            marker.setTitle("Marker");
            marker.setSnippet(description);

            savedMarker = true;
            dismiss();
        });
        Button cancelButton = view.findViewById(R.id.cancelBtn);
        cancelButton.setOnClickListener(v -> {
            marker.remove();
            dismiss();
        });
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if(!savedMarker)
            marker.remove();
        super.onDismiss(dialog);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet, container, false);
    }

    public Marker getMarker() {
        return marker;
    }

    public void resetSaved() {
        savedMarker = false;
    }
}