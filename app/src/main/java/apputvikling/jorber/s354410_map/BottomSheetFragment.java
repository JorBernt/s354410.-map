package apputvikling.jorber.s354410_map;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private LatLng coordinates;
    private Marker marker;
    private boolean savedMarker = false;
    private AddressViewModel addressViewModel;
    private IOnClick iOnClick;
    private TextView addressView, coordinatesView;
    private EditText titleView, descriptionView;

    public BottomSheetFragment(LatLng coordinates, Marker marker, IOnClick iOnClick) {
        this.coordinates = coordinates;
        this.marker = marker;
        this.iOnClick = iOnClick;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public void setAddress(String address) {
        addressViewModel.getCurrentAddress().postValue(address);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Button okButton = view.findViewById(R.id.okBtn);
        if(getArguments() == null) {
            okButton.setEnabled(false);
            okButton.setText(R.string.waitingForAddress);
        }
        addressViewModel = new ViewModelProvider(this).get(AddressViewModel.class);
        final Observer<String> addressObserver = address -> {
            ((TextView) getView().findViewById(R.id.addressTextView)).setText(address);
            okButton.setText("OK");
            okButton.setEnabled(true);
        };
        addressViewModel.getCurrentAddress().observe(this, addressObserver);
        titleView = getView().findViewById(R.id.titleTextView);
        titleView.setHint(R.string.newAttraction);
        addressView = getView().findViewById(R.id.addressTextView);
        addressView.setText(R.string.fetchingAddress);
        String coord = String.format(Locale.ENGLISH, "Lat: %.2f\nLong: %.2f", coordinates.latitude, coordinates.longitude);
        coordinatesView = getView().findViewById(R.id.coordTextView);
        coordinatesView.setText(coord);
        descriptionView = getView().findViewById(R.id.descriptionTextInput);
        okButton.setOnClickListener(v -> {
            String description = descriptionView.getText().toString();
            String title = titleView.getText().toString();
            marker.setTitle(title);
            marker.setSnippet(description);
            savedMarker = true;
            iOnClick.saveAttractionInDb(String.format(Locale.ENGLISH, "%s,%s", coordinates.latitude, coordinates.longitude), title, description, addressViewModel.getCurrentAddress().getValue());
            dismiss();
        });
        Button cancelButton = view.findViewById(R.id.cancelBtn);
        cancelButton.setOnClickListener(v -> {
            if(!savedMarker)
                marker.remove();
            dismiss();
        });
        if(getArguments() != null) {
            titleView.setText(getArguments().getString("title"));
            descriptionView.setText(getArguments().getString("description"));
            addressView.setText(getArguments().getString("address"));
            coordinatesView.setText(getArguments().getString("latlng"));
            savedMarker = true;

        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (!savedMarker)
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