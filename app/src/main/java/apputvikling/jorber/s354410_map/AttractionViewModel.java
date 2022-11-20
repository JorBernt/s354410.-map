package apputvikling.jorber.s354410_map;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class AttractionViewModel extends ViewModel {
    private MutableLiveData<List<Attraction>> attractions;

    public MutableLiveData<List<Attraction>> getCurrentAttractions() {
        if (attractions == null) {
            attractions = new MutableLiveData<List<Attraction>>();
        }
        return attractions;
    }
}
