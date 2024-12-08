package com.bluealert.app;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mqttapp.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.app.Notification;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.content.Intent;
import android.app.PendingIntent;

public class MainActivity extends AppCompatActivity {

    private static final String BROKER_URL = "tcp://192.168.68.245:1883";
    private static final String CLIENT_ID = "android_app";
    private MqttHandler mqttHandler;
    private TextView temperatureValue;
    private TextView pulseValue;

    private static final String CHANNEL_ID = "default_channel";

    GraphView graph;
    GraphView pulseGraph;
    LineGraphSeries<DataPoint> series;
    BarGraphSeries<DataPoint> pulseSeries;
    private float x = 1;
    private float xP = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mqttHandler = new MqttHandler(this);
        mqttHandler.connect(BROKER_URL,CLIENT_ID);
        // Initialize the TextView
        temperatureValue = findViewById(R.id.temperatureValue);
        pulseValue = findViewById(R.id.pulseValue);

        // Check and request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

        // Change the status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // Set the color of the status bar
            window.setStatusBarColor(getResources().getColor(R.color.statusBarColor)); // You can change the color
        }

        Switch notifSwitch = findViewById(R.id.switchToggle);

        // Set up a listener for the switch
        notifSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch is ON
                subscribeToTopic("home/sensors/pir");
            } else {
                // Switch is OFF
                unsubscribeFromTopic("home/sensors/pir");
            }
        });

        Switch lightsSwitch = findViewById(R.id.switchLED);

        // Set up a listener for the switch
        lightsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch is ON
                publishMessage("home/actuators/led", "on");
                turnFlashlightOn();
            } else {
                // Switch is OFF
                publishMessage("home/actuators/led", "off");
                turnFlashlightOff();
            }
        });

        // Create notification channel if needed (Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "This is the default notification channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Initialize the GraphView and Series
        graph = findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        graph.addSeries(series);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setScalable(true);

        pulseGraph = findViewById(R.id.graphPulse);
        pulseSeries = new BarGraphSeries<>();
        pulseGraph.addSeries(pulseSeries);
        pulseSeries.setSpacing(0);
        pulseGraph.getViewport().setYAxisBoundsManual(true);
        pulseGraph.getViewport().setXAxisBoundsManual(true);
        pulseGraph.getViewport().setScalable(true);
        pulseGraph.getGridLabelRenderer().setLabelVerticalWidth(60);

        // Subscribe to the temperature topic
        subscribeToTopic("home/sensors/temperature");
        subscribeToTopic("home/sensors/dangerous gases");
        subscribeToTopic("home/sensors/pulse");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mqttHandler.disconnect();
        super.onDestroy();
    }

    // Method to handle the arrival of the message on the subscribed topic
    public void onMessageArrived(String topic, String message) {
        if ("home/sensors/temperature".equals(topic)) {
            temperatureValue.setText(message + " °C");

            // Parse the temperature value
            double y = Double.parseDouble(message);

            // Append the new data point to the graph
            addDataPoint(x, y);
            x++; // Increment x for the next point

        }
        if ("home/sensors/pir".equals(topic)) {
            // Call this method when you want to send a notification
            sendNotification("Security Alert", "Motion detected! Please check your premises.");
        }
        if ("home/sensors/dangerous gases".equals(topic)) {
            // Call this method when you want to send a notification
            sendNotification("Gas Leak Alert", "Possible gas leak detected! Please check your stove and other gas appliances.");
        }

        if ("home/sensors/pulse".equals(topic)) {
            pulseValue.setText(message + " bmp");

            double y = Double.parseDouble(message);
            addDataPointPulse(xP, y);
            xP++;
        }
    }

    private void publishMessage(String topic, String message){
        mqttHandler.publish(topic,message);
    }
    private void subscribeToTopic(String topic){
        mqttHandler.subscribe(topic);
    }

    private void unsubscribeFromTopic(String topic){
        mqttHandler.unsubscribe(topic);
    }

    private void sendNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);  // Open MainActivity when notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);  // Add intent to open activity

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    private void requestNotificationPermission() {
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Notification permissions are required for full functionality.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Request permission
        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
    }

    private void addDataPoint(double x, double y) {
        series.appendData(new DataPoint(x, y), true, 50); // Keep the latest 50 points in the series

        // Center the graph around the new data point
        double viewportStart = Math.max(x - 13, 0); // Center X-axis around the new point (15 points wide)
        double viewportEnd = viewportStart + 16;     // Show 15 points at a time
        graph.getViewport().setMinX(viewportStart);
        graph.getViewport().setMaxX(viewportEnd);

        double padding = 5; // Adjust this for more or less space around the point
        double yViewportStart = Math.max(y - padding, 0); // Ensure Y-axis does not go negative
        double yViewportEnd = y + padding;
        graph.getViewport().setMinY(yViewportStart);
        graph.getViewport().setMaxY(yViewportEnd);
    }

    private void turnFlashlightOn() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0]; // Usually, 0 is the rear camera
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void turnFlashlightOff() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error turning flashlight off", Toast.LENGTH_SHORT).show();
        }
    }
    private void addDataPointPulse(double x, double y) {
        pulseSeries.appendData(new DataPoint(x, y), true, 50); // Keep the latest 50 points in the series

        // Center the graph around the new data point
        double viewportStart = Math.max(x - 13.5, 0); // Center X-axis around the new point (15 points wide)
        double viewportEnd = viewportStart + 16;     // Show 15 points at a time
        pulseGraph.getViewport().setMinX(viewportStart);
        pulseGraph.getViewport().setMaxX(viewportEnd);

        double padding = 5; // Adjust this for more or less space around the point
        double yViewportStart = Math.max(y - padding, 0); // Ensure Y-axis does not go negative
        double yViewportEnd = y + padding;
        pulseGraph.getViewport().setMinY(yViewportStart);
        pulseGraph.getViewport().setMaxY(yViewportEnd);
    }

}