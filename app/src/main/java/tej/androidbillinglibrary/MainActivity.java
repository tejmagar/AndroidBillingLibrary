package tej.androidbillinglibrary;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.List;

import tej.billing.lib.Billing;
import tej.billing.lib.interfaces.OnQueryItems;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}