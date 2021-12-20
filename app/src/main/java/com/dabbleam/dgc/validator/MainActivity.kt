package com.dabbleam.dgc.validator

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScanner.CAMERA_BACK
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.dabbleam.dgc.validator.ui.theme.DGCValidatorTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.DefaultChain
import ehn.techiop.hcert.kotlin.chain.impl.TrustListCertificateRepository
import ehn.techiop.hcert.kotlin.data.GreenCertificate
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MainActivity : ComponentActivity() {
    private val trustList = TrustList( this )
    private var chain: Chain? = null
    var scanner: CodeScanner? = null

    @ExperimentalPermissionsApi
    override fun onCreate( savedInstanceState: Bundle? ) {
        super.onCreate( savedInstanceState )
        val trustList = trustList.getTrustList(trustList.loadTrustList()!!)
        chain = LooseChain.buildVerificationChain( ManualCertificateRepository( trustList ) )
        setContent {
            DGCValidatorTheme {
                Surface( color = MaterialTheme.colors.background )
                {
                    val cameraPermissionState = rememberPermissionState( android.Manifest.permission.CAMERA )
                    PermissionRequired(
                        permissionState = cameraPermissionState,
                        permissionNotGrantedContent = {
                            Column( Modifier.padding( 24.dp ), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally ) {
                                Text( "¡Hola! Es necesario activar los permisos de cámara para continuar." )
                                Spacer( modifier = Modifier.height( 16.dp ) )
                                Button( onClick = { cameraPermissionState.launchPermissionRequest() } ) { Text( "OK" ) }
                            }
                        },
                        permissionNotAvailableContent = {
                            Text( "Cámara no disponible" )
                        })
                    {
                        MainScreen( this )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if ( scanner != null )
        {
            scanner!!.releaseResources()
        }
    }

    override fun onResume()
    {
        super.onResume()
        if ( scanner != null )
        {
            scanner!!.startPreview()
        }
    }

    fun checkCertificate( qrCode: String ): GreenCertificate?
    {
        val decodeResult = chain!!.decode( qrCode )!!.chainDecodeResult
        return decodeResult.eudgc
    }
}

@Composable
@ExperimentalTime
fun MainScreen( ctx: MainActivity )
{
    var lastScanResult by remember { mutableStateOf<Boolean?>( null ) }
    var lastCertificate by remember { mutableStateOf<GreenCertificate?>( null ) }
    var lastCertificateStr: String? = null

    val metrics = DisplayMetrics()
    ctx.windowManager.defaultDisplay.getMetrics( metrics )
    val screenHeightDp = (metrics.heightPixels / metrics.density).dp
    val vibrator = ctx.getSystemService(ComponentActivity.VIBRATOR_SERVICE) as Vibrator

    Column() {
        Scanner( DecodeCallback {
            val newCert = ctx.checkCertificate( it.text )
            var pattern: LongArray? = null
            var amplitudes: IntArray? = null

            var shouldVibrate = lastCertificateStr != it.text
            lastCertificateStr = it.text

            if ( newCert == null )
            {
                pattern    = longArrayOf( 100, 80, 100, 80, 100, 80 )
                amplitudes = intArrayOf(  -1,  0,   -1,  0,   -1,  0 )
                lastScanResult = false
                lastCertificate = null
            }
            else
            {
                pattern    = longArrayOf( 500, 0 )
                amplitudes = intArrayOf(  -1,  0 )
                lastScanResult = true
                lastCertificate = newCert
            }
            if ( shouldVibrate )
            {
                if ( Build.VERSION.SDK_INT >= 26 )
                {
                    vibrator.vibrate( VibrationEffect.createWaveform( pattern, amplitudes, -1 ) )
                }
                else
                {
                    vibrator.vibrate( pattern, -1 )
                }
            }
        }, mainActivity = ctx, modifier = Modifier.height( screenHeightDp - 112.dp ) )
        lastScanResult?.let {
            Row( modifier = Modifier.fillMaxSize().padding( 8.dp ), verticalAlignment = Alignment.CenterVertically )
            {
                Image(
                    ImageVector.vectorResource( id = if ( it ) R.drawable.ic_ok else R.drawable.ic_baseline_error_24 ),
                    if ( it ) "OK" else "Inválido",
                    modifier = Modifier.fillMaxHeight().aspectRatio( 1.0f )
                )
                if ( lastCertificate != null )
                {
                    var name = lastCertificate!!.subject.familyNameTransliterated
                    lastCertificate!!.subject.givenNameTransliterated?.let { name = "$it $name" }
                    name = name.replace( '<', ' ' )

                    var birthDate = lastCertificate!!.dateOfBirthString
                    lastCertificate!!.dateOfBirth?.let {
                        val date = it
                        birthDate = date.dayOfMonth.toString().padStart( 2, '0' ) + "/" +
                                date.monthNumber.toString().padStart( 2, '0' ) + "/" +
                                date.year;
                    }
                    Column()
                    {
                        Text( name )
                        Text( birthDate )
                    }
                }
                else
                {
                    Text( "¡Certificado inválido!" )
                }
            }
        } ?: run {
            Row( modifier = Modifier.fillMaxSize() ) {}
        }
    }
}

@Composable
@ExperimentalTime
fun Scanner( callback: DecodeCallback, mainActivity: MainActivity, modifier: Modifier )
{
    AndroidView( factory = { ctx ->
        com.budiyev.android.codescanner.CodeScannerView( ctx ).apply {
            val scanner = CodeScanner( ctx, this )
            scanner.camera = CAMERA_BACK
            scanner.formats = listOf( BarcodeFormat.QR_CODE )
            scanner.autoFocusMode = AutoFocusMode.SAFE
            scanner.scanMode = ScanMode.CONTINUOUS
            scanner.isAutoFocusEnabled = true
            scanner.decodeCallback = callback
            scanner.errorCallback = ErrorCallback {
                Log.wtf( "DGCValidator", it.message );
            }
            scanner.startPreview()

            mainActivity.scanner = scanner
        }
    }, modifier = modifier )
}