package com.example.mlkitsample

import androidx.camera.core.*
import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.Context

import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*

import android.hardware.camera2.CameraManager
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

import android.view.Surface.*

import android.widget.*
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout

import androidx.lifecycle.LifecycleOwner
import com.example.mlkitsample.databinding.FragmentFirstBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.core.content.ContextCompat.getSystemService
import android.hardware.SensorManager
import androidx.core.content.PackageManagerCompat.LOG_TAG

import android.hardware.camera2.CameraAccessException

import android.hardware.camera2.CameraCharacteristics

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Log
import android.util.Size
import androidx.core.content.PackageManagerCompat
import android.widget.ArrayAdapter
import androidx.core.content.getSystemService


class FirstFragment : Fragment() {
    private val requestCodeCameraPermission = 1001
    private var _binding: FragmentFirstBinding? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var surfaceViewCamera: PreviewView
    private lateinit var takePhoto: Button
    private lateinit var textViewOut: TextView
    private lateinit var imageViewPreview: ImageView
    private lateinit var spinnerItem: Spinner
    private lateinit var imageCaptureBuilder: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var globalLayout: LinearLayout
    private lateinit var mainFrame: FrameLayout

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var scanner: BarcodeScanner
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var frameDraw: ConstraintLayout
    private var widthSize = 0
    private var heightSize = 0
    private var bottomOffset = 0
    private val LOG_TAG="CameraXLog"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {


        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        imageViewPreview = _binding!!.imageViewPreview
        imageViewPreview.alpha = 0.0f
        takePhoto = _binding!!.takeFoto
        textViewOut = _binding!!.textView
        surfaceViewCamera = _binding!!.surfaceViewCamera

        mainFrame = _binding!!.mainFrame
        globalLayout = _binding!!.globalLayout
        frameDraw = _binding!!.frameDraw
        spinnerItem=     _binding!!.spinnerItem
        return _binding!!.root
    }


    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        if (cameraPermissionWasGranted()) {
            textViewOut.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    imageViewPreview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    heightSize = mainFrame.height //height is ready 1788
                    widthSize = mainFrame.width ///1080
                    bottomOffset = (globalLayout.bottom - mainFrame.bottom)*2
                }
            })
            surfaceViewCamera.post { setupCamera() }
        } else {
            askForCameraPermission()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        buildControls()
        addButtonTakePhoto()
    }

    @SuppressLint("ServiceCast")
    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

        imageCaptureBuilder = ImageCapture.Builder()
            //.setTargetResolution(Size(displayWidth, displayHeight))
            .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
   val mCameraManager  = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val resItems :  MutableList<String> = ArrayList()

        try {
            // Получение списка камер с устройства
         // var  myCameras = arrayOfNulls<String>(mCameraManager.cameraIdList.size)

            // выводим информацию по камере
            for (cameraID in mCameraManager.cameraIdList) {
                val id = cameraID.toInt()

                // Получениe характеристик камеры
                val cc: CameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraID)
                // Получения списка выходного формата, который поддерживает камера
                val configurationMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                //  Определение какая камера куда смотрит

                if ( cc.get(CameraCharacteristics.LENS_FACING)!! == CameraCharacteristics.LENS_FACING_BACK) {
                     val mTrueCameraID = cameraID


                    val sizesJPEG: Array<Size>? = configurationMap!!.getOutputSizes(ImageFormat.JPEG)
                    if (sizesJPEG != null) {
                        for (item in sizesJPEG) {
                            resItems.add("w:" + item.width.toString() + " h:" + item.height)
                        }
                    }
                }
            }
        } catch (e: CameraAccessException) {
            e.message?.let { Log.e(LOG_TAG, it) }
            e.printStackTrace()
        }


        spinnerItem.adapter = ArrayAdapter(requireContext(),  R.layout.drop_list_item , resItems)



    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        preview = Preview.Builder()
            //.setTargetResolution(Size(displayWidth, displayHeight)) ///2120  1080 ))
            .build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        preview.setSurfaceProvider(surfaceViewCamera.surfaceProvider)

        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageCaptureBuilder,
            preview
        )
    }

    private fun addButtonTakePhoto() {

        takePhoto.setOnClickListener {
            when (takePhoto.text) {
                "Назад" -> buttonBack()
                "Фото" -> startRecognize()
            }
        }
    }

    private fun startRecognize() {
        imageCaptureBuilder.takePicture(cameraExecutor,
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
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODE_128
            )
            .build()
        scanner = BarcodeScanning.getClient(options)
    }

    private fun setImageToView(image: Bitmap, rotationDegrees: Int) {
        val msg =
            image.height.toString() + " / " + image.width.toString() //1080/2160   image.mDensity 440

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        imageViewPreview.setImageBitmap(image)
        imageViewPreview.adjustViewBounds = true
        imageViewPreview.scaleType = ImageView.ScaleType.FIT_END
        //  imageViewPreview.rotation = rotationDegrees.toFloat()
        imageViewPreview.alpha = 1.0f
        surfaceViewCamera.alpha = 0.0f
        //imageViewPreview.baseline
        cameraProvider.unbindAll()
        takePhoto.text = "Назад"


    }

    private fun countration(width: Int, height: Int): Float {
        val kWidth = widthSize.toFloat() / width.toFloat()
        val kHeight =  heightSize.toFloat() / height.toFloat()
        return if (kWidth > kHeight) {
            kHeight
        } else {
            kWidth
        }
    }


    private fun recognizeBarcode(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image, image.imageInfo.rotationDegrees)
        val ratiom = countration(bitmap.width, bitmap.height)
        val inputImg = InputImage.fromBitmap(bitmap, 0)
        var i = 0
        var msg = "Штрихкоды: "


        val arrayBounds: MutableList<Rect> = ArrayList()
        val arrayCorners: MutableList<Array<Point>> = ArrayList()
      //  val rationX = ratiom
      //  val rationY = ratiom
        scanner.process(inputImg)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    msg += barcode.displayValue!!.toString() //+ "{" + corners[0].x + "/" + corners[0].y + "}" + "{" + corners[1].x + "/" + corners[1].y + "}" + "{" + corners[2].x + "/" + corners[2].y + "}" + "{" + corners[3].x + "/" + corners[3].y + "}, "
                    arrayBounds.add(bounds!!)
                    arrayCorners.add(corners!!)
                    i++
                }
                setImageToView(bitmap, image.imageInfo.rotationDegrees)
                addOkImage(arrayBounds, arrayCorners, ratiom, ratiom)
                msg += " Всего: " + i
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

    private fun addOkImage(
        arrayBounds: MutableList<Rect>,
        arrayCorners: MutableList<Array<Point>>, rationX: Float, rationY: Float,
    ) {

        val offY = bottomOffset.toFloat()
        val offX = 0f

        for (newPic in arrayCorners) {
            frameDraw.addView(Rectangle(requireContext(), newPic, rationX, rationY, offX, offY))
        }

    }

    private class Rectangle(
        context: Context,
        val rect: Array<Point>,
        val ratX: Float,
        val ratY: Float,
        val offX: Float,
        val offY: Float,
    ) : View(context) {

        var paint = Paint()

        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas) {
            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f

//            for( i in 0..2)
//            {
//                canvas.drawLine(rect[i].x.toFloat(),rect[i].y.toFloat() ,rect[i+1].x.toFloat(),rect[i+1].y.toFloat(),paint)
//            }
//            canvas.drawLine(rect[3].x.toFloat(),rect[3].y.toFloat() ,rect[0].x.toFloat(),rect[0].y.toFloat(),paint)

            canvas.drawLine(convert(0, ratX, ratY, 1),
                convert(0, ratX, ratY, 2),
                convert(1, ratX, ratY, 1),
                convert(1, ratX, ratY, 2),
                paint)
            canvas.drawLine(convert(1, ratX, ratY, 1),
                convert(1, ratX, ratY, 2),
                convert(2, ratX, ratY, 1),
                convert(2, ratX, ratY, 2),
                paint)
            canvas.drawLine(convert(2, ratX, ratY, 1),
                convert(2, ratX, ratY, 2),
                convert(3, ratX, ratY, 1),
                convert(3, ratX, ratY, 2),
                paint)
            canvas.drawLine(convert(3, ratX, ratY, 1),
                convert(3, ratX, ratY, 2),
                convert(0, ratX, ratY, 1),
                convert(0, ratX, ratY, 2),
                paint)
        }

        private fun convert(index: Int, ratx: Float, raty: Float, type: Int): Float {

            return when (type) {
                1 ->
                    rect[index].x.toFloat() * ratx + offX
                else -> rect[index].y.toFloat() * raty + offY
            }

        }
    }


    private fun buttonBack() {
        imageViewPreview.alpha = 0.0f
        takePhoto.text = "Фото"
        textViewOut.text = ""
        surfaceViewCamera.alpha = 1.0f
        frameDraw.removeAllViews()
        setupCamera()
    }

    private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)


        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
            matrix, true)
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

