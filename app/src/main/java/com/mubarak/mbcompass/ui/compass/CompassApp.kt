package com.mubarak.mbcompass.ui.compass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mubarak.mbcompass.MainViewModel
import com.mubarak.mbcompass.R
import com.mubarak.mbcompass.sensor.AndroidSensorEventListener
import com.mubarak.mbcompass.utils.CardinalDirection
import kotlin.math.roundToInt

@Composable
fun CompassApp(context: Context) {
    val androidSensorEventListener = AndroidSensorEventListener(context)

    KeepScreenOn()
    var degreeIn by remember {
        mutableFloatStateOf(0F)
    }
    Scaffold { innerPadding ->
        RegisterListener(
            lifecycleEventObserver = LocalLifecycleOwner.current,
            listener = androidSensorEventListener,
        ) {
            degreeIn = it
        }
        MBCompass(modifier = Modifier.padding(innerPadding), degreeIn = degreeIn)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MBCompass(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
    degreeIn: Float
) {

    val azimuthState by viewModel.azimuth.collectAsStateWithLifecycle()
    val cardinalDirection = CardinalDirection.getDirectionFromAzimuth(azimuthState)
    val degree = azimuthState.roundToInt()
    viewModel.updateAzimuth(degreeIn)

    FlowColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Color.Black
            ),
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.Center
    ) {

        val imageModifier = Modifier
            .size(400.dp)
            .graphicsLayer {
                rotationZ = -degree.toFloat()
            }

        Image(
            painterResource(id = R.drawable.v2_compass_mb),
            contentDescription = stringResource(id = R.string.compass), modifier = imageModifier
        )

        val fontFamily = FontFamily(
            Font(resId = R.font.dm_sans_18, weight = FontWeight.Bold)
        )

        FlowColumn(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {

            Text(
                text = "$degree°",
                fontFamily = fontFamily,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = stringResource(id = cardinalDirection.dirName),
                color = Color.White,
                fontFamily = fontFamily,
                style = MaterialTheme.typography.titleLarge,
            )
        }

    }

}

@Composable
fun RegisterListener(
    lifecycleEventObserver: LifecycleOwner,
    listener: AndroidSensorEventListener,
    degree: (Float) -> Unit = {},
) {

    DisposableEffect(lifecycleEventObserver) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listener.registerSensor()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                listener.unregisterSensorListener()
            }
        }
        lifecycleEventObserver.lifecycle.addObserver(observer)

        onDispose {
            lifecycleEventObserver.lifecycle.removeObserver(observer)
            listener.unregisterSensorListener()
        }
    }

    val list = object : AndroidSensorEventListener.AzimuthValueListener {
        override fun onAzimuthValueChange(degree: Float) {
            degree(degree)
        }
    }

    listener.setAzimuthListener(list)
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