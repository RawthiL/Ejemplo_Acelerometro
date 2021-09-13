package com.atleastitworks.ejemplo_acelerometro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    private double mLastX, mLastY, mLastZ;

    private boolean mInitialized;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;


    // variables del Exponential Moving Average
    private double alpha_decay = 0.25;
    private double ema_x;
    private double ema_y;
    private double ema_z;
    private double ema_count_x;
    private double ema_count_y;
    private double ema_count_z;

    // Calibracion
    private boolean CALIBRATION_FLAG;
    private double NOISE_X = 0.02;
    private double NOISE_Y = 0.02;
    private double NOISE_Z = 0.02;
    private int calib_count = 0;
    private int CALIB_SAMPLES = 100;
    private double CALIB_CONSTANT = 1.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mInitialized = false;
        CALIBRATION_FLAG = false;


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        // Boton de calibracion
        final Button boton_cal = findViewById(R.id.cal_button);

        boton_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrar_ruido();
            }
        });


    }

    private void calibrar_ruido()
    {
        Context context = getApplicationContext();
        CharSequence text = "Calibrando, no mueva el dispositivo.";
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        CALIBRATION_FLAG = true;

    }




    @Override
    public void onSensorChanged(SensorEvent event) {

        TextView tvX= findViewById(R.id.x_axis);
        TextView tvX_ang= findViewById(R.id.ang_x);
        TextView tvY= findViewById(R.id.y_axis);
        TextView tvY_ang= findViewById(R.id.ang_y);
        TextView tvZ= findViewById(R.id.z_axis);
        TextView tvZ_ang= findViewById(R.id.ang_z);

        TextView mod_g_text= findViewById(R.id.modulo_g_text);





        ImageView iv = findViewById(R.id.image);


        double x = event.values[0];

        double y = event.values[1];

        double z = event.values[2];

        if (!mInitialized) {

            mLastX = x;
            ema_x = 0;
            mLastY = y;
            ema_y = 0;
            mLastZ = z;
            ema_z = 0;

            ema_count_x = 0;
            ema_count_y = 0;
            ema_count_z = 0;

            tvX.setText("0.0");
            tvX_ang.setText("0.0");
            tvY.setText("0.0");
            tvY_ang.setText("0.0");
            tvZ.setText("0.0");
            tvY_ang.setText("0.0");

            mod_g_text.setText("El modulo de aceleracion actual es de... 0.0 m/seg^2");

            mInitialized = true;

        }

        if ( mInitialized && !CALIBRATION_FLAG)
        {
            // Actualizo el moving average exponencial,
            // siempre que detectemos un cambio mas alla del piso de ruido (empirico)
            if (Math.abs(mLastX - x) > NOISE_X)
            {
                ema_x = x + (1-alpha_decay)*ema_x;
                ema_count_x = 1 + (1-alpha_decay)*ema_count_x;
            }
            mLastX = x;
            if (Math.abs(mLastY - y) > NOISE_Y)
            {
                ema_y = y + (1-alpha_decay)*ema_y;
                ema_count_y = 1 + (1-alpha_decay)*ema_count_y;
            }
            mLastY = y;
            if (Math.abs(mLastZ - z) > NOISE_Z)
            {
                ema_z = z + (1-alpha_decay)*ema_z;
                ema_count_z = 1 + (1-alpha_decay)*ema_count_z;
            }
            mLastZ = z;


            // Asigno el nuevo valor
            double use_x = ema_x/ema_count_x;
            double use_y = ema_y/ema_count_y;
            double use_z = ema_z/ema_count_z;



            tvX.setText(String.format("%.4f",use_x));

            tvY.setText(String.format("%.4f",use_y));

            tvZ.setText(String.format("%.4f",use_z));


            if (Math.abs(use_x)  > Math.abs(use_y)) {

                iv.setImageResource(R.drawable.shaker_fig_1);

            } else if (Math.abs(use_y) > Math.abs(use_x)) {

                iv.setImageResource(R.drawable.shaker_fig_2);

            }
            /*
            else {iv.setVisibility(View.INVISIBLE);}
             */

            // Calculo el modulo
            double mod_g_act;
            mod_g_act = Math.sqrt(Math.pow(use_x,2)+Math.pow(use_y,2)+Math.pow(use_z,2));
            mod_g_text.setText("El modulo de aceleracion actual es de... "+String.format("%.2f",mod_g_act)+" m/seg^2");

            // Calculo el angulo
            double g = 9.8; // m/seg^2
            double ang_med_x = 0, ang_med_y = 0, ang_med_z = 0;

            ang_med_x = Math.round(Math.asin(use_x/g)*180.0/Math.PI);
            ang_med_y = Math.round(Math.asin(use_y/g)*180.0/Math.PI);
            ang_med_z = Math.round(Math.asin(use_z/g)*180.0/Math.PI);

            tvX_ang.setText(String.format("%.2f",ang_med_x));

            tvY_ang.setText(String.format("%.2f",ang_med_y));

            tvZ_ang.setText(String.format("%.2f",ang_med_z));

        }

        // Modo calibracion
        if (CALIBRATION_FLAG)
        {
            if (calib_count == 0)
            {
                // Reinicio los promedios
                ema_x = Math.abs(mLastX - x);
                ema_y = Math.abs(mLastY - y);
                ema_z = Math.abs(mLastZ - z);

                mLastX = x;
                mLastY = y;
                mLastZ = z;
            }
            else
            {
                // Ahora hago un promedio normal, ya que el
                // dispositivo se encuentra siempre en el mismo estado
                // durante la calibracion (no hay que moverlo)

                ema_x = (((calib_count*ema_x) + Math.abs(mLastX - x))/(calib_count + 1));
                ema_y = (((calib_count*ema_x) + Math.abs(mLastY - y))/(calib_count + 1));
                ema_z = (((calib_count*ema_x) + Math.abs(mLastZ - z))/(calib_count + 1));

                mLastX = x;
                mLastY = y;
                mLastZ = z;

                // Esto se llama "running moving average (RMA)"
                // Tiene esa forma ya que nuestro N (calib_count) empieza
                // en 0 en lugar de 1, como la expresion matematica.
            }


            if(calib_count > CALIB_SAMPLES)
            {
                // asigno los nuevos valores de ruido
                NOISE_X = ema_x*CALIB_CONSTANT;
                NOISE_Y = ema_y*CALIB_CONSTANT;
                NOISE_Z = ema_z*CALIB_CONSTANT;
                // Reseteo el contador
                calib_count = 0;
                // Salgo del estado calibracion
                CALIBRATION_FLAG = false;
                // Me pongo en modo no inicializado (asi reinicion el EMA)
                mInitialized = false;


                Context context = getApplicationContext();
                CharSequence text = "Ruido: X = "+String.format("%.4f",NOISE_X)+"Ruido: Y = "+String.format("%.4f",NOISE_Y)+"Ruido: Z = "+String.format("%.4f",NOISE_Z);
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

            }

            calib_count++;

        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // can be safely ignored for this demo
    }






    protected void onResume() {

        super.onResume();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onPause() {

        super.onPause();

        mSensorManager.unregisterListener(this);

    }

}
