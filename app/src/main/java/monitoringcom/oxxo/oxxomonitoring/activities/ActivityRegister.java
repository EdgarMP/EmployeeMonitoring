package monitoringcom.oxxo.oxxomonitoring.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import monitoringcom.oxxo.oxxomonitoring.R;

public class ActivityRegister extends AppCompatActivity {

public static final String MY_PREFS_NAME = "MyPrefsFile";

    EditText editTextNom,editTextPlaza,editTextClave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button bttReg = (Button) findViewById(R.id.buttonReg);
        bttReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                editTextNom   = (EditText)findViewById(R.id.nombre);
                editTextPlaza   = (EditText)findViewById(R.id.plaza);
                editTextClave   = (EditText)findViewById(R.id.clave);

                SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();

                Log.v("editTextNom", editTextNom.getText().toString());
                Log.v("editTextPlaza", editTextPlaza.getText().toString());
                Log.v("editTextClave", editTextClave.getText().toString());


                editor.putString("nombre", editTextNom.getText().toString());
                editor.putString("plaza", editTextPlaza.getText().toString());
                editor.putString("clave", editTextClave.getText().toString());
                editor.commit();

                startActivity(new Intent(ActivityRegister.this, ActivityInit.class));

            }
        });
    }

}
