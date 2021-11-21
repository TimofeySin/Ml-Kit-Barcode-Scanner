package com.example.mlkitsample

import androidx.camera.core.*
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mlkitsample.databinding.FragmentFirstBinding

import android.util.Size
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.camera.view.PreviewView
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FirstFragment : Fragment() {
    private val requestCodeCameraPermission = 1001
    private var _binding: FragmentFirstBinding? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var surfaceViewCamera: PreviewView
    private lateinit var takePhoto: Button
    private lateinit var textViewOut: TextView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val displayWidth = Resources.getSystem().displayMetrics.widthPixels
    private val displayHeight = Resources.getSystem().displayMetrics.heightPixels
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var scanner: BarcodeScanner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_first, container, false)
        surfaceViewCamera = view.findViewById(R.id.surfaceViewCamera)
        takePhoto = view.findViewById(R.id.takeFoto)
        textViewOut=  view.findViewById(R.id.textView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (cameraPermissionWasGranted()) {
            //    buildControls()
            surfaceViewCamera.post { setupCamera() }
        } else {
            askForCameraPermission()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(displayWidth, displayHeight))
            .build()
        buildControls()
        addButtonTakePhoto()
    }

    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        val preview: Preview = Preview.Builder()
            .setTargetResolution(Size(displayWidth, displayHeight))
            .build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        preview.setSurfaceProvider(surfaceViewCamera.surfaceProvider)

        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageCapture,
            preview
        )
    }

    private fun addButtonTakePhoto() {

        takePhoto.setOnClickListener {
            imageCapture.takePicture(cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        super.onCaptureSuccess(image)
                        recognizeBarcode(image)
                        image.close()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        super.onError(exception)
                        val msg = exception.localizedMessage
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }


    private fun buildControls() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
        scanner = BarcodeScanning.getClient(options)
    }

    private fun recognizeBarcode(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image)
        val inputImg = InputImage.fromBitmap(bitmap, image.imageInfo.rotationDegrees)
        val result = scanner.process(inputImg)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    val barcodeNum = barcode.displayValue
                    Handler(Looper.getMainLooper()).post {
                        textViewOut.text=barcodeNum
                    }
                }
            }
            .addOnFailureListener {e ->

                val msg = "Ошибка распознования:" + e.localizedMessage
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }

            }


    }


    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

//


//                // Pass image to an ML Kit Vision API
//                @androidx.camera.core.ExperimentalGetImage
//                val result = scanner.process(image)
//                    .addOnSuccessListener { barcodes ->
//                        // Task completed successfully
//                        Log.i("Status", "In success listener")
//                        for (barcode in barcodes) {
//                            val bounds = barcode.boundingBox
//                            val corners = barcode.cornerPoints
//
//                            val rawValue = barcode.rawValue
//                            Log.i("QR code", rawValue.toString())
//
//                            val valueType = barcode.valueType
//                            // See API reference for complete list of supported types
//                            when (valueType) {
//                                Barcode.TYPE_WIFI -> {
//                                    val ssid = barcode.wifi!!.ssid
//                                    val password = barcode.wifi!!.password
//                                    val type = barcode.wifi!!.encryptionType
//                                }
//                                Barcode.TYPE_URL -> {
//                                    val title = barcode.url!!.title
//                                    val url = barcode.url!!.url
//                                }
//                            }
//                        }
//                    }
//                    .addOnFailureListener {
//                        // Task failed with an exception
//                        Log.i("Status", "In failure listener")
//                    }
//                    .addOnCompleteListener{
//                        // Task failed with an exception
//                        Log.i("Status", "In on complete listener")
//                        imageProxy.close()
//                    }
//            }
//
//        })

    //       cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)


//        val detectionTaskCallback: DetectionTaskCallback<List<Barcode>> =
//            DetectionTaskCallback<List<Barcode>> { detectionTask ->
//                detectionTask
//                    .addOnSuccessListener { results -> onDetectionTaskSuccess(results) }
//                    .addOnFailureListener { e -> onDetectionTaskFailure(e) }
//            }
//




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun cameraPermissionWasGranted() =
        ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun askForCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            requestCodeCameraPermission
        )
    }


}
