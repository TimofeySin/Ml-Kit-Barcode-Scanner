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
import android.widget.*

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
    private lateinit var imageViewPreview: ImageView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val displayWidth = Resources.getSystem().displayMetrics.widthPixels
    private val displayHeight = Resources.getSystem().displayMetrics.heightPixels
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var scanner: BarcodeScanner
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_first, container, false)
        surfaceViewCamera = view.findViewById(R.id.surfaceViewCamera)
        takePhoto = view.findViewById(R.id.takeFoto)
        textViewOut = view.findViewById(R.id.textView)
        imageViewPreview = view.findViewById(R.id.imageViewPreview)
        imageViewPreview.alpha = "0.0".toFloat()
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
            cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        preview = Preview.Builder()
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
           when(takePhoto.text){
            "Назад"-> buttonBack()
            "Фото" -> startRecognize()
           }
        }
    }

    private fun startRecognize() {
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

    private fun buildControls() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
        scanner = BarcodeScanning.getClient(options)
    }

    private fun setImageToView(image: Bitmap) {
        imageViewPreview.setImageBitmap(image)
        imageViewPreview.alpha = "1.0".toFloat()
        cameraProvider.unbindAll()
        takePhoto.text = "Назад"
    }

    private fun recognizeBarcode(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image)
        val inputImg = InputImage.fromBitmap(bitmap, image.imageInfo.rotationDegrees)
        var msg = "Штрихкоды: "
        scanner.process(inputImg)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    msg += barcode.displayValue.toString() + " "
                }
                setImageToView(bitmap)
                Handler(Looper.getMainLooper()).post {
                    textViewOut.text = msg
                }
            }
            .addOnFailureListener { e ->
                val msg = "Ошибка распознования:" + e.localizedMessage
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addImageView() {
        val imageView = ImageView(requireContext())
        imageView.layoutParams = LinearLayout.LayoutParams(160, 160) // value is in pixels

        val imgResId = R.drawable.ok
        var resId = imgResId
        imageView.setImageResource(imgResId)


    }

    private fun buttonBack(){
        imageViewPreview.alpha = "0.0".toFloat()
        takePhoto.text = "Фото"
        textViewOut.text = ""
        setupCamera()
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

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
