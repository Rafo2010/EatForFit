package rafayel.hakobyan.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RegisterViewModel extends ViewModel {

    private String email    = "";
    private String password = "";

    private final MutableLiveData<String> name = new MutableLiveData<>("");

    private final MutableLiveData<String> weight = new MutableLiveData<>("");
    private final MutableLiveData<String> height = new MutableLiveData<>("");
    private final MutableLiveData<String> disease = new MutableLiveData<>("");


    public LiveData<String> getName() { return name; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public LiveData<String> getWeight() { return weight; }
    public LiveData<String> getHeight() { return height; }
    public LiveData<String> getDisease() { return disease; }


    public void setName(String value) { name.setValue(value); }
    public void setEmail(String email)       { this.email    = email; }
    public void setPassword(String password) { this.password = password; }
    public void setWeight(String value) { weight.setValue(value); }
    public void setHeight(String value) { height.setValue(value); }
    public void setDisease(String value) { disease.setValue(value); }
}


/*
package rafayel.hakobyan.project;

import androidx.lifecycle.ViewModel;

public class RegisterViewModel extends ViewModel {

    private String email    = "";
    private String password = "";

    public void setEmail(String email)       { this.email    = email; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail()    { return email; }
    public String getPassword() { return password; }

}

 */