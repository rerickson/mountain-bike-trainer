import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

public class TrainerAPIClient {
    private val client = OkHttpClient()

    fun saveData() {
        val request = Request.Builder()
            .url("http://192.168.4.69:5029/api/Sessions")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }

                    println(response.body!!.string())
                }
            }
        })
    }
}
