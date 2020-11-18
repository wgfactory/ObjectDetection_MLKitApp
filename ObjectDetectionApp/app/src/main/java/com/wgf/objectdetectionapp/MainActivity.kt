package com.wgf.objectdetectionapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // 카메라, 갤러리 권한을 얻기 위한 Permission 전역 변수 선언
    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val STORAGE_PERMISSION = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    // 카메라, 갤러리 권한 요청을 위한 Permission 요청 코드
    val REQ_PERMISSION_CAMERA = 98
    val REQ_PERMISSION_STORAGE = 99

    // 카메라, 갤러리 앱 열기를 위한 Intent 요청 코드
    val ODT_REQ_CAMERA_IMAGE = 101
    val ODT_REQ_GALLERY_IMAGE = 102

    // 카메라 원본이미지 Uri(촬영된 사진의 원본)를 저장할 변수
    var mPhotoURI: Uri? = null
    var mBitmap: Bitmap? = null

    /**
     * (1) onCreate() :
     *
     *      생명주기 가장 첫 실행되는 함수
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, ">> onCreate()")

        // 권한 체크하기 함수 호출!
        if (checkPermission(STORAGE_PERMISSION, REQ_PERMISSION_STORAGE)) {
            setViews()
        }
    }

    /**
     * 권한처리
     * (2) checkPermission() :
     *
     *      권한처리를 수행하는 함수
     */
    fun checkPermission(permissions: Array<out String>, flag: Int) : Boolean {
        Log.d(TAG, ">> checkPermission()")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, flag)
                    return false
                }
            }
        }
        return true
    }

    /**
     * (3) onRequestPermissionsResult() :
     *
     *      Permission(권한) 처리 후 결과를 확인하는 함수
     */
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQ_PERMISSION_STORAGE -> {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        showToast("저장소 권한을 승인해야지만 앱을 사용할 수 있습니다.")
                        finish()
                        return
                    }
                }

                setViews()
            }
            REQ_PERMISSION_CAMERA -> {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        showToast("카메라 권한을 승인해야지만 카메라를 사용할 수 있습니다.")
                        return
                    }
                }
                openCamera()
            }
        }
    }

    /**
     * (4) setViews() :
     *
     *      Permission(권한) 처리 후 버튼 클릭을 대기하는 뷰 함수
     */
    fun setViews(){
        Log.d(TAG, ">> setViews()")

        //카메라 버튼 클릭
        buttonCamera.setOnClickListener {
            openCamera()
        }
        //갤러리 버튼 클릭
        buttonGallery.setOnClickListener {
            openGallery()
        }
    }

    /**
     * (5) openCamera() :
     *
     *      시스템 카메라 앱을 호출하여 수행하는 함수
     */
    fun openCamera() {
        Log.d(TAG, ">> openCamera()")

        if (checkPermission(CAMERA_PERMISSION, REQ_PERMISSION_CAMERA)) {

            // 시스템 카메라 앱 호출 Intent
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            createImageUri(newFileName(), "image/jpg")?.let { uri ->
                mPhotoURI = uri
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoURI)
                startActivityForResult(takePictureIntent, ODT_REQ_CAMERA_IMAGE)
            }
        }
    }

    /**
     * (6) openGallery() :
     *
     *      시스템 갤러리 앱을 호출하여 수행하는 함수
     */
    fun openGallery() {
        Log.d(TAG, ">> openGallery()")

        // 시스템 갤러리 앱 호출 Intent
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent, ODT_REQ_GALLERY_IMAGE)
    }

    /**
     * (7) onActivityResult() :
     *
     *      카메라, 갤러리 앱 호출 후 결과를 수행하는 함수
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            when(requestCode){
                ODT_REQ_CAMERA_IMAGE -> {
                    if (mPhotoURI != null) {
                        val bitmap = loadBitmapFromMediaStoreBy(mPhotoURI!!)
                        mBitmap = bitmap
                            val image = getCapturedImage(mPhotoURI!!)
                        runObjectDetection(image)

                        mPhotoURI = null // 사용 후 null 처리
                    }
                }
                ODT_REQ_GALLERY_IMAGE -> {
                    val uri = data?.data
                    val image = getCapturedImage(uri!!)
                    runObjectDetection(image)
                }
            }
        }
    }

    /**
     * (8) createImageUri() :
     *
     *      미디어스토어에 카메라 이미지를 저장할 URI를 미리 생성하는 함수
     */
    fun createImageUri(filename: String, mimeType: String) : Uri? {
        Log.d(TAG, ">> createImageUri()")

        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /**
     * (9) newFileName() :
     *
     *      촬영된 사진의 파일 이름을 생성하는 함수
     */
    fun newFileName() : String {
        Log.d(TAG, ">> newFileName()")

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())

        return "$filename.jpg"
    }

    /**
     * (10) loadBitmapFromMedia() :
     *      Camera 촬영으로 얻은 원본 이미지 가져오기
     *      미디어 스토어에서 uri로 이미지 불러오는 함수
     */
    fun loadBitmapFromMediaStoreBy(photoUri: Uri): Bitmap? {
        var image: Bitmap? = null
        try {
            image = if (Build.VERSION.SDK_INT > 27) { // Api 버전별 이미지 처리
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(this.contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }

    /**
     * (11) getCapturedImage():
     *     Decodes and center crops the captured image from camera.
     *     Object Detection을 수행하기 전에 이미지 처리를 수행하는 함수
     */
    private fun getCapturedImage(imgUri: Uri): Bitmap {
        Log.d(TAG, ">> getCapturedImage()")

        var srcImage: Bitmap? = null
        try {
            srcImage = InputImage.fromFilePath(baseContext, imgUri!!).bitmapInternal
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // crop image to match imageView's aspect ratio
        val scaleFactor = Math.min(
            srcImage!!.width / imagePreview.width.toFloat(),
            srcImage!!.height / imagePreview.height.toFloat()
        )

        val deltaWidth = (srcImage.width - imagePreview.width * scaleFactor).toInt()
        val deltaHeight = (srcImage.height - imagePreview.height * scaleFactor).toInt()

        val scaledImage = Bitmap.createBitmap(
            srcImage, deltaWidth / 2, deltaHeight / 2,
            srcImage.width - deltaWidth, srcImage.height - deltaHeight
        )
        srcImage.recycle()
        return scaledImage
    }

    /**
     * MLKit Object Detection Function
     * (12) runObjectDetection() :
     *      MLKit Object Detection Function
     *      Object Detection을 수행하는 함수
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        Log.d(TAG, ">> runObjectDetection()")

        // Step 1: create MLKit's VisionImage object
        val image = InputImage.fromBitmap(bitmap, 0)

        // Step 2: acquire detector object
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()  // Optional
            .build()

        val detector = ObjectDetection.getClient(options)

        // Step 3: feed given image to detector and setup callback
        detector.process(image)
            .addOnSuccessListener {
                // Task completed successfully
                // Post-detection processing : draw result

                if(it.size == 0) {
                    showToast("아쉽지만 이미지에서 사물을 못찾았습니다. \n 다시 시도해주세요!")
                }
                val drawingView = DrawingView(applicationContext, it)
                drawingView.draw(Canvas(bitmap))

                runOnUiThread {
                    imagePreview.setImageBitmap(bitmap)
                }
            }
            .addOnFailureListener {
                // Task failed with an exception
                showToast("Oops, something went wrong!")
            }
    }

    /**
     * (13) showToast() :
     *      Make Toast Function
     *     토스트 메세지 표시 하는 함수
     */
    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Log.d(TAG, ">> showToast()")

        val toast = Toast.makeText(applicationContext, message, duration)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}