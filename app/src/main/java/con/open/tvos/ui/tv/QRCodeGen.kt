package con.open.tvos.ui.tv

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
object QRCodeGen {
    
    fun generateBitmap(content: String, width: Int, height: Int, padding: Int): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "utf-8",
            EncodeHintType.MARGIN to padding.toString()
        )
        
        return try {
            val encode = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val pixels = IntArray(width * height)
            
            for (i in 0 until height) {
                for (j in 0 until width) {
                    pixels[i * width + j] = if (encode.get(j, i)) 0x00000000 else 0xffffffff.toInt()
                }
            }
            
            Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun generateBitmap(content: String, width: Int, height: Int): Bitmap? {
        return generateBitmap(content, width, height, 0)
    }
}
