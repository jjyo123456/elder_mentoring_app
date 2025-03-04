package com.example.elderprojectfinal
import android.app.Application
import android.content.Context
import org.webrtc.*
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.elderprojectfinal.data_classes_for_handelling_gemini_response.geminiresponse
import com.example.elderprojectfinal.databinding.ActivityTheMainWorkoutMatchBinding
import com.google.android.gms.common.api.internal.ApiKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Calendar
import java.util.Objects
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoTrack
import org.webrtc.VideoCapturer
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnection.IceServer
import javax.microedition.khronos.egl.EGLContext


class the_main_workout_match_activity : AppCompatActivity() {




    val firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

    val apiKey:String = "AIzaSyDJW69wH1BqmlnSu7XoK9Avhp5v8q_PuE4"

    var url:String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?"

    public var main_video_call_button:Button = findViewById(R.id.main_video_call_button)

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var remoteVideoTrack: VideoTrack
    private lateinit var videoCapturer: VideoCapturer

    private var context:Context =



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_the_main_workout_match)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var binding:ActivityTheMainWorkoutMatchBinding = ActivityTheMainWorkoutMatchBinding.inflate(layoutInflater)

        main_video_call_button.visibility = View.GONE


        binding.button5.setOnClickListener({
            matching()
        })




    }


    public fun matching(){

        var firebaseauth:FirebaseAuth = FirebaseAuth.getInstance()

        var user_collection = firestore.collection("users")
        var current_user_id:String = firebaseauth.currentUser.toString()




        val a = firestore.collection().document().collection().get().addOnSuccessListener { document ->
            val user:object_for_matching = document.toObjects<object_for_matching>()bject(object_for_matching::class.java)

            val age_of_user = user.age
            val interests:Array<String> = user.interests
            val workout_tpye = user.workout_type
            val weight = user.weight
            val fitness_goal = user.fitness_goal

            user_collection
                .whereGreaterThanOrEqualTo("age", age_of_user - 2)
            .whereLessThanOrEqualTo("age", age_of_user + 2)
            .whereGreaterThanOrEqualTo("weight",  weight - 10)
            .whereLessThanOrEqualTo("weight", weight + 10)
            .whereEqualTo("fitness_goal", fitness_goal)
            .whereEqualTo("workout_mode", workout_tpye)
                .limit(1)
            .get()
                .addOnSuccessListener {document_of_match ->
                    val match = document_of_match.documents[0]
                    val match_id = match.id

                   save_match(current_user_id , match_id){save_match_result ->



                    }

                    setContentView(R.layout.matched_user_profile_layout)

                    finalize_the_schedule_time(current_user_id,match_id)

                    Put_in_the_info_for_the_profile(match_id)



                }
                .addOnFailureListener({

                })



        }







    }

    public fun save_match(current_user_id:String, match_id:String, callback:(String) -> Unit){
        var firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

        Thread {

            firestore.collection("users").document(current_user_id).collection("matches")
                .document(match_id)

            callback("save_succsessfull")
        }
    }

    public fun deletematch(){
        var firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

        Thread{
        firestore.collection("users").document(current_user_id_main).collection("matches").document(matched_user_id).delete().addOnSuccessListener {
        }

        }

    }

    public fun finalize_the_schedule_time(current_user_id: String, match_id: String){

        var current_user_ref_firebase = firestore.collection("users").document(current_user_id).collection("schedule").document("preferences")

        var current_user_days: List<String>? = null

        var current_user_start_time:String? = null

        var current_user_end_time:String? = null

        var matched_user_days:List<String>? = null

        var matched_user_start_time:String? = null

        var matched_user_end_time:String? = null

            current_user_ref_firebase.get().addOnSuccessListener {document->
            var current_user_days:List<String> = document.get("")
            var current_user_start_time = document.get("start_time")
            var current_user_end_time = document.get("end_time")
        }

        var mathced_user_ref_firebase = firestore.collection("users").document(match_id).collection("schedule").document("preferences")

        current_user_ref_firebase.get().addOnSuccessListener {document->
            var matched_user_days:List<String> = document.get("")
            var matched_user_start_time = document.get("start_time")
            var matched_user_end_time = document.get("end_time")


                sendToGeminiForScheduling(current_user_days,current_user_start_time,current_user_end_time,matched_user_days,matched_user_start_time,matched_user_end_time )


        }





    }

    suspend fun sendToGeminiForScheduling(
        currentUserDays: List<String>?, currentUserStartTime: String?, currentUserEndTime: String?,
        matchedUserDays: List<String>?, matchedUserStartTime: Any?, matchedUserEndTime: Any?
    ) {

        val prompt = """
        Find the best available time for two users based on their schedules.
        User 1: Available on $currentUserDays from $currentUserStartTime to $currentUserEndTime
        User 2: Available on $matchedUserDays from $matchedUserStartTime to $matchedUserEndTime
        Suggest a single best time slot where both users are available.return the answer in Json format like - 
        
        {
        best_day : "Tuesday"
        start_time : 4:00 AM
        end_tme : 5:00 AM
    """.trimIndent()



        var url:String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?"
        val gemini_retrofit = Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build()

        var apiservie = gemini_retrofit.create(geminiapiservice::class.java)


        apiservie.generateresponse(apiKey,gemini_data_prompt(prompt))

    }

    public interface geminiapiservice{
        @Headers("content-type : application/json")
        @POST()
        suspend fun generateresponse(
            @Query("key") apiKey:String,
            @Body requestBody:  gemini_data_prompt
        ):Response<geminiresponse_handler>


    }








    data class gemini_data_prompt(val string:String){
        val prompt:String = string
    }

    data class geminiresponse_handler(var response:geminiresponse){
        var prompt_actual_response = response.candidates.firstOrNull()?.content.parts.firstOrNull()?.texts

        var best_day =
        var start_time =
        var end_tim =


           GlobalScope.launch {
               while(true){
                   delay(30000)
                   val calendar = Calendar.getInstance()
                   val day = calendar.get(Calendar.DAY_OF_MONTH)
                   val hour = calendar.get(Calendar.HOUR_OF_DAY)
                   val minute = calendar.get(Calendar.MINUTE)

                   if(day == best_day && ){
                       main_video_call_button.visibility = View.VISIBLE
                   }
               }



           }


    }



    public fun setupwebrtc(context: Context){

        var eglBase = EglBase.create()
        var eglbasecontext:EglBase.Context = eglBase.eglBaseContext

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder().setVideoDecoderFactory(DefaultVideoDecoderFactory(eglbasecontext)).setVideoEncoderFactory(DefaultVideoEncoderFactory(eglbasecontext,true,true)).createPeerConnectionFactory()

        var videosource = peerConnectionFactory.createVideoSource(false)
        var video_track_for_camera_related_video = peerConnectionFactory.createVideoTrack("local_track",videosource)

        peerConnection = peerConnectionFactory.createPeerConnection(
            listOf(IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                object:PeerConnection.Observer{
                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                        TODO("Not yet implemented")
                    }

                    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                        TODO("Not yet implemented")
                    }

                    override fun onIceConnectionReceivingChange(p0: Boolean) {
                        TODO("Not yet implemented")
                    }

                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                        TODO("Not yet implemented")
                    }

                    override fun onIceCandidate(p0: IceCandidate?) {
                        send(p0)
                    }

                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                        TODO("Not yet implemented")
                    }

                    override fun onAddStream(p0: MediaStream?) {
                        TODO("Not yet implemented")
                    }

                    override fun onRemoveStream(p0: MediaStream?) {
                        TODO("Not yet implemented")
                    }

                    override fun onDataChannel(p0: DataChannel?) {
                        TODO("Not yet implemented")
                    }

                    override fun onRenegotiationNeeded() {
                        TODO("Not yet implemented")
                    }

                    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                        TODO("Not yet implemented")
                    }
                })
        )
    }

public fun seticecandidate(){

}



}