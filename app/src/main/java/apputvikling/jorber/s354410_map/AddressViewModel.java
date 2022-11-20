package apputvikling.jorber.s354410_map;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AddressViewModel extends ViewModel {
    private MutableLiveData<String> address;

    public MutableLiveData<String> getCurrentAddress() {
        if (address == null) {
            address = new MutableLiveData<String>();
        }
        return address;
    }
}
