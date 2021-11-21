package com.example.mlkitsample

import androidx.camera.core.*
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.util.Size
import android.view.Display
import android.view.Surface.*
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener


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
    private lateinit var mainFraime: ConstraintLayout
    private var widthSize = 0
    private var heightSize = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater,container,false)
        imageViewPreview = _binding!!.imageViewPreview
        imageViewPreview.alpha = 0.0f
        takePhoto = _binding!!.takeFoto
        textViewOut = _binding!!.textView
        surfaceViewCamera = _binding!!.surfaceViewCamera
        mainFraime = _binding!!.mainFraime


        return _binding!!.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (cameraPermissionWasGranted()) {

            imageViewPreview.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    imageViewPreview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    heightSize=   mainFraime.height //height is ready 1788
                    widthSize= mainFraime.width ///1080

                }
            })


            surfaceViewCamera.post { setupCamera() }
        } else {
            askForCameraPermission()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(displayWidth, displayHeight)) ///2120  1080

            .build()

        buildControls()
        addButtonTakePhoto()
    //    imageCapture.mCropAspectRatio    27/53   mNumerator/mDenominator

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
            .setTargetResolution(Size(displayWidth, displayHeight)) ///2120  1080
           // .setTargetResolution(Size(widthSize, heightSize))
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

    private fun setImageToView(image: Bitmap,rotationDegrees: Int) {
        val  msg = image.height.toString() + " / " +image.width.toString() //1080/2160   image.mDensity 440
        //val inf = image.imageInfo



        Handler(Looper.getMainLooper()).post {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        imageViewPreview.setImageBitmap(image)
        imageViewPreview.adjustViewBounds = true

        //  imageViewPreview.rotation = rotationDegrees.toFloat()
        imageViewPreview.alpha = 1.0f
        surfaceViewCamera.alpha =0.0f
        //imageViewPreview.baseline
        cameraProvider.unbindAll()
        takePhoto.text = "Назад"

//imageViewPreview.mMeasuredHeight 1788
        //imageViewPreview.mMeasuredWidth 1080
     //   imageViewPreview.mTempDst 0,624,1080,1164  (0,0,1080,540
        //   imageViewPreview.mTempSrc  0,0,2160,1080
    }

    private fun recognizeBarcode(image: ImageProxy) {
        val bitmap = imageProxyToBitmap(image,image.imageInfo.rotationDegrees)
        val inputImg = InputImage.fromBitmap(bitmap,0)
        var msg = "Штрихкоды: "
        val arrayBounds: MutableList<Rect> = ArrayList()
        val arrayCorners: MutableList<Array<Point>> = ArrayList()
        val rationX = heightSize.toFloat() / image.height.toFloat()
        val rationY = widthSize.toFloat()  / image.width.toFloat()
        scanner.process(inputImg)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints
                    msg += barcode.displayValue!!.toString() + " "
                    arrayBounds.add(bounds!!)
                    arrayCorners.add(corners!!)
                }
                setImageToView(bitmap,image.imageInfo.rotationDegrees)


                addOkImage(arrayBounds,arrayCorners,rationX,rationY)
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

    private fun addOkImage(arrayBounds: MutableList<Rect>,arrayCorners: MutableList<Array<Point>>,rationX:Float,rationY:Float) {

        for (newPic in arrayCorners) {
            mainFraime.addView(Rectangle(requireContext(),newPic,rationX,rationY))
        }

    }

    private class Rectangle(context: Context,val rect:Array<Point>,val ratX:Float, val ratY: Float) : View(context) {

        var paint = Paint()

        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas) {
            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth =10f

            for( i in 0..2)
            {
                canvas.drawLine(rect[i].x.toFloat(),rect[i].y.toFloat() ,rect[i+1].x.toFloat(),rect[i+1].y.toFloat(),paint)
            }
            canvas.drawLine(rect[3].x.toFloat(),rect[3].y.toFloat() ,rect[0].x.toFloat(),rect[0].y.toFloat(),paint)

//            canvas.drawLine(convert(0,ratX,ratY,1), convert(0,ratX,ratY,2), convert(1,ratX,ratY,1), convert(1,ratX,ratY,2), paint)
//            canvas.drawLine(convert(1,ratX,ratY,1), convert(1,ratX,ratY,2), convert(2,ratX,ratY,1), convert(2,ratX,ratY,2), paint)
//            canvas.drawLine(convert(2,ratX,ratY,1), convert(2,ratX,ratY,2), convert(3,ratX,ratY,1), convert(3,ratX,ratY,2), paint)
//            canvas.drawLine(convert(3,ratX,ratY,1), convert(3,ratX,ratY,2), convert(0,ratX,ratY,1), convert(0,ratX,ratY,2), paint)
        }
        private fun convert(index: Int, ratx: Float, raty: Float, type: Int): Float {

            return when (type) {
               1 ->
                   rect[index].x.toFloat() * ratx
                else -> rect[index].y.toFloat() * raty
            }

        }
    }


    private fun buttonBack(){
        imageViewPreview.alpha = 0.0f
        takePhoto.text = "Фото"
        textViewOut.text = ""
        surfaceViewCamera.alpha =1.0f
        mainFraime.removeAllViews()
        setupCamera()
    }

    private fun imageProxyToBitmap(image: ImageProxy,rotationDegrees:Int): Bitmap {
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

