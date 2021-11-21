package com.example.mlkitsample

import androidx.camera.core.*
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
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
import android.widget.Toast

import androidx.camera.view.PreviewView
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FirstFragment : Fragment() {
    private val requestCodeCameraPermission = 1001
    private var _binding: FragmentFirstBinding? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var surfaceViewCamera: PreviewView
    private lateinit var takePhoto: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val displayWidth = Resources.getSystem().displayMetrics.widthPixels
    private val displayHeight = Resources.getSystem().displayMetrics.heightPixels
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_first, container, false)
        surfaceViewCamera = view.findViewById(R.id.surfaceViewCamera)
        takePhoto = view.findViewById(R.id.takeFoto)
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
        addButtonTakePhoto()
    }

    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {

        // val screenAspectRatio = Rational(displayWidth, displayHeight)
        val preview : Preview = Preview.Builder()
            .setTargetResolution(Size(displayWidth, displayHeight))
            .build()
        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        preview.setSurfaceProvider(surfaceViewCamera.surfaceProvider)

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector,imageCapture,preview)
    }

    private fun addButtonTakePhoto() {

        takePhoto.setOnClickListener {
            imageCapture.takePicture(cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {

                    override fun onCaptureSuccess(image: ImageProxy) {
                        super.onCaptureSuccess(image)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(requireContext(), "Сфотал!", Toast.LENGTH_SHORT).show()
                        }
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























//    private fun buildControls() {
//        val options = BarcodeScannerOptions.Builder()
//            .setBarcodeFormats(
//                Barcode.FORMAT_EAN_13
//            )
//            .build()
//        detector = BarcodeScanning.getClient(options)
//    }
//



       // cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, , preview)

//        val imageAnalysis = ImageAnalysis.Builder()
//            .setTargetResolution(screenSize)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()

//        // Define options for barcode scanner
//        val options = BarcodeScannerOptions.Builder()
//            .setBarcodeFormats(
//                Barcode.FORMAT_EAN_13,
//                )
//            .build()

//        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), ImageAnalysis.Analyzer { imageProxy ->
//            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//
//            // Initiate barcode scanner
//            val scanner = BarcodeScanning.getClient(options)
//
//            // insert your code here.
//            @androidx.camera.core.ExperimentalGetImage
//            val mediaImage = imageProxy.image
//
//            @androidx.camera.core.ExperimentalGetImage
//            if (mediaImage != null) {
//                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
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
//        val builder: CameraSourceConfig.Builder = CameraSourceConfig.Builder(
//            requireActivity().applicationContext, detector, detectionTaskCallback
//        )
//            .setFacing(lensFacing)
////        targetResolution = PreferenceUtils.getCameraXTargetResolution(
////            requireActivity().applicationContext,
////            lensFacing
////        )
//
//        builder.setRequestedPreviewSize(targetResolution!!.width, targetResolution!!.height)
//
//        cameraXSource = CameraXSource(builder.build(), surfaceViewCamera)
//        needUpdateGraphicOverlayImageSourceInfo = true



















//
//
//    private fun buildControls() {
//        val options = BarcodeScannerOptions.Builder()
//            .setBarcodeFormats(
//                Barcode.EAN_13)
//            .build()
//
//
//        barcodeDetector = BarcodeDetector.Builder(this@MainActivity).build()
//
//        barcodeDetector.setProcessor(barcodeProcessor)
//
//        cameraSource = CameraSource.Builder(this@MainActivity, barcodeDetector)
//            .setAutoFocusEnabled(true)
//            .build()
//        cameraSurfaceView.holder.addCallback(surfaceCallback)
//    }


//    override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
//        if (!receivedCode && detections != null && detections.detectedItems.isNotEmpty()) {
//            //  receivedCode = true
//            val codes: SparseArray<Barcode> = detections.detectedItems
//            var stringCode =' '.toString()
//            var stringkey =0
//
//            for (i in 0 until codes.size()) {
//                stringkey += codes.keyAt(i)
//                // get the object by the key.
//                stringCode += codes.get(stringkey).displayValue+ ' '
//            }
//
//            TextViewBar.text = stringCode //+ '/' + stringkey.toString()
//
//        } else {
//            Log.v("BARCODE_PROCESSOR", "Code not found")
//        }
//    }


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
