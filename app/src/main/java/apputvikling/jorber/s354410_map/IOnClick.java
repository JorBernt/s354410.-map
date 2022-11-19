package apputvikling.jorber.s354410_map;

import android.location.Address;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public interface IOnClick {
    void addAddress(Address address);
}
