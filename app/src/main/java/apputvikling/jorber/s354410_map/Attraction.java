package apputvikling.jorber.s354410_map;

import com.google.android.gms.maps.model.LatLng;

public class Attraction {
    private String title, description, address;
    private LatLng latLng;

    public Attraction(String title, String description, String address, LatLng latLng) {
        this.title = title;
        this.description = description;
        this.address = address;
        this.latLng = latLng;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}
