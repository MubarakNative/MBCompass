package com.mubarak.mbcompass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mubarak.mbcompass.ui.theme.MBCompassTheme
import kotlinx.coroutines.delay
import java.text.DecimalFormat

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager

    private val accelerometerReading = FloatArray(3) // Accelerometer readings
    private val magnetometerReading = FloatArray(3) // Magnetometer readings

    private val rotationMatrix = FloatArray(9) // rotational matrix based on accelerometer and magnetometer readings
    private val adjustedRotationMatrix = FloatArray(9) // adjust the rotational matrix based on screen orientation to also support landscape
    private val orientationAngles = FloatArray(3) // final azimuth,roll,pitch values from getOrientation method

    private var azimuth = 0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        setContent {
            MBCompassTheme {
                KeepScreenOn() // keep the screen on (Optional)
                Scaffold { innerPadding ->
                    val lifeCycleOwner = LocalLifecycleOwner.current
                    MBCompass(
                        modifier = Modifier.padding(innerPadding),
                        lifecycleEventObserver = lifeCycleOwner,
                        sensorManager = sensorManager
                    )
                }
            }
        }
    }

    @Composable
    fun MBCompass(
        modifier: Modifier = Modifier,
        lifecycleEventObserver: LifecycleOwner,
        sensorManager: SensorManager
    ) {
        var azimuthState by remember { mutableFloatStateOf(0f) }

        DisposableEffect(lifecycleEventObserver) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) { // register the Listener when activity is in foreground
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                        ?.also { accelerometer ->
                            sensorManager.registerListener(
                                this@MainActivity,
                                accelerometer,
                                SensorManager.SENSOR_DELAY_NORMAL,
                                SensorManager.SENSOR_DELAY_UI
                            )
                        }
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                        ?.also { magneticField ->
                            sensorManager.registerListener(
                                this@MainActivity,
                                magneticField,
                                SensorManager.SENSOR_DELAY_NORMAL,
                                SensorManager.SENSOR_DELAY_UI
                            )
                        }
                } else if (event == Lifecycle.Event.ON_PAUSE) {
                    sensorManager.unregisterListener(this@MainActivity) // unregisterListener when activity is not in foreground
                }
            }
            lifecycleEventObserver.lifecycle.addObserver(observer)

            onDispose {
                lifecycleEventObserver.lifecycle.removeObserver(observer)
                sensorManager.unregisterListener(this@MainActivity)
            }
        }

        LaunchedEffect(Unit) {
            while (true) {  //Todo: Update it from suspicious while loop to reactive streams like livedata or flows
                azimuthState = azimuth
                delay(100) // Update UI at 10 Hz
            }
        }

        val decimalFormatter = DecimalFormat("###.#")
        val degree = decimalFormatter.format(azimuthState)

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    if (isSystemInDarkTheme()) {
                        Color.Black
                    } else Color.Black
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageModifier = Modifier
                .size(400.dp)
                .background(
                    if (isSystemInDarkTheme()) {
                        Color.Black
                    } else Color.Black
                )
                .graphicsLayer(rotationZ = -azimuthState) // rotate the image based on pointing geomagnetic field

            Image(
                painterResource(id = R.drawable.compass_image_png), // replace with your compass image resource
                contentDescription = "Compass", modifier = imageModifier

            )

            Text(
                text = "\t\t\t${degree}°\n ${getDirectionFromAzimuth(azimuthState)}",
                modifier = modifier.background(
                        if (isSystemInDarkTheme()) {
                            Color.Black
                        } else Color.White
                    ),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size) // provide values to the accelerometerReading from event.values
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size) // provide values to the magnetometerReading from event.values
        }

        updateOrientationAngles()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Toast.makeText(this@MainActivity, "Calibration required~", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix, null, accelerometerReading, magnetometerReading
        )


        // Get the current device rotation
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.display?.rotation
        } else {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
        }
        // Remap the rotation matrix based on the current device rotation
        when (rotation) {
            Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(
                rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, adjustedRotationMatrix
            )

            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                adjustedRotationMatrix
            )

            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_X,
                SensorManager.AXIS_MINUS_Y,
                adjustedRotationMatrix
            )

            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_MINUS_Y,
                SensorManager.AXIS_X,
                adjustedRotationMatrix
            )
        }

        // Use the adjusted rotation matrix to get the orientation angles.
        SensorManager.getOrientation(adjustedRotationMatrix, orientationAngles) // getOrientation method provide value for orientationAngles
        val inDegrees = (Math.toDegrees(orientationAngles[0].toDouble())
            .toFloat() + 360) % 360 // convert radian to +degree and range at max 360

        azimuth = inDegrees // update the azimuth value from orientationAngles provided degree
    // orientationAngles[0] for Azimuth (this is we only care)
    // orientationAngles[1] for Pitch (angle of rotation about the x axis) ONLY REQUIRED FOR AR apps
    // orientationAngles[2] for Roll (angle of rotation about the y axis) ONLY REQUIRED FOR AR apps
    }

}


fun getDirectionFromAzimuth(azimuth: Float): String {
    return when (azimuth) {
        in 0.0..22.5, in 337.5..360.0 -> "North"
        in 22.5..45.0 -> "North-Northeast"
        in 45.0..67.5 -> "Northeast"
        in 67.5..90.0 -> "East-Northeast"
        in 90.0..112.5 -> "East"
        in 112.5..135.0 -> "East-Southeast"
        in 135.0..157.5 -> "Southeast"
        in 157.5..180.0 -> "South-Southeast"
        in 180.0..202.5 -> "South"
        in 202.5..225.0 -> "South-Southwest"
        in 225.0..247.5 -> "Southwest"
        in 247.5..270.0 -> "West-Southwest"
        in 270.0..292.5 -> "West"
        in 292.5..315.0 -> "West-Northwest"
        in 315.0..337.5 -> "Northwest"
        else -> "N/A" // In case the azimuth doesn't fall in any expected range
    }
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}